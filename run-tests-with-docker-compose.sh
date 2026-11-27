#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.yml"
TEST_IMAGE="nbank-tests:local"
COMPOSE_PROJECT_NAME="nbank-tests"
FRONTEND_DIR=""
for candidate in "$SCRIPT_DIR/../NBank-UI" "$SCRIPT_DIR/../../NBank-UI"; do
  if [[ -d "$candidate" ]]; then
    FRONTEND_DIR="$(cd -- "$candidate" && pwd)"
    break
  fi
done
if [[ -z "$FRONTEND_DIR" ]]; then
  echo "Ошибка: не найдена соседняя папка NBank-UI. Нужен фронтенд с npm start." >&2
  exit 1
fi
COMPOSE=(docker compose --project-name "$COMPOSE_PROJECT_NAME" --file "$COMPOSE_FILE")
FRONTEND_PID=""
FRONTEND_SERVER_PID=""
FRONTEND_LOG=""
FRONTEND_PORT=3000
UI_HOST_IP="$(ifconfig en0 2>/dev/null | awk '$1 == "inet" && $2 != "127.0.0.1" { print $2; exit }')"

if ! command -v docker >/dev/null 2>&1; then
  echo "Ошибка: Docker не найден. Установите и запустите Docker Desktop." >&2
  exit 127
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "Ошибка: curl не найден; он нужен для проверки готовности сервисов." >&2
  exit 127
fi

if ! command -v npm >/dev/null 2>&1; then
  echo "Ошибка: npm не найден; установите Node.js для запуска фронтенда." >&2
  exit 127
fi

if ! command -v lsof >/dev/null 2>&1; then
  echo "Ошибка: lsof не найден; он нужен, чтобы выбрать свободный порт для фронтенда." >&2
  exit 127
fi

if [[ -z "$UI_HOST_IP" ]]; then
  echo "Ошибка: не удалось определить IP-адрес Mac на интерфейсе en0." >&2
  exit 1
fi

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "Ошибка: не найден файл $COMPOSE_FILE" >&2
  exit 1
fi

cleanup() {
  local exit_code=$?
  trap - EXIT

  if [[ -n "$FRONTEND_PID" ]] && kill -0 "$FRONTEND_PID" 2>/dev/null; then
    echo ">>> Останавливаю запущенный npm start..."
    kill "$FRONTEND_PID" 2>/dev/null || true
    wait "$FRONTEND_PID" 2>/dev/null || true
  fi
  if [[ -n "$FRONTEND_SERVER_PID" ]] && kill -0 "$FRONTEND_SERVER_PID" 2>/dev/null; then
    echo ">>> Останавливаю процесс React-сервера (PID $FRONTEND_SERVER_PID)..."
    kill "$FRONTEND_SERVER_PID" 2>/dev/null || true
  fi
  if [[ -n "$FRONTEND_LOG" ]]; then
    rm -f "$FRONTEND_LOG"
  fi

  echo ">>> Останавливаю тестовое окружение..."
  if ! "${COMPOSE[@]}" down --remove-orphans; then
    echo ">>> Не удалось корректно остановить окружение." >&2
    if [[ "$exit_code" -eq 0 ]]; then
      exit_code=1
    fi
  fi

  exit "$exit_code"
}

trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

echo ">>> Собираю образ с API и UI тестами..."
docker build --tag "$TEST_IMAGE" "$SCRIPT_DIR"

echo ">>> Поднимаю тестовое окружение через Docker Compose..."
"${COMPOSE[@]}" up -d --remove-orphans

while lsof -nP -tiTCP:"$FRONTEND_PORT" -sTCP:LISTEN >/dev/null 2>&1; do
  echo ">>> Порт $FRONTEND_PORT занят; выбираю следующий порт для фронтенда."
  FRONTEND_PORT=$((FRONTEND_PORT + 1))
done

if [[ ! -d "$FRONTEND_DIR/node_modules" ]]; then
  echo ">>> Устанавливаю зависимости фронтенда..."
  (cd "$FRONTEND_DIR" && npm ci)
fi

echo ">>> Запускаю фронтенд из $FRONTEND_DIR через npm start на порту $FRONTEND_PORT..."
echo ">>> Адрес фронтенда для Chrome в Selenoid: http://$UI_HOST_IP:$FRONTEND_PORT"
FRONTEND_LOG="$(mktemp -t nbank-frontend.XXXXXX)"
(cd "$FRONTEND_DIR" && exec env PORT="$FRONTEND_PORT" BROWSER=none npm start) >"$FRONTEND_LOG" 2>&1 &
FRONTEND_PID=$!

wait_for_url() {
  local url="$1"
  local description="$2"
  local timeout_seconds=120
  local start_time=$SECONDS

  echo ">>> Жду готовности: $description ($url)"
  until curl --silent --output /dev/null --max-time 2 "$url"; do
    if (( SECONDS - start_time >= timeout_seconds )); then
      echo "Ошибка: $description не ответил за ${timeout_seconds} секунд." >&2
      "${COMPOSE[@]}" logs --no-color
      if [[ "$description" == "Frontend" && -n "$FRONTEND_LOG" ]]; then
        echo ">>> Последние сообщения npm start:"
        cat "$FRONTEND_LOG" >&2
      fi
      return 1
    fi
    sleep 2
  done
}

wait_for_url "http://localhost:4111" "Backend"
wait_for_url "http://localhost:$FRONTEND_PORT" "Frontend"
FRONTEND_SERVER_PID="$(lsof -nP -tiTCP:"$FRONTEND_PORT" -sTCP:LISTEN | head -n 1 || true)"
if [[ -z "$FRONTEND_SERVER_PID" ]]; then
  echo "Ошибка: не удалось определить PID React-сервера на порту $FRONTEND_PORT." >&2
  exit 1
fi
wait_for_url "http://localhost:4444/status" "Selenoid"
wait_for_url "http://localhost:8080" "Selenoid UI"

echo ">>> Запускаю все API и UI тесты в контейнере..."
docker run --rm \
  --network nbank-network \
  --env APIBASEURL="http://backend:4111" \
  --env UIBASEURL="http://$UI_HOST_IP:$FRONTEND_PORT" \
  --env SELENOID_URL="http://selenoid:4444" \
  --env SELENOID_UI_URL="http://selenoid-ui:8080" \
  --env UIREMOTE="http://selenoid:4444/wd/hub" \
  "$TEST_IMAGE" \
  mvn --batch-mode test

echo ">>> Все тесты завершили работу."

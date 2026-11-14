#!/bin/bash
set -euo pipefail

# Настройка
IMAGE_NAME=nbank-tests
DOCKERHUB_USERNAME="mariadobrinevskaya"
TAG=1.0.0
TOKEN="${1:-}"

LOCAL_IMAGE="$IMAGE_NAME:latest"
REMOTE_IMAGE="$DOCKERHUB_USERNAME/$IMAGE_NAME:$TAG"

if [ -z "$TOKEN" ]; then
  echo 'Использование: ./push-tests.sh <токен Docker Hub>' >&2
  exit 1
fi

if [ -z "$DOCKERHUB_USERNAME" ] || [ "$DOCKERHUB_USERNAME" = "your-dockerhub-username" ]; then
  echo 'Укажи свой логин Docker Hub в переменной DOCKERHUB_USERNAME.' >&2
  exit 1
fi

# Проверяем, что образ уже собран
if ! docker image inspect "$LOCAL_IMAGE" >/dev/null; then
  printf 'Не удалось найти локальный образ %s. Сначала собери его.\n' "$LOCAL_IMAGE" >&2
  exit 1
fi

# Входим в Docker Hub
echo '>>> Вход в Docker Hub'
printf '%s\n' "$TOKEN" |
  docker login --username "$DOCKERHUB_USERNAME" --password-stdin

# Тегируем и публикуем образ
echo ">>> Публикация образа $REMOTE_IMAGE"
docker image tag "$LOCAL_IMAGE" "$REMOTE_IMAGE"
docker image push "$REMOTE_IMAGE"

# Вывод итогов
echo '>>> Образ опубликован'

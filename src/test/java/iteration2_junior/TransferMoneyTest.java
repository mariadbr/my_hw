package iteration2_junior;

import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static io.restassured.RestAssured.given;

public class TransferMoneyTest extends SetupRestAssured {
    private final float MAX_DEPOSIT_AMOUNT = 5000.0f;

    @Test
    public void userCanTransferValidAmountIntoSomeonesAccount() {
        //создать 1 юзера
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                        "username": "sam001",
                        "password": "Hel0!!@@:124",
                        "role": "USER"
                        }
                        """)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        //получить токен 1 юзера
        String firstUserAuthToken = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                        "username": "sam001",
                        "password": "Hel0!!@@:124"
                        }
                        """)
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("authorization");

        //создать аккаунт 1 юзера
        int firstUserAccountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", firstUserAuthToken)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        //создать 2 юзера
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                        "username": "roy399",
                        "password": "Hel0!!@@:124",
                        "role": "USER"
                        }
                        """)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        //получить токен 2 юзера
        String secondUserAuthToken = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                        "username": "roy399",
                        "password": "Hel0!!@@:124"
                        }
                        """)
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("authorization");

        //создать аккаунт 2 юзера
        int secondUserAccountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", secondUserAuthToken)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        //депозит денег
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", firstUserAuthToken)
                .body("""
                        {
                          "id": %s,
                          "balance": %s
                        }
                        """.formatted(firstUserAccountId, MAX_DEPOSIT_AMOUNT))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .log().all()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        //трансфер денег
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", firstUserAuthToken)
                .body("""
                        {
                          "senderAccountId": %s,
                          "receiverAccountId": %s,
                          "amount": 50.05
                        }
                        """.formatted(firstUserAccountId, secondUserAccountId))
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        //проверка счета 1 пользователя
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", firstUserAuthToken)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("[0].balance", Matchers.equalTo(4949.95f));

        //проверка счета 2 пользователя
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", secondUserAuthToken)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("[0].balance", Matchers.equalTo(50.05f));

        //проверка транзакции
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", firstUserAuthToken)
                .pathParam("accountId", firstUserAccountId)
                .get("http://localhost:4111/api/v1/accounts/{accountId}/transactions")
                .then()
                .assertThat()
                .body("find { it.type == 'TRANSFER_OUT' }.amount", Matchers.equalTo(50.05f))
                .body("find { it.type == 'TRANSFER_OUT' }.relatedAccountId", Matchers.equalTo(secondUserAccountId));
    }

    @CsvSource(value =
            //positive cases
            {"0.01, stas113", "9999.99, stas114"})
    @ParameterizedTest
    public void userCanTransferAmountIntoOwnAccount(float amount, String userName) {
        //создание пользователя
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                          "username": "%s",
                          "password": "Hello!!@@:123",
                          "role": "USER"
                        }
                        
                        """.formatted(userName))
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        //получить токен юзера
        String userAuthToken = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username":"%s",
                          "password": "Hello!!@@:123"
                        }
                        """.formatted(userName))
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("authorization");

        //создать 1 аккаунт юзера
        int userFirstAccountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", userAuthToken)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        //создать 2 аккаунт юзера
        int userSecondAccountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", userAuthToken)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        //депозит денег
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", userAuthToken)
                .body("""
                        {
                          "id": %s,
                          "balance": %s
                        }
                        """.formatted(userFirstAccountId, MAX_DEPOSIT_AMOUNT))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .log().all()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", userAuthToken)
                .body("""
                        {
                          "id": %s,
                          "balance": %s
                        }
                        """.formatted(userFirstAccountId, MAX_DEPOSIT_AMOUNT))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .log().all()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        //трансфер денег
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", userAuthToken)
                .body("""
                        {
                          "senderAccountId": %s,
                          "receiverAccountId": %s,
                          "amount": %s
                        }
                        """.formatted(userFirstAccountId, userSecondAccountId, amount))
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        //проверка счета
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", userAuthToken)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("find { it.id == %s }.balance".formatted(userSecondAccountId),
                        Matchers.equalTo(amount));

        //проверка транзакции
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", userAuthToken)
                .pathParam("accountId", userFirstAccountId)
                .get("http://localhost:4111/api/v1/accounts/{accountId}/transactions")
                .then()
                .assertThat()
                .body("find { it.type == 'TRANSFER_OUT' }.amount", Matchers.equalTo(amount))
                .body("find { it.type == 'TRANSFER_OUT' }.relatedAccountId", Matchers.equalTo(userSecondAccountId));
    }

    @Test
    public void userCannotTransferNegativeAmountIntoOwnAccount() {
        //создание пользователя
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                          "username": "sta112",
                          "password": "Hello!!@@:123",
                          "role": "USER"
                        }
                        
                        """)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        //получить токен юзера
        String userAuthToken = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username":"sta112",
                          "password": "Hello!!@@:123"
                        }
                        """)
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("authorization");

        //создать 1 аккаунт юзера
        int userFirstAccountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", userAuthToken)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        //создать 2 аккаунт юзера
        int userSecondAccountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", userAuthToken)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        //депозит денег
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", userAuthToken)
                .body("""
                        {
                          "id": %s,
                          "balance": %s
                        }
                        """.formatted(userFirstAccountId, MAX_DEPOSIT_AMOUNT))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .log().all()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        //трансфер денег
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", userAuthToken)
                .body("""
                        {
                          "senderAccountId": %s,
                          "receiverAccountId": %s,
                          "amount": -1
                        }
                        """.formatted(userFirstAccountId, userSecondAccountId))
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        //проверка счета
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", userAuthToken)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("find { it.id == %s }.balance".formatted(userSecondAccountId),
                        Matchers.equalTo(0.0f));
    }

    @Test
    public void userCannotTransferInvalidAmountIntoSomeonesAccount() {
        //создание пользователя
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                          "username": "maria111",
                          "password": "Hello!!@@:123",
                          "role": "USER"
                        }
                        
                        """)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        //получить токен юзера
        String firstUserAuthToken = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username":"maria111",
                          "password": "Hello!!@@:123"
                        }
                        """)
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("authorization");

        //создать аккаунт 1 юзера
        int firstUserAccountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", firstUserAuthToken)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        //создание 2 пользователя
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                          "username": "maria112",
                          "password": "Hello!!@@:123",
                          "role": "USER"
                        }
                        
                        """)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        //получить токен 2 юзера
        String secondUserAuthToken = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username":"maria112",
                          "password": "Hello!!@@:123"
                        }
                        """)
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("authorization");

        //создать аккаунт 2 юзера
        int secondUserAccountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", secondUserAuthToken)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        //депозит денег
        for (int i = 0; i < 3; i++) {
            given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("authorization", firstUserAuthToken)
                    .body("""
                        {
                          "id": %s,
                          "balance": %s
                        }
                        """.formatted(firstUserAccountId, MAX_DEPOSIT_AMOUNT))
                    .post("http://localhost:4111/api/v1/accounts/deposit")
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_OK);
        }

        //трансфер денег
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", firstUserAuthToken)
                .body("""
                        {
                          "senderAccountId": %s,
                          "receiverAccountId": %s,
                          "amount": 10000.01
                        }
                        """.formatted(firstUserAccountId, secondUserAccountId))
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        //проверка счета 1 пользователя
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", firstUserAuthToken)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("[0].balance", Matchers.equalTo(MAX_DEPOSIT_AMOUNT * 3));

        //проверка счета 2 пользователя
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", secondUserAuthToken)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("[0].balance", Matchers.equalTo(0.0f));
    }

    @Test
    public void userCannotTransferAmountBiggerThanAvailableIntoSomeonesAccount() {
        //создание пользователя
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                          "username": "sta113",
                          "password": "Hello!!@@:123",
                          "role": "USER"
                        }
                        
                        """)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        //получить токен юзера
        String firstUserAuthToken = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username":"sta113",
                          "password": "Hello!!@@:123"
                        }
                        """)
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("authorization");

        //создать аккаунт 1 юзера
        int firstUserAccountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", firstUserAuthToken)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        //создание 2 пользователя
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                          "username": "sta114",
                          "password": "Hello!!@@:123",
                          "role": "USER"
                        }
                        
                        """)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        //получить токен 2 юзера
        String secondUserAuthToken = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "username":"sta114",
                          "password": "Hello!!@@:123"
                        }
                        """)
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("authorization");

        //создать аккаунт 2 юзера
        int secondUserAccountId = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", secondUserAuthToken)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        //депозит денег
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", firstUserAuthToken)
                .body("""
                        {
                          "id": %s,
                          "balance": 100
                        }
                        """.formatted(firstUserAccountId))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        //трансфер денег
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", firstUserAuthToken)
                .body("""
                        {
                          "senderAccountId": %s,
                          "receiverAccountId": %s,
                          "amount": 100.5
                        }
                        """.formatted(firstUserAccountId, secondUserAccountId))
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        //проверка счета 1 пользователя
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", firstUserAuthToken)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("[0].balance", Matchers.equalTo(100f));

        //проверка счета 2 пользователя
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", secondUserAuthToken)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("[0].balance", Matchers.equalTo(0.0f));
    }
}

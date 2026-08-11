import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static io.restassured.RestAssured.given;

public class TransferMoneyTest extends SetupRestAssured {
    @Test
    public void userCanTransferValidAmountIntoSomeonesAccount() {
        //создать 1 юзера
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                        "username": "sam207",
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
                        "username": "sam207",
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
                        "username": "roy208",
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
                        "username": "roy208",
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
                          "balance": 100
                        }
                        """.formatted(firstUserAccountId))
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

        //проверка
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
                          "balance": 10000
                        }
                        """.formatted(userFirstAccountId))
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

        //проверка
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
                          "balance": 100
                        }
                        """.formatted(userFirstAccountId))
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
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", firstUserAuthToken)
                .body("""
                        {
                          "id": %s,
                          "balance": 10001
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
                          "amount": 10000.01
                        }
                        """.formatted(firstUserAccountId, secondUserAccountId))
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

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

    }
}

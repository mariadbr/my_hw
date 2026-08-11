import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

public class DepositMoneyTest extends SetupRestAssured {

    public static Stream<Arguments> validUserAndAmountDataForDeposit() {
        return Stream.of(
                Arguments.of(generateValidUsername(), 0.01f),
                Arguments.of(generateValidUsername(), 4999.99f));
    }

    @MethodSource("validUserAndAmountDataForDeposit")
    @ParameterizedTest
    public void userCanDepositValidAmountIntoOwnAccount(String username, float amount) {
        //создать юзера
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                          "username": "%s",
                          "password": "Hel0!!@@:124",
                          "role": "USER"
                        }
                        
                        """.formatted(username))
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        //получить токен юзера
        String userAuthToken = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                          "username": "%s",
                          "password": "Hel0!!@@:124"
                        }
                        
                        """.formatted(username))
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("authorization");

        //создать аккаунт
        int accountId = given()
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
                        """.formatted(accountId, amount))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .log().all()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("balance", Matchers.equalTo(amount));
    }

    @Test
    public void userCannotDepositInvalidAmountIntoOwnAccount() {
            //создать юзера
            given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("authorization", "Basic YWRtaW46YWRtaW4=")
                    .body("""
                        {
                        "username": "kate233",
                        "password": "Hel0!!@@:124",
                        "role": "USER"
                        }
                        """)
                    .post("http://localhost:4111/api/v1/admin/users")
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_CREATED);

            //получить токен
            String userAuthToken = given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .header("authorization", "Basic YWRtaW46YWRtaW4=")
                    .body("""
                        {
                         "username": "kate233",
                         "password": "Hel0!!@@:124"
                        }
                        """)
                    .post("http://localhost:4111/api/v1/auth/login")
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_OK)
                    .extract()
                    .header("authorization");

            //создать аккаунт
            int accountId = given()
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
                        "balance": 5000.01
                        }
                        """.formatted(accountId))
                    .post("http://localhost:4111/api/v1/accounts/deposit")
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void userCannotDepositNegativeAmountIntoOwnAccount() {
        //создать юзера
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                        "username": "kate22236",
                        "password": "Hel0!!@@:124",
                        "role": "USER"
                        }
                        """)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        //получить токен
        String userAuthToken = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                         "username": "kate22236",
                         "password": "Hel0!!@@:124"
                        }
                        """)
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("authorization");

        //создать аккаунт
        int accountId = given()
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
                        "balance": -5
                        }
                        """.formatted(accountId))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void userCannotDepositAmountIntoSomeonesAccount() {
        //создать 1 юзера
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                        "username": "kate203",
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
                        "username": "kate203",
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
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", firstUserAuthToken)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        //создать 2 юзера
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                        "username": "kate204",
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
                        "username": "kate204",
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
                          "balance": 100.5
                        }
                        """.formatted(secondUserAccountId))
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body(Matchers.equalTo("Unauthorized access to account"));
    }

    @Test
    public void userCannotDepositAmountIntoNonExistentAccount() {
        //создать 1 юзера
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                        "username": "mate205",
                        "password": "Hel0!!@@:124",
                        "role": "USER"
                        }
                        """)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        //получить токен 1 юзера
        String userAuthToken = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                        "username": "mate205",
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
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", userAuthToken)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        //депозит денег
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", userAuthToken)
                .body("""
                        {
                          "id": 95,
                          "balance": 100
                        }
                        """)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .log().all()
                .assertThat()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body(Matchers.equalTo("Unauthorized access to account"));
    }
}

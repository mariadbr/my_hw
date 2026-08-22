package iteration2_junior;

import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

public class UpdateNameTest extends SetupRestAssured {
    @Test
    public void userCanUpdateNameInProfile() {
        //создание пользователя
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                          "username": "sta115",
                          "password": "Hello!!@@:125",
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
                          "username":"sta115",
                          "password": "Hello!!@@:125"
                        }
                        """)
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("authorization");

        //проверка имени
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", userAuthToken)
                .body("""
                        {
                          "name": "Stas St"
                        }
                        """)
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("customer.name", Matchers.equalTo("Stas St"))
                .body("message", Matchers.equalTo("Profile updated successfully"));

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", userAuthToken)
                .get("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("name", Matchers.equalTo("Stas St"));
    }

    public static Stream<Arguments> userData() {
        return Stream.of(
                Arguments.of(generateValidUsername(), "Stas"),
                Arguments.of(generateValidUsername(), "StasMar"),
                Arguments.of(generateValidUsername(), "StasMar "),
                Arguments.of(generateValidUsername(), "12 13"),
                Arguments.of(generateValidUsername(), "St@s Sta$"),
                Arguments.of(generateValidUsername(), ""));
    }


    @MethodSource("userData")
    @ParameterizedTest
    public void userCannotUpdateNameToInvalidName(String username, String name) {
        //создание пользователя
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", "Basic YWRtaW46YWRtaW4=")
                .body("""
                        {
                          "username": "%s",
                          "password": "Hello!!@@:125",
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
                .body("""
                        {
                          "username":"%s",
                          "password": "Hello!!@@:125"
                        }
                        """.formatted(username))
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("authorization");

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", userAuthToken)
                .body("""
                        {
                          "name": "%s"
                        }
                        """.formatted(name))
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(Matchers.equalTo("Name must contain two words with letters only"));

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("authorization", userAuthToken)
                .get("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("name", Matchers.equalTo(null));
    }
}

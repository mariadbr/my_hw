package iteration2;

import generators.RandomData;
import models.CreateUserRequest;
import models.UpdateNameRequest;
import models.UserRole;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.AdminCreateUserRequester;
import requests.UpdateNameRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

public class UpdateNameTest extends BaseTest {
    @Test
    public void userCanUpdateNameInProfile() {
        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        //создание пользователя
        new AdminCreateUserRequester(RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(createUserRequest);

        String updatedName = "Stas St";

        UpdateNameRequest updateNameRequest = UpdateNameRequest.builder()
                .name(updatedName)
                .build();

        new UpdateNameRequester(RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .sendPut(updateNameRequest)
                .body("customer.name", Matchers.equalTo(updatedName))
                .body("message", Matchers.equalTo("Profile updated successfully"));
    }


    public static Stream<Arguments> userData() {
        return Stream.of(
                Arguments.of(RandomData.getUsername(), "Stas"),
                Arguments.of(RandomData.getUsername(), "StasMar"),
                Arguments.of(RandomData.getUsername(), "StasMar "),
                Arguments.of(RandomData.getUsername(), "12 13"),
                Arguments.of(RandomData.getUsername(), "St@s Sta$"),
                Arguments.of(RandomData.getUsername(), ""));
    }


    @MethodSource("userData")
    @ParameterizedTest
    public void userCannotUpdateNameToInvalidName(String username, String name) {
        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .username(username)
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        //создание пользователя
        new AdminCreateUserRequester(RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(createUserRequest);

        UpdateNameRequest updateNameRequest = UpdateNameRequest.builder()
                .name(name)
                .build();

        new UpdateNameRequester(RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequestWithText("Name must contain two words with letters only"))
                .sendPut(updateNameRequest);
    }
}
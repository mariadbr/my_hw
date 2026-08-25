package iteration2_middle;

import generators.RandomData;
import models.*;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.AdminCreateUserRequester;
import requests.GetCustomerProfileRequester;
import requests.UpdateNameRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

public class UpdateNameTest extends BaseTest {
    @Test
    public void userCanUpdateNameInProfile() {
        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        //создание пользователя
        CreateUserResponse createUserResponse = new AdminCreateUserRequester(RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(createUserRequest)
                .extract()
                .as(CreateUserResponse.class);

        String updatedName = "Stas St";

        UpdateNameRequest updateNameRequest = UpdateNameRequest.builder()
                .name(updatedName)
                .build();

        //изменение имени
        new UpdateNameRequester(RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .sendPut(updateNameRequest)
                .body("customer.name", Matchers.equalTo(updatedName))
                .body("message", Matchers.equalTo("Profile updated successfully"));

        //проверка имени
        GetCustomerProfileResponse getCustomerProfileResponse = new GetCustomerProfileRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(GetCustomerProfileResponse.class);

        softly.assertThat(getCustomerProfileResponse.getId()).isEqualTo(createUserResponse.getId());
        softly.assertThat(getCustomerProfileResponse.getUsername()).isEqualTo(createUserRequest.getUsername());
        softly.assertThat(getCustomerProfileResponse.getPassword()).isNotEqualTo(createUserRequest.getPassword());
        softly.assertThat(getCustomerProfileResponse.getName()).isEqualTo(updatedName);
        softly.assertThat(getCustomerProfileResponse.getRole().toString()).isEqualTo(UserRole.USER.toString());
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
        CreateUserResponse createUserResponse = new AdminCreateUserRequester(RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(createUserRequest)
                .extract()
                .as(CreateUserResponse.class);

        UpdateNameRequest updateNameRequest = UpdateNameRequest.builder()
                .name(name)
                .build();

        new UpdateNameRequester(RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequestWithText("Name must contain two words with letters only"))
                .sendPut(updateNameRequest);

        //проверка имени
        GetCustomerProfileResponse getCustomerProfileResponse = new GetCustomerProfileRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(GetCustomerProfileResponse.class);

        softly.assertThat(getCustomerProfileResponse.getId()).isEqualTo(createUserResponse.getId());
        softly.assertThat(getCustomerProfileResponse.getUsername()).isEqualTo(createUserRequest.getUsername());
        softly.assertThat(getCustomerProfileResponse.getPassword()).isNotEqualTo(createUserRequest.getPassword());
        softly.assertThat(getCustomerProfileResponse.getName()).isEqualTo(null);
        softly.assertThat(getCustomerProfileResponse.getRole().toString()).isEqualTo(UserRole.USER.toString());
    }
}
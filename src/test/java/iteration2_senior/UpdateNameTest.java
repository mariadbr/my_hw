package iteration2_senior;

import generators.RandomData;
import generators.RandomModelGenerator;
import models.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

public class UpdateNameTest extends BaseTest {
    @Test
    public void userCanUpdateNameInProfile() {
        CreateUserRequest createUserRequest = RandomModelGenerator.generate(CreateUserRequest.class);

        //создание пользователя
        CreateUserResponse createUserResponse = AdminSteps.createUserReturnResponse(createUserRequest);

        String updatedName = RandomData.getRandomValidName();

        UpdateCustomerProfileRequest updateCustomerProfileRequest = RandomModelGenerator.generate(UpdateCustomerProfileRequest.class);
        updateCustomerProfileRequest.setName(updatedName);

        //изменение имени
        UpdateCustomerProfileResponse updateCustomerProfileResponse = new UserSteps(createUserRequest.getUsername(), createUserRequest.getPassword())
                .updateProfile(updateCustomerProfileRequest);

        softly.assertThat(updateCustomerProfileResponse.getMessage()).isEqualTo(AlertMessage.PROFILE_UPDATED_SUCCESSFULLY.getMessage());
        softly.assertThat(updateCustomerProfileResponse.getCustomer().getId()).isEqualTo(createUserResponse.getId());
        softly.assertThat(updateCustomerProfileResponse.getCustomer().getUsername()).isEqualTo(createUserResponse.getUsername());
        softly.assertThat(updateCustomerProfileResponse.getCustomer().getPassword()).isNotEqualTo(createUserRequest.getPassword());
        softly.assertThat(updateCustomerProfileResponse.getCustomer().getName()).isEqualTo(updateCustomerProfileRequest.getName());
        softly.assertThat(updateCustomerProfileResponse.getCustomer().getRole().toString()).isEqualTo(createUserRequest.getRole());
        softly.assertThat(updateCustomerProfileResponse.getCustomer().getAccounts()).isEmpty();

        //проверка имени
        GetCustomerProfileResponse getCustomerProfileResponse = new UserSteps(
                createUserRequest.getUsername(), createUserRequest.getPassword()).getProfile();

        softly.assertThat(getCustomerProfileResponse.getId()).isEqualTo(createUserResponse.getId());
        softly.assertThat(getCustomerProfileResponse.getUsername()).isEqualTo(createUserRequest.getUsername());
        softly.assertThat(getCustomerProfileResponse.getPassword()).isNotEqualTo(createUserRequest.getPassword());
        softly.assertThat(getCustomerProfileResponse.getName()).isEqualTo(updatedName);
        softly.assertThat(getCustomerProfileResponse.getRole().toString()).isEqualTo(createUserRequest.getRole());
        softly.assertThat(getCustomerProfileResponse.getAccounts()).isEmpty();
    }

    @ValueSource(strings =
            //negative cases
            {"Stas", "StasMar", "StasMar ", "12 13", "St@s Sta$", ""})
    @ParameterizedTest
    public void userCannotUpdateNameToInvalidName(String name) {
        CreateUserRequest createUserRequest = RandomModelGenerator.generate(CreateUserRequest.class);

        //создание пользователя
        CreateUserResponse createUserResponse = AdminSteps.createUserReturnResponse(createUserRequest);

        UpdateCustomerProfileRequest updateCustomerProfileRequest = RandomModelGenerator.generate(UpdateCustomerProfileRequest.class);
        updateCustomerProfileRequest.setName(name);

        //изменение имени
        new CrudRequester(RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                Endpoint.UPDATE_CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsBadRequestWithText(AlertMessage.NAME_MUST_CONTAIN_TWO_WORDS.getMessage()))
                .put(updateCustomerProfileRequest);

        //проверка имени
        GetCustomerProfileResponse getCustomerProfileResponse = new UserSteps(createUserRequest.getUsername(), createUserRequest.getPassword())
                .getProfile();

        softly.assertThat(getCustomerProfileResponse.getId()).isEqualTo(createUserResponse.getId());
        softly.assertThat(getCustomerProfileResponse.getUsername()).isEqualTo(createUserRequest.getUsername());
        softly.assertThat(getCustomerProfileResponse.getPassword()).isNotEqualTo(createUserRequest.getPassword());
        softly.assertThat(getCustomerProfileResponse.getName()).isNull();
        softly.assertThat(getCustomerProfileResponse.getRole().toString()).isEqualTo(UserRole.USER.toString());
        softly.assertThat(getCustomerProfileResponse.getAccounts()).isEmpty();
    }
}
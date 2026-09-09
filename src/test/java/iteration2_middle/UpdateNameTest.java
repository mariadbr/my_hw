package iteration2_middle;

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
        CreateUserResponse createUserResponse = AdminSteps.createUserGetResponse(createUserRequest);
//        CreateUserResponse createUserResponse = new ValidatedCrudRequester<CreateUserResponse>(RequestSpecs.adminSpec(),
//                Endpoint.ADMIN_USERS,
//                ResponseSpecs.entityWasCreated())
//                .post(createUserRequest);

        String updatedName = "Stas St";

        UpdateCustomerProfileRequest updateCustomerProfileRequest = UpdateCustomerProfileRequest.builder()
                .name(updatedName)
                .build();

        //изменение имени
        UpdateCustomerProfileResponse updateCustomerProfileResponse = UserSteps.updateProfile(
                createUserRequest.getUsername(), createUserRequest.getPassword(), updateCustomerProfileRequest);
//        UpdateCustomerProfileResponse updateCustomerProfileResponse = new ValidatedCrudRequester<UpdateCustomerProfileResponse>(
//                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
//                Endpoint.UPDATE_CUSTOMER_PROFILE,
//                ResponseSpecs.requestReturnsOK())
//                .put(updateCustomerProfileRequest);

        softly.assertThat(updateCustomerProfileResponse.getCustomer().getName()).isEqualTo(updatedName);
        softly.assertThat(updateCustomerProfileResponse.getMessage()).isEqualTo(AlertMessage.PROFILE_UPDATED_SUCCESSFULLY.getMessage());

        //проверка имени
        GetCustomerProfileResponse getCustomerProfileResponse = UserSteps.getProfile(
                createUserRequest.getUsername(), createUserRequest.getPassword());
//        GetCustomerProfileResponse getCustomerProfileResponse = new ValidatedCrudRequester<GetCustomerProfileResponse>(
//                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
//                Endpoint.GET_CUSTOMER_PROFILE,
//                ResponseSpecs.requestReturnsOK())
//                .get();

        softly.assertThat(getCustomerProfileResponse.getId()).isEqualTo(createUserResponse.getId());
        softly.assertThat(getCustomerProfileResponse.getUsername()).isEqualTo(createUserRequest.getUsername());
        softly.assertThat(getCustomerProfileResponse.getPassword()).isNotEqualTo(createUserRequest.getPassword());
        softly.assertThat(getCustomerProfileResponse.getName()).isEqualTo(updatedName);
        softly.assertThat(getCustomerProfileResponse.getRole().toString()).isEqualTo(UserRole.USER.toString());
    }

    @ValueSource(strings =
            //negative cases
            {"Stas", "StasMar", "StasMar ", "12 13", "St@s Sta$", ""})
    @ParameterizedTest
    public void userCannotUpdateNameToInvalidName(String name) {
        CreateUserRequest createUserRequest = RandomModelGenerator.generate(CreateUserRequest.class);

        //создание пользователя
        CreateUserResponse createUserResponse = AdminSteps.createUserGetResponse(createUserRequest);
//        CreateUserResponse createUserResponse = new ValidatedCrudRequester<CreateUserResponse>(RequestSpecs.adminSpec(),
//                Endpoint.ADMIN_USERS,
//                ResponseSpecs.entityWasCreated())
//                .post(createUserRequest);

        UpdateCustomerProfileRequest updateCustomerProfileRequest = UpdateCustomerProfileRequest.builder()
                .name(name)
                .build();

        new CrudRequester(RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                Endpoint.UPDATE_CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsBadRequestWithText(AlertMessage.NAME_MUST_CONTAIN_TWO_WORDS.getMessage()))
                .put(updateCustomerProfileRequest);

        //проверка имени
        GetCustomerProfileResponse getCustomerProfileResponse = UserSteps.getProfile(
                createUserRequest.getUsername(), createUserRequest.getPassword());
//        GetCustomerProfileResponse getCustomerProfileResponse = new ValidatedCrudRequester<GetCustomerProfileResponse>(
//                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
//                Endpoint.GET_CUSTOMER_PROFILE,
//                ResponseSpecs.requestReturnsOK())
//                .get();

        softly.assertThat(getCustomerProfileResponse.getId()).isEqualTo(createUserResponse.getId());
        softly.assertThat(getCustomerProfileResponse.getUsername()).isEqualTo(createUserRequest.getUsername());
        softly.assertThat(getCustomerProfileResponse.getPassword()).isNotEqualTo(createUserRequest.getPassword());
        softly.assertThat(getCustomerProfileResponse.getName()).isNull();
        softly.assertThat(getCustomerProfileResponse.getRole().toString()).isEqualTo(UserRole.USER.toString());
    }
}
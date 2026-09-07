package iteration2_middle;

import generators.RandomData;
import generators.RandomModelGenerator;
import io.restassured.common.mapper.TypeRef;
import models.*;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import requests.AdminCreateUserRequester;
import requests.CreateAccountRequester;
import requests.DepositMoneyRequester;
import requests.GetCustomerAccountsRequester;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;

public class DepositMoneyTest extends BaseTest {

    public static Stream<Arguments> validUserAndAmountDataForDeposit() {
        return Stream.of(
                Arguments.of(RandomData.getUsername(), 0.01f),
                Arguments.of(RandomData.getUsername(), 4999.99f));
    }

    @MethodSource("validUserAndAmountDataForDeposit")
    @ParameterizedTest
    public void checkBoundaryValuesDepositPositiveCases(String username, float amount) {
        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .username(username)
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        //создать юзера
        new CrudRequester(RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USERS,
                ResponseSpecs.entityWasCreated())
                .post(createUserRequest);
//        new AdminCreateUserRequester(RequestSpecs.adminSpec(), ResponseSpecs.entityWasCreated())
//                .post(createUserRequest);

        //создать аккаунт
       CreateAccountResponse createAccountResponse = new ValidatedCrudRequester<CreateAccountResponse>(
               RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
               Endpoint.ACCOUNTS,
               ResponseSpecs.entityWasCreated())
               .post();
//        CreateAccountResponse createAccountResponse = new CreateAccountRequester(
//                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
//                ResponseSpecs.entityWasCreated())
//                .post()
//                .extract()
//                .as(CreateAccountResponse.class);

        //могу ли тоже как-то рандомно создать эту сущность?
        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
                .id(createAccountResponse.getId())
                .balance(amount)
                .build();

        //депозит денег
        DepositMoneyResponse depositMoneyResponse = new ValidatedCrudRequester<DepositMoneyResponse>(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsOK())
                .post(depositMoneyRequest);
//        DepositMoneyResponse depositMoneyResponse = new DepositMoneyRequester(
//                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
//                ResponseSpecs.requestReturnsOK())
//                .post(depositMoneyRequest)
//                .extract()
//                .as(DepositMoneyResponse.class);

        assertThat(depositMoneyResponse.getBalance(), Matchers.equalTo(amount));

        //проверка
        List<GetCustomerAccountsResponse> list = new ValidatedCrudRequester<GetCustomerAccountsResponse>(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK())
                .getList(new TypeRef<List<GetCustomerAccountsResponse>>() {});

//        List<GetCustomerAccountResponse> accountResponseList = new GetCustomerAccountsRequester(
//                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
//                ResponseSpecs.requestReturnsOK())
//                .get()
//                .extract()
//                .as(new TypeRef<List<GetCustomerAccountResponse>>() {});

        GetCustomerAccountsResponse account = list.stream()
                .filter(getCustomerAccountsResponse -> getCustomerAccountsResponse.getId() == createAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        assertThat(account.getBalance(), Matchers.equalTo(amount));
    }

    @ParameterizedTest
    @ValueSource(floats = {5000.01f})
    public void checkBoundaryValuesDepositNegativeCase(float amount) {
        CreateUserRequest createUserRequest = RandomModelGenerator.generate(CreateUserRequest.class);
//        CreateUserRequest createUserRequest = CreateUserRequest.builder()
//                .username(RandomData.getUsername())
//                .password(RandomData.getPassword())
//                .role(UserRole.USER.toString())
//                .build();

        //создать юзера
        new CrudRequester(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USERS,
                ResponseSpecs.entityWasCreated())
                .post(createUserRequest);

//        new AdminCreateUserRequester(RequestSpecs.adminSpec(),
//                ResponseSpecs.entityWasCreated())
//                .post(createUserRequest);

        //создать аккаунт
        CreateAccountResponse createAccountResponse = new ValidatedCrudRequester<CreateAccountResponse>(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated())
                .post();
//        CreateAccountResponse createAccountResponse = new CreateAccountRequester(
//                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
//                ResponseSpecs.entityWasCreated())
//                .post()
//                .extract()
//                .as(CreateAccountResponse.class);

        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
                .id(createAccountResponse.getId())
                .balance(amount)
                .build();

        //депозит денег
        new CrudRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsBadRequestWithText(AlertMessage.DEPOSIT_AMOUNT_CANNOT_EXCEED_5000.getMessage()))
                .post(depositMoneyRequest);
//        new DepositMoneyRequester(RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
//                ResponseSpecs.requestReturnsBadRequestWithText( AlertMessage.DEPOSIT_AMOUNT_CANNOT_EXCEED_5000.getMessage()))
//                .post(depositMoneyRequest);

        //проверка
        List<GetCustomerAccountsResponse> getCustomerAccountResponseList = new ValidatedCrudRequester<GetCustomerAccountsResponse>(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK())
                .getList(new TypeRef<List<GetCustomerAccountsResponse>>() {});
//        List<GetCustomerAccountResponse> getCustomerAccountResponseList = new GetCustomerAccountsRequester(
//                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
//                ResponseSpecs.requestReturnsOK())
//                .get()
//                .extract()
//                .as(new TypeRef<List<GetCustomerAccountResponse>>() {});

        GetCustomerAccountsResponse account = getCustomerAccountResponseList.stream()
                .filter(getCustomerAccountsResponse -> getCustomerAccountsResponse.getId() == createAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(account.getBalance()).isEqualTo(0.0f);
        softly.assertThat(account.getTransactions()).isEmpty();
    }

    @Test
    public void userCannotDepositNegativeAmountIntoOwnAccount() {
        CreateUserRequest createUserRequest = RandomModelGenerator.generate(CreateUserRequest.class);
//        CreateUserRequest createUserRequest = CreateUserRequest.builder()
//                .username(RandomData.getUsername())
//                .password(RandomData.getPassword())
//                .role(UserRole.USER.toString())
//                .build();

        //создать юзера
        new CrudRequester(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USERS,
                ResponseSpecs.entityWasCreated())
                .post(createUserRequest);
//        new AdminCreateUserRequester(RequestSpecs.adminSpec(),
//                ResponseSpecs.entityWasCreated())
//                .post(createUserRequest);

        //создать аккаунт
        CreateAccountResponse createAccountResponse = new ValidatedCrudRequester<CreateAccountResponse>(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated())
                .post();
//        CreateAccountResponse createAccountResponse = new CreateAccountRequester(
//                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
//                ResponseSpecs.entityWasCreated())
//                .post()
//                .extract()
//                .as(CreateAccountResponse.class);

        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
                .id(createAccountResponse.getId())
                .balance(RandomData.getRandomNegativeFloat())
                .build();

        //депозит денег
        new CrudRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsBadRequestWithText(AlertMessage.DEPOSIT_AMOUNT_MUST_BE_AT_LEAST_001.getMessage()))
                .post(depositMoneyRequest);
//        new DepositMoneyRequester(RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
//                ResponseSpecs.requestReturnsBadRequestWithText( AlertMessage.DEPOSIT_AMOUNT_MUST_BE_AT_LEAST_001.getMessage()))
//                .post(depositMoneyRequest);

        //проверка
        List<GetCustomerAccountsResponse> getCustomerAccountsResponseList = new ValidatedCrudRequester<GetCustomerAccountsResponse>(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK())
                .getList(new TypeRef<List<GetCustomerAccountsResponse>>() {});
//        List<GetCustomerAccountsResponse> getCustomerAccountsResponseList = new GetCustomerAccountsRequester(
//                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
//                ResponseSpecs.requestReturnsOK())
//                .get()
//                .extract()
//                .as(new TypeRef<List<GetCustomerAccountsResponse>>() {});

        GetCustomerAccountsResponse account = getCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse -> getCustomerAccountResponse.getId() == createAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(account.getBalance()).isEqualTo(0.0f);
        softly.assertThat(account.getTransactions()).isEmpty();
    }

    @Test
    public void userCannotDepositAmountIntoSomeonesAccount() {
        CreateUserRequest createFirstUserRequest = RandomModelGenerator.generate(CreateUserRequest.class);
//        CreateUserRequest createFirstUserRequest = CreateUserRequest.builder()
//                .username(RandomData.getUsername())
//                .password(RandomData.getPassword())
//                .role(UserRole.USER.toString())
//                .build();

        //создать 1 юзера
        new CrudRequester(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USERS,
                ResponseSpecs.entityWasCreated())
                .post(createFirstUserRequest);
//        new AdminCreateUserRequester(RequestSpecs.adminSpec(),
//                ResponseSpecs.entityWasCreated())
//                .post(createFirstUserRequest);

        //создать аккаунт 1 юзера
        new CrudRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated())
                .post();
//        new CreateAccountRequester(
//                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
//                ResponseSpecs.entityWasCreated())
//                .post();

        CreateUserRequest createSecondUserRequest = RandomModelGenerator.generate(CreateUserRequest.class);
//        CreateUserRequest createSecondUserRequest = CreateUserRequest.builder()
//                .username(RandomData.getUsername())
//                .password(RandomData.getPassword())
//                .role(UserRole.USER.toString())
//                .build();

        //создать 2 юзера
        new CrudRequester(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USERS,
                ResponseSpecs.entityWasCreated())
                .post(createSecondUserRequest);
//        new AdminCreateUserRequester(RequestSpecs.adminSpec(),
//                ResponseSpecs.entityWasCreated())
//                .post(createSecondUserRequest);

        //создать аккаунт 2 юзера
        CreateAccountResponse createSecondUserAccountResponse = new ValidatedCrudRequester<CreateAccountResponse>(
                RequestSpecs.authAsUser(createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated())
                .post();
//        CreateAccountResponse createSecondUserAccountResponse = new CreateAccountRequester(
//                RequestSpecs.authAsUser(createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword()),
//                ResponseSpecs.entityWasCreated())
//                .post()
//                .extract()
//                .as(CreateAccountResponse.class);

        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
                .id(createSecondUserAccountResponse.getId())
                .balance(RandomData.getRandomPositiveFloat())
                .build();

        //депозит
        new CrudRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsForbidden(AlertMessage.UNAUTHORIZED_ACCESS_TO_ACCOUNT.getMessage()))
                .post(depositMoneyRequest);
//        new DepositMoneyRequester(
//                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
//                ResponseSpecs.requestReturnsForbidden(AlertMessage.UNAUTHORIZED_ACCESS_TO_ACCOUNT.getMessage()))
//                .post(depositMoneyRequest);

        //проверка аккаунта 1 юзера
        List<GetCustomerAccountsResponse> firstUserGetCustomerAccountsResponseList = new ValidatedCrudRequester<GetCustomerAccountsResponse>(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK())
                .getList(new TypeRef<List<GetCustomerAccountsResponse>>() {});
//        List<GetCustomerAccountsResponse> firstUserGetCustomerAccountsResponseList = new GetCustomerAccountsRequester(
//                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
//                ResponseSpecs.requestReturnsOK())
//                .get()
//                .extract()
//                .as(new TypeRef<List<GetCustomerAccountsResponse>>() {});

        softly.assertThat(firstUserGetCustomerAccountsResponseList.getFirst().getBalance()).isEqualTo(0.0f);
        softly.assertThat(firstUserGetCustomerAccountsResponseList.getFirst().getTransactions()).isEmpty();

        //проверка аккаунта 2 юзера
        List<GetCustomerAccountsResponse> secondUserGetCustomerAccountsResponseList = new ValidatedCrudRequester<GetCustomerAccountsResponse>(
                RequestSpecs.authAsUser(createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword()),
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK())
                .getList(new TypeRef<List<GetCustomerAccountsResponse>>() {});
//        List<GetCustomerAccountsResponse> secondUserGetCustomerAccountsResponseList = new GetCustomerAccountsRequester(
//                RequestSpecs.authAsUser(createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword()),
//                ResponseSpecs.requestReturnsOK())
//                .get()
//                .extract()
//                .as(new TypeRef<List<GetCustomerAccountsResponse>>() {});

        softly.assertThat(secondUserGetCustomerAccountsResponseList.getFirst().getBalance()).isEqualTo(0.0f);
        softly.assertThat(secondUserGetCustomerAccountsResponseList.getFirst().getTransactions()).isEmpty();
    }

    @Test
    public void userCannotDepositAmountIntoNonExistentAccount() {
        CreateUserRequest createUserRequest = RandomModelGenerator.generate(CreateUserRequest.class);
//        CreateUserRequest createUserRequest = CreateUserRequest.builder()
//                .username(RandomData.getUsername())
//                .password(RandomData.getPassword())
//                .role(UserRole.USER.toString())
//                .build();

        //создать юзера
        new CrudRequester(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USERS,
                ResponseSpecs.entityWasCreated())
                .post(createUserRequest);
//        new AdminCreateUserRequester(RequestSpecs.adminSpec(),
//                ResponseSpecs.entityWasCreated())
//                .post(createUserRequest);

        //создать аккаунт
        new CrudRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated())
                .post();
//        new CreateAccountRequester(
//                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
//                ResponseSpecs.entityWasCreated())
//                .post();

        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
                .id(1000)
                .balance(RandomData.getRandomPositiveFloat())
                .build();

        //депозит денег
        new CrudRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsForbidden(AlertMessage.UNAUTHORIZED_ACCESS_TO_ACCOUNT.getMessage()))
                .post(depositMoneyRequest);
//        new DepositMoneyRequester(RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
//                ResponseSpecs.requestReturnsForbidden(AlertMessage.UNAUTHORIZED_ACCESS_TO_ACCOUNT.getMessage()))
//                .post(depositMoneyRequest);

        //проверка
        List<GetCustomerAccountsResponse> getCustomerAccountsResponseList = new ValidatedCrudRequester<GetCustomerAccountsResponse>(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK())
                .getList(new TypeRef<List<GetCustomerAccountsResponse>>() {});
//        List<GetCustomerAccountsResponse> getCustomerAccountsResponseList = new GetCustomerAccountsRequester(
//                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
//                ResponseSpecs.requestReturnsOK())
//                .get()
//                .extract()
//                .as(new TypeRef<List<GetCustomerAccountsResponse>>() {});

        softly.assertThat(getCustomerAccountsResponseList.getFirst().getTransactions()).isEmpty();
        softly.assertThat(getCustomerAccountsResponseList.getFirst().getBalance()).isEqualTo(0.0f);
    }
}

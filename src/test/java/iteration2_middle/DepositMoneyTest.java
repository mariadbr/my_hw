package iteration2_middle;

import generators.RandomData;
import models.*;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequester;
import requests.steps.AdminSteps;
import models.UserBalanceDefaults;
import requests.steps.UserSteps;
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

        //создать аккаунт
        CreateAccountResponse createAccountResponse = UserSteps.createAccount(createUserRequest.getUsername(), createUserRequest.getPassword());
//       CreateAccountResponse createAccountResponse = new ValidatedCrudRequester<CreateAccountResponse>(
//               RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
//               Endpoint.ACCOUNTS,
//               ResponseSpecs.entityWasCreated())
//               .post();

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

        assertThat(depositMoneyResponse.getBalance(), Matchers.equalTo(amount));

        //проверка
        List<GetCustomerAccountsResponse> list = UserSteps.getAccounts(createUserRequest.getUsername(), createUserRequest.getPassword());
//        List<GetCustomerAccountsResponse> list = new ValidatedCrudRequester<GetCustomerAccountsResponse>(
//                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
//                Endpoint.CUSTOMER_ACCOUNTS,
//                ResponseSpecs.requestReturnsOK())
//                .getList(new TypeRef<List<GetCustomerAccountsResponse>>() {});

        GetCustomerAccountsResponse account = list.stream()
                .filter(getCustomerAccountsResponse -> getCustomerAccountsResponse.getId() == createAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        assertThat(account.getBalance(), Matchers.equalTo(amount));
    }

    @ParameterizedTest
    @ValueSource(floats = {5000.01f})
    public void checkBoundaryValuesDepositNegativeCase(float amount) {
//        CreateUserRequest createUserRequest = RandomModelGenerator.generate(CreateUserRequest.class);

        //создать юзера
        CreateUserRequest createUserRequest = AdminSteps.createUserGetRequest();
//        new CrudRequester(
//                RequestSpecs.adminSpec(),
//                Endpoint.ADMIN_USERS,
//                ResponseSpecs.entityWasCreated())
//                .post(createUserRequest);

        //создать аккаунт
        CreateAccountResponse createAccountResponse = UserSteps.createAccount(createUserRequest.getUsername(), createUserRequest.getPassword());
//        CreateAccountResponse createAccountResponse = new ValidatedCrudRequester<CreateAccountResponse>(
//                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
//                Endpoint.ACCOUNTS,
//                ResponseSpecs.entityWasCreated())
//                .post();

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

        //проверка
        List<GetCustomerAccountsResponse> getCustomerAccountResponseList = UserSteps.getAccounts(
                createUserRequest.getUsername(), createUserRequest.getPassword());
//        List<GetCustomerAccountsResponse> getCustomerAccountResponseList = new ValidatedCrudRequester<GetCustomerAccountsResponse>(
//                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
//                Endpoint.CUSTOMER_ACCOUNTS,
//                ResponseSpecs.requestReturnsOK())
//                .getList(new TypeRef<List<GetCustomerAccountsResponse>>() {});

        GetCustomerAccountsResponse account = getCustomerAccountResponseList.stream()
                .filter(getCustomerAccountsResponse -> getCustomerAccountsResponse.getId() == createAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(account.getBalance()).isEqualTo(UserBalanceDefaults.INITIAL_BALANCE.getAmount());
        softly.assertThat(account.getTransactions()).isEmpty();
    }

    @Test
    public void userCannotDepositNegativeAmountIntoOwnAccount() {
        //CreateUserRequest createUserRequest = RandomModelGenerator.generate(CreateUserRequest.class);

        //создать юзера
//        new CrudRequester(
//                RequestSpecs.adminSpec(),
//                Endpoint.ADMIN_USERS,
//                ResponseSpecs.entityWasCreated())
//                .post(createUserRequest);

        CreateUserRequest createUserRequest = AdminSteps.createUserGetRequest();

        //создать аккаунт
//        CreateAccountResponse createAccountResponse = new ValidatedCrudRequester<CreateAccountResponse>(
//                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
//                Endpoint.ACCOUNTS,
//                ResponseSpecs.entityWasCreated())
//                .post();
        CreateAccountResponse createAccountResponse = UserSteps.createAccount(createUserRequest.getUsername(), createUserRequest.getPassword());

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

        //проверка
//        List<GetCustomerAccountsResponse> getCustomerAccountsResponseList = new ValidatedCrudRequester<GetCustomerAccountsResponse>(
//                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
//                Endpoint.CUSTOMER_ACCOUNTS,
//                ResponseSpecs.requestReturnsOK())
//                .getList(new TypeRef<List<GetCustomerAccountsResponse>>() {});
        List<GetCustomerAccountsResponse> getCustomerAccountsResponseList = UserSteps.getAccounts(
                createUserRequest.getUsername(), createUserRequest.getPassword());

        GetCustomerAccountsResponse account = getCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse -> getCustomerAccountResponse.getId() == createAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(account.getBalance()).isEqualTo(UserBalanceDefaults.INITIAL_BALANCE.getAmount());
        softly.assertThat(account.getTransactions()).isEmpty();
    }

    @Test
    public void userCannotDepositAmountIntoSomeonesAccount() {
        //CreateUserRequest createFirstUserRequest = RandomModelGenerator.generate(CreateUserRequest.class);

        //создать 1 юзера
        CreateUserRequest createFirstUserRequest = AdminSteps.createUserGetRequest();
//        new CrudRequester(
//                RequestSpecs.adminSpec(),
//                Endpoint.ADMIN_USERS,
//                ResponseSpecs.entityWasCreated())
//                .post(createFirstUserRequest);

        //создать аккаунт 1 юзера
        UserSteps.createAccount(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword());
//        new CrudRequester(
//                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
//                Endpoint.ACCOUNTS,
//                ResponseSpecs.entityWasCreated())
//                .post();

        //CreateUserRequest createSecondUserRequest = RandomModelGenerator.generate(CreateUserRequest.class);

        //создать 2 юзера
        CreateUserRequest createSecondUserRequest = AdminSteps.createUserGetRequest();
//        new CrudRequester(
//                RequestSpecs.adminSpec(),
//                Endpoint.ADMIN_USERS,
//                ResponseSpecs.entityWasCreated())
//                .post(createSecondUserRequest);

        //создать аккаунт 2 юзера
        CreateAccountResponse createSecondUserAccountResponse = UserSteps.createAccount(
                createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword());
//        CreateAccountResponse createSecondUserAccountResponse = new ValidatedCrudRequester<CreateAccountResponse>(
//                RequestSpecs.authAsUser(createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword()),
//                Endpoint.ACCOUNTS,
//                ResponseSpecs.entityWasCreated())
//                .post();

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

        //проверка аккаунта 1 юзера
        List<GetCustomerAccountsResponse> firstUserGetCustomerAccountsResponseList = UserSteps.getAccounts(
                createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword());

//        List<GetCustomerAccountsResponse> firstUserGetCustomerAccountsResponseList = new ValidatedCrudRequester<GetCustomerAccountsResponse>(
//                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
//                Endpoint.CUSTOMER_ACCOUNTS,
//                ResponseSpecs.requestReturnsOK())
//                .getList(new TypeRef<List<GetCustomerAccountsResponse>>() {});

        softly.assertThat(firstUserGetCustomerAccountsResponseList.getFirst().getBalance()).isEqualTo(UserBalanceDefaults.INITIAL_BALANCE.getAmount());
        softly.assertThat(firstUserGetCustomerAccountsResponseList.getFirst().getTransactions()).isEmpty();

        //проверка аккаунта 2 юзера
        List<GetCustomerAccountsResponse> secondUserGetCustomerAccountsResponseList = UserSteps.getAccounts(
                createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword());

//        List<GetCustomerAccountsResponse> secondUserGetCustomerAccountsResponseList = new ValidatedCrudRequester<GetCustomerAccountsResponse>(
//                RequestSpecs.authAsUser(createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword()),
//                Endpoint.CUSTOMER_ACCOUNTS,
//                ResponseSpecs.requestReturnsOK())
//                .getList(new TypeRef<List<GetCustomerAccountsResponse>>() {});

        softly.assertThat(secondUserGetCustomerAccountsResponseList.getFirst().getBalance()).isEqualTo(UserBalanceDefaults.INITIAL_BALANCE.getAmount());
        softly.assertThat(secondUserGetCustomerAccountsResponseList.getFirst().getTransactions()).isEmpty();
    }

    @Test
    public void userCannotDepositAmountIntoNonExistentAccount() {
//        CreateUserRequest createUserRequest = RandomModelGenerator.generate(CreateUserRequest.class);

        //создать юзера
        CreateUserRequest createUserRequest = AdminSteps.createUserGetRequest();
//        new CrudRequester(
//                RequestSpecs.adminSpec(),
//                Endpoint.ADMIN_USERS,
//                ResponseSpecs.entityWasCreated())
//                .post(createUserRequest);

        //создать аккаунт
        UserSteps.createAccount(createUserRequest.getUsername(), createUserRequest.getPassword());
//        new CrudRequester(
//                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
//                Endpoint.ACCOUNTS,
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

        //проверка
        List<GetCustomerAccountsResponse> getCustomerAccountsResponseList = UserSteps.getAccounts(
                createUserRequest.getUsername(), createUserRequest.getPassword());
//        List<GetCustomerAccountsResponse> getCustomerAccountsResponseList = new ValidatedCrudRequester<GetCustomerAccountsResponse>(
//                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
//                Endpoint.CUSTOMER_ACCOUNTS,
//                ResponseSpecs.requestReturnsOK())
//                .getList(new TypeRef<List<GetCustomerAccountsResponse>>() {});

        softly.assertThat(getCustomerAccountsResponseList.getFirst().getTransactions()).isEmpty();
        softly.assertThat(getCustomerAccountsResponseList.getFirst().getBalance()).isEqualTo(UserBalanceDefaults.INITIAL_BALANCE.getAmount());
    }
}

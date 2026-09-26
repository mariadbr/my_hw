package iteration2.api;

import api.generators.RandomData;
import api.models.*;
import api.models.comparison.ModelAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.CrudRequester;
import api.requests.steps.AdminSteps;
import api.requests.steps.UserSteps;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

import java.util.List;

public class DepositMoneyTest extends BaseTest {

    @ValueSource(floats = {
            //positive cases
            0.01f, 4999.99f})
    @ParameterizedTest
    public void checkBoundaryValuesDepositPositiveCases(float amount) {
        //создать юзера
        CreateUserRequest createUserRequest = AdminSteps.createUserReturnRequest();

        //создать аккаунт
        CreateAccountResponse createAccountResponse = UserSteps.createAccount(createUserRequest.getUsername(), createUserRequest.getPassword());

        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
                .id(createAccountResponse.getId())
                .balance(amount)
                .build();

        //депозит денег
        DepositMoneyResponse depositMoneyResponse = UserSteps.depositMoney(
                createUserRequest.getUsername(), createUserRequest.getPassword(), depositMoneyRequest);

        ModelAssertions.assertThatModels(depositMoneyRequest, depositMoneyResponse).match();
        softly.assertThat(depositMoneyResponse.getTransactions()).isNotEmpty();

        //проверка
        List<GetCustomerAccountsResponse> list = UserSteps.getAccounts(createUserRequest.getUsername(), createUserRequest.getPassword());

        GetCustomerAccountsResponse account = list.stream()
                .filter(getCustomerAccountsResponse -> getCustomerAccountsResponse.getId() == createAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(account.getBalance()).isEqualTo(amount);
        softly.assertThat(account.getTransactions()).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(floats = {5000.01f})
    public void checkBoundaryValuesDepositNegativeCase(float amount) {
        //создать юзера
        CreateUserRequest createUserRequest = AdminSteps.createUserReturnRequest();

        //создать аккаунт
        CreateAccountResponse createAccountResponse = UserSteps.createAccount(createUserRequest.getUsername(), createUserRequest.getPassword());

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

        GetCustomerAccountsResponse account = getCustomerAccountResponseList.stream()
                .filter(getCustomerAccountsResponse -> getCustomerAccountsResponse.getId() == createAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(account.getBalance()).isEqualTo(UserBalanceDefaults.INITIAL_BALANCE.getAmount());
        softly.assertThat(account.getTransactions()).isEmpty();
        softly.assertThat(account.getId()).isEqualTo(createAccountResponse.getId());
    }

    @Test
    public void userCannotDepositNegativeAmountIntoOwnAccount() {
        //создать юзера
        CreateUserRequest createUserRequest = AdminSteps.createUserReturnRequest();

        //создать аккаунт
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
        List<GetCustomerAccountsResponse> getCustomerAccountsResponseList = UserSteps.getAccounts(
                createUserRequest.getUsername(), createUserRequest.getPassword());

        GetCustomerAccountsResponse account = getCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse -> getCustomerAccountResponse.getId() == createAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(account.getBalance()).isEqualTo(UserBalanceDefaults.INITIAL_BALANCE.getAmount());
        softly.assertThat(account.getTransactions()).isEmpty();
        softly.assertThat(account.getId()).isEqualTo(createAccountResponse.getId());
    }

    @Test
    public void userCannotDepositAmountIntoSomeonesAccount() {
        //создать 1 юзера
        CreateUserRequest createFirstUserRequest = AdminSteps.createUserReturnRequest();

        //создать аккаунт 1 юзера
        CreateAccountResponse createFirstUserAccountResponse = UserSteps.createAccount(
                createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword());

        //создать 2 юзера
        CreateUserRequest createSecondUserRequest = AdminSteps.createUserReturnRequest();

       //создать аккаунт 2 юзера
        CreateAccountResponse createSecondUserAccountResponse = UserSteps.createAccount(
                createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword());

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

        softly.assertThat(firstUserGetCustomerAccountsResponseList.getFirst().getBalance()).isEqualTo(UserBalanceDefaults.INITIAL_BALANCE.getAmount());
        softly.assertThat(firstUserGetCustomerAccountsResponseList.getFirst().getTransactions()).isEmpty();
        softly.assertThat(firstUserGetCustomerAccountsResponseList.getFirst().getId()).isEqualTo(createFirstUserAccountResponse.getId());

        //проверка аккаунта 2 юзера
        List<GetCustomerAccountsResponse> secondUserGetCustomerAccountsResponseList = UserSteps.getAccounts(
                createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword());

        softly.assertThat(secondUserGetCustomerAccountsResponseList.getFirst().getBalance()).isEqualTo(UserBalanceDefaults.INITIAL_BALANCE.getAmount());
        softly.assertThat(secondUserGetCustomerAccountsResponseList.getFirst().getTransactions()).isEmpty();
        softly.assertThat(secondUserGetCustomerAccountsResponseList.getFirst().getId()).isEqualTo(createSecondUserAccountResponse.getId());

    }

    @Test
    public void userCannotDepositAmountIntoNonExistentAccount() {
        //создать юзера
        CreateUserRequest createUserRequest = AdminSteps.createUserReturnRequest();

        //создать аккаунт
        CreateAccountResponse createAccountResponse = UserSteps.createAccount(
                createUserRequest.getUsername(), createUserRequest.getPassword());

        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
                .id(RandomData.getRandomAccountId())
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

        softly.assertThat(getCustomerAccountsResponseList.getFirst().getId()).isEqualTo(createAccountResponse.getId());
        softly.assertThat(getCustomerAccountsResponseList.getFirst().getTransactions()).isEmpty();
        softly.assertThat(getCustomerAccountsResponseList.getFirst().getBalance()).isEqualTo(UserBalanceDefaults.INITIAL_BALANCE.getAmount());
    }
}

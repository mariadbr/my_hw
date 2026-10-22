package iteration2.api;

import api.dao.AccountDao;
import api.dao.comparison.DaoAndModelAssertions;
import api.generators.RandomData;
import api.generators.RandomModelGenerator;
import api.models.*;
import api.models.comparison.ModelAssertions;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.CrudRequester;
import api.requests.steps.AdminSteps;
import api.requests.steps.DataBaseSteps;
import api.requests.steps.UserSteps;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import common.annotations.TestVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

public class DepositMoneyTest extends BaseTest {
    @ValueSource(floats = {
            //positive cases
            0.01f, 4999.99f})
    @ParameterizedTest
    @TestVersion({"with_database_with_fix"})
    public void checkBoundaryValuesDepositPositiveCases(float amount) {
        //создать юзера
        CreateUserRequest createUserRequest = AdminSteps.createUserReturnRequest();

        //создать аккаунт
        CreateAccountResponse createAccountResponse = new UserSteps(createUserRequest.getUsername(), createUserRequest.getPassword())
                .createAccount();

        DepositMoneyRequest depositMoneyRequest = RandomModelGenerator.generate(DepositMoneyRequest.class);
        depositMoneyRequest.setId(createAccountResponse.getId());
        depositMoneyRequest.setBalance(amount);

        //депозит денег
        DepositMoneyResponse depositMoneyResponse = new UserSteps(createUserRequest.getUsername(), createUserRequest.getPassword())
                .depositMoney(depositMoneyRequest);

        ModelAssertions.assertThatModels(depositMoneyRequest, depositMoneyResponse).match();
        softly.assertThat(depositMoneyResponse.getTransactions()).isNotEmpty();

        //проверка
        List<GetCustomerAccountsResponse> list = new UserSteps(createUserRequest.getUsername(), createUserRequest.getPassword())
                .getAccounts();

        GetCustomerAccountsResponse account = list.stream()
                .filter(getCustomerAccountsResponse -> getCustomerAccountsResponse.getId() == createAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(account.getBalance()).isEqualTo(amount);
        softly.assertThat(account.getTransactions()).isNotEmpty();

        //проверка через бд
        AccountDao accountDao = DataBaseSteps.getAccountByAccountNumber(createAccountResponse.getAccountNumber());

        DaoAndModelAssertions.assertThat(depositMoneyResponse, accountDao).match();
    }

    @ParameterizedTest
    @ValueSource(floats = {5000.01f})
    @TestVersion({"with_database_with_fix"})
    public void checkBoundaryValuesDepositNegativeCase(float amount) {
        //создать юзера
        CreateUserRequest createUserRequest = AdminSteps.createUserReturnRequest();

        //создать аккаунт
        CreateAccountResponse createAccountResponse = new UserSteps(createUserRequest.getUsername(), createUserRequest.getPassword())
                .createAccount();

        DepositMoneyRequest depositMoneyRequest = RandomModelGenerator.generate(DepositMoneyRequest.class);
        depositMoneyRequest.setId(createAccountResponse.getId());
        depositMoneyRequest.setBalance(amount);

        //депозит денег
        new CrudRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsBadRequestWithText(AlertMessage.DEPOSIT_AMOUNT_EXCEEDS_5000.getMessage()))
                .post(depositMoneyRequest);

        //проверка
        List<GetCustomerAccountsResponse> getCustomerAccountResponseList = new UserSteps(createUserRequest.getUsername(), createUserRequest.getPassword())
                .getAccounts();

        GetCustomerAccountsResponse account = getCustomerAccountResponseList.stream()
                .filter(getCustomerAccountsResponse -> getCustomerAccountsResponse.getId() == createAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(account.getBalance()).isEqualTo(UserBalanceDefaults.INITIAL_BALANCE.getAmount());
        softly.assertThat(account.getTransactions()).isEmpty();
        softly.assertThat(account.getId()).isEqualTo(createAccountResponse.getId());

        //проверка через бд
        AccountDao accountDao = DataBaseSteps.getAccountByAccountNumber(createAccountResponse.getAccountNumber());

        DaoAndModelAssertions.assertThat(createAccountResponse, accountDao).match();
    }

    @Test
    @TestVersion({"with_database_with_fix"})
    public void userCannotDepositNegativeAmountIntoOwnAccount() {
        //создать юзера
        CreateUserRequest createUserRequest = AdminSteps.createUserReturnRequest();

        //создать аккаунт
        CreateAccountResponse createAccountResponse = new UserSteps(createUserRequest.getUsername(), createUserRequest.getPassword())
                .createAccount();

        DepositMoneyRequest depositMoneyRequest = RandomModelGenerator.generate(DepositMoneyRequest.class);
        depositMoneyRequest.setId(createAccountResponse.getId());
        depositMoneyRequest.setBalance(RandomData.getRandomNegativeFloat());


        //депозит денег
        new CrudRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsBadRequestWithText(AlertMessage.INVALID_ACCOUNT_OR_AMOUNT.getMessage()))
                .post(depositMoneyRequest);

        //проверка
        List<GetCustomerAccountsResponse> getCustomerAccountsResponseList = new UserSteps(createUserRequest.getUsername(), createUserRequest.getPassword())
                .getAccounts();

        GetCustomerAccountsResponse account = getCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse -> getCustomerAccountResponse.getId() == createAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(account.getBalance()).isEqualTo(UserBalanceDefaults.INITIAL_BALANCE.getAmount());
        softly.assertThat(account.getTransactions()).isEmpty();
        softly.assertThat(account.getId()).isEqualTo(createAccountResponse.getId());

        //проверка через бд
        AccountDao accountDao = DataBaseSteps.getAccountByAccountNumber(createAccountResponse.getAccountNumber());

        DaoAndModelAssertions.assertThat(createAccountResponse, accountDao).match();
    }

    @Test
    @TestVersion({"with_database_with_fix"})
    public void userCannotDepositAmountIntoSomeonesAccount() {
        //создать 1 юзера
        CreateUserRequest createFirstUserRequest = AdminSteps.createUserReturnRequest();

        //создать аккаунт 1 юзера
        CreateAccountResponse createFirstUserAccountResponse = new UserSteps(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword())
                .createAccount();

        //создать 2 юзера
        CreateUserRequest createSecondUserRequest = AdminSteps.createUserReturnRequest();

        //создать аккаунт 2 юзера
        CreateAccountResponse createSecondUserAccountResponse = new UserSteps(createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword())
                .createAccount();

        DepositMoneyRequest depositMoneyRequest = RandomModelGenerator.generate(DepositMoneyRequest.class);
        depositMoneyRequest.setId(createSecondUserAccountResponse.getId());

        //депозит
        new CrudRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsForbidden(AlertMessage.UNAUTHORIZED_ACCESS_TO_ACCOUNT.getMessage()))
                .post(depositMoneyRequest);

        //проверка аккаунта 1 юзера
        List<GetCustomerAccountsResponse> firstUserGetCustomerAccountsResponseList = new UserSteps(
                createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()).getAccounts();

        softly.assertThat(firstUserGetCustomerAccountsResponseList.getFirst().getBalance()).isEqualTo(UserBalanceDefaults.INITIAL_BALANCE.getAmount());
        softly.assertThat(firstUserGetCustomerAccountsResponseList.getFirst().getTransactions()).isEmpty();
        softly.assertThat(firstUserGetCustomerAccountsResponseList.getFirst().getId()).isEqualTo(createFirstUserAccountResponse.getId());

        //проверка аккаунта 2 юзера
        List<GetCustomerAccountsResponse> secondUserGetCustomerAccountsResponseList = new UserSteps(
                createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword()).getAccounts();

        softly.assertThat(secondUserGetCustomerAccountsResponseList.getFirst().getBalance()).isEqualTo(UserBalanceDefaults.INITIAL_BALANCE.getAmount());
        softly.assertThat(secondUserGetCustomerAccountsResponseList.getFirst().getTransactions()).isEmpty();
        softly.assertThat(secondUserGetCustomerAccountsResponseList.getFirst().getId()).isEqualTo(createSecondUserAccountResponse.getId());

        //проверка через бд
        AccountDao firstUserAccountDao = DataBaseSteps.getAccountByAccountNumber(createFirstUserAccountResponse.getAccountNumber());

        DaoAndModelAssertions.assertThat(createFirstUserAccountResponse, firstUserAccountDao).match();

        AccountDao secondUserAccountDao = DataBaseSteps.getAccountByAccountNumber(createSecondUserAccountResponse.getAccountNumber());

        DaoAndModelAssertions.assertThat(createSecondUserAccountResponse, secondUserAccountDao).match();
    }

    @Test
    @TestVersion({"with_database_with_fix"})
    public void userCannotDepositAmountIntoNonExistentAccount() {
        //создать юзера
        CreateUserRequest createUserRequest = AdminSteps.createUserReturnRequest();

        //создать аккаунт
        CreateAccountResponse createAccountResponse = new UserSteps(createUserRequest.getUsername(), createUserRequest.getPassword())
                .createAccount();

        DepositMoneyRequest depositMoneyRequest = RandomModelGenerator.generate(DepositMoneyRequest.class);
        depositMoneyRequest.setId(RandomData.getRandomAccountId());

        //депозит денег
        new CrudRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsForbidden(AlertMessage.UNAUTHORIZED_ACCESS_TO_ACCOUNT.getMessage()))
                .post(depositMoneyRequest);

        //проверка
        List<GetCustomerAccountsResponse> getCustomerAccountsResponseList = new UserSteps(createUserRequest.getUsername(), createUserRequest.getPassword())
                .getAccounts();

        softly.assertThat(getCustomerAccountsResponseList.getFirst().getId()).isEqualTo(createAccountResponse.getId());
        softly.assertThat(getCustomerAccountsResponseList.getFirst().getTransactions()).isEmpty();
        softly.assertThat(getCustomerAccountsResponseList.getFirst().getBalance()).isEqualTo(UserBalanceDefaults.INITIAL_BALANCE.getAmount());

        //проверка через бд
        AccountDao accountDao = DataBaseSteps.getAccountByAccountNumber(createAccountResponse.getAccountNumber());

        DaoAndModelAssertions.assertThat(createAccountResponse, accountDao).match();
    }
}

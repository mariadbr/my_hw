package iteration2_senior;

import generators.RandomData;
import generators.RandomModelGenerator;
import models.*;
import models.comparison.ModelAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.List;

public class DepositMoneyTest extends BaseTest {
    //вернуться к 3 строчке deposit request и посомтреть с генерацией

    @ValueSource(floats = {
            //positive cases
            0.01f, 4999.99f})
    @ParameterizedTest
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
    }

    @ParameterizedTest
    @ValueSource(floats = {5000.01f})
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
                ResponseSpecs.requestReturnsBadRequestWithText(AlertMessage.DEPOSIT_AMOUNT_CANNOT_EXCEED_5000.getMessage()))
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
    }

    @Test
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
                ResponseSpecs.requestReturnsBadRequestWithText(AlertMessage.DEPOSIT_AMOUNT_MUST_BE_AT_LEAST_001.getMessage()))
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
    }

    @Test
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

    }

    @Test
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
    }
}
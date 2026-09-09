package iteration2_senior;

import generators.RandomData;
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

import static models.UserBalanceDefaults.MAX_DEPOSIT_AMOUNT;
import static org.assertj.core.api.AssertionsForClassTypes.within;

public class TransferMoneyTest extends BaseTest {
    @Test
    public void userCanTransferValidAmountIntoSomeonesAccount() {
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

        //депозит
        UserSteps.depositMaxDepositAmount(
                createFirstUserAccountResponse.getId(),
                createFirstUserRequest.getUsername(),
                createFirstUserRequest.getPassword());

        float randomTransferAmount = RandomData.getRandomPositiveFloat();

        //трансфер
        TransferMoneyRequest transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(createFirstUserAccountResponse.getId())
                .receiverAccountId(createSecondUserAccountResponse.getId())
                .amount(randomTransferAmount)
                .build();

        TransferMoneyResponse transferMoneyResponse = UserSteps.transferMoney(
                createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword(), transferMoneyRequest);

        ModelAssertions.assertThatModels(transferMoneyRequest, transferMoneyResponse).match();
        softly.assertThat(transferMoneyResponse.getMessage()).isEqualTo(AlertMessage.TRANSFER_SUCCESSFUL.getMessage());

        //проверка счета 1 пользователя
        List<GetCustomerAccountsResponse> firstUserGetCustomerAccountsResponseList = UserSteps.getAccounts(
                createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword());

        GetCustomerAccountsResponse firstUserGetCustomerAccountsResponse = firstUserGetCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse ->
                        getCustomerAccountResponse.getId() == createFirstUserAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(firstUserGetCustomerAccountsResponse.getBalance()).isCloseTo(
                MAX_DEPOSIT_AMOUNT.getAmount() - randomTransferAmount, within(0.001f));
        softly.assertThat(firstUserGetCustomerAccountsResponse.getTransactions().size()).isEqualTo(2);

        //проверка счета 2 пользователя
        List<GetCustomerAccountsResponse> secondUserGetCustomerAccountsResponseList = UserSteps.getAccounts(
                createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword());

        GetCustomerAccountsResponse secondUserGetCustomerAccountsResponse = secondUserGetCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse ->
                        getCustomerAccountResponse.getId() == createSecondUserAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(secondUserGetCustomerAccountsResponse.getBalance()).isEqualTo(randomTransferAmount);
        softly.assertThat(secondUserGetCustomerAccountsResponse.getTransactions().size()).isEqualTo(1);

        //проверка транзакции
        List<GetTransactionResponse> firstUserGetTransactionResponseList = UserSteps.getTransactions(
                createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword(), createFirstUserAccountResponse.getId());

        GetTransactionResponse firstUserGetTransactionResponse = firstUserGetTransactionResponseList.stream()
                .filter(transactionResponse -> transactionResponse.getType() == TransactionType.TRANSFER_OUT)
                .findFirst()
                .orElseThrow();

        List<GetTransactionResponse> secondUserGetTransactionResponseList = UserSteps.getTransactions(
                createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword(), createSecondUserAccountResponse.getId());

        GetTransactionResponse secondUserGetTransactionResponse = secondUserGetTransactionResponseList.stream()
                .filter(transactionResponse -> transactionResponse.getType() == TransactionType.TRANSFER_IN)
                .findFirst()
                .orElseThrow();

        softly.assertThat(firstUserGetTransactionResponse.getId()).isNotNull();
        softly.assertThat(firstUserGetTransactionResponse.getAmount()).isEqualTo(randomTransferAmount);
        softly.assertThat(firstUserGetTransactionResponse.getRelatedAccountId()).isEqualTo(createSecondUserAccountResponse.getId());

        softly.assertThat(secondUserGetTransactionResponse.getId()).isNotNull();
        softly.assertThat(secondUserGetTransactionResponse.getAmount()).isEqualTo(randomTransferAmount);
        softly.assertThat(secondUserGetTransactionResponse.getRelatedAccountId()).isEqualTo(createFirstUserAccountResponse.getId());
    }

    @ValueSource(floats =
            //positive cases
            {0.01f, 9999.99f})
    @ParameterizedTest
    public void checkBoundaryValuesTransferBetweenOwnAccountsPositiveCases(float amount) {
        //создать юзера
        CreateUserRequest createUserRequest = AdminSteps.createUserReturnRequest();

        //создать 1 аккаунт
        CreateAccountResponse createFirstAccountResponse = UserSteps.createAccount(
                createUserRequest.getUsername(), createUserRequest.getPassword());

        //создать 2 аккаунт
        CreateAccountResponse createSecondAccountResponse = UserSteps.createAccount(
                createUserRequest.getUsername(), createUserRequest.getPassword());

        //депозит денег
        for (int i = 0; i < 2; i++) {
            UserSteps.depositMaxDepositAmount(
                    createFirstAccountResponse.getId(), createUserRequest.getUsername(), createUserRequest.getPassword());
        }

        //трансфер
        TransferMoneyRequest transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(createFirstAccountResponse.getId())
                .receiverAccountId(createSecondAccountResponse.getId())
                .amount(amount)
                .build();


        TransferMoneyResponse transferMoneyResponse = UserSteps.transferMoney(
                createUserRequest.getUsername(), createUserRequest.getPassword(), transferMoneyRequest);

        ModelAssertions.assertThatModels(transferMoneyRequest, transferMoneyResponse).match();
        softly.assertThat(transferMoneyResponse.getMessage()).isEqualTo(AlertMessage.TRANSFER_SUCCESSFUL.getMessage());

        //проверка 1 и 2 счета
        List<GetCustomerAccountsResponse> getCustomerAccountsResponseList = UserSteps.getAccounts(
                createUserRequest.getUsername(), createUserRequest.getPassword());

        GetCustomerAccountsResponse firstGetCustomerAccountsResponse = getCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse ->
                        getCustomerAccountResponse.getId() == createFirstAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        GetCustomerAccountsResponse secondGetCustomerAccountsResponse = getCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse ->
                        getCustomerAccountResponse.getId() == createSecondAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(firstGetCustomerAccountsResponse.getId()).isEqualTo(createFirstAccountResponse.getId());
        softly.assertThat(firstGetCustomerAccountsResponse.getBalance()).isCloseTo(
                MAX_DEPOSIT_AMOUNT.getAmount() * 2 - amount, within(0.001f));
        softly.assertThat(firstGetCustomerAccountsResponse.getTransactions()).isNotEmpty();

        softly.assertThat(secondGetCustomerAccountsResponse.getId()).isEqualTo(createSecondAccountResponse.getId());
        softly.assertThat(secondGetCustomerAccountsResponse.getBalance()).isEqualTo(amount);
        softly.assertThat(secondGetCustomerAccountsResponse.getTransactions()).isNotEmpty();
    }

    @Test
    public void userCannotTransferNegativeAmountIntoOwnAccount() {
        //создать юзера
        CreateUserRequest createUserRequest = AdminSteps.createUserReturnRequest();

        //создать 1 аккаунт
        CreateAccountResponse createFirstAccountResponse = UserSteps.createAccount(
                createUserRequest.getUsername(), createUserRequest.getPassword());

        //создать 2 аккаунт
        CreateAccountResponse createSecondAccountResponse = UserSteps.createAccount(
                createUserRequest.getUsername(), createUserRequest.getPassword());

       //депозит денег
        UserSteps.depositMaxDepositAmount(createFirstAccountResponse.getId(), createUserRequest.getUsername(), createUserRequest.getPassword());

        //трансфер
        TransferMoneyRequest transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(createFirstAccountResponse.getId())
                .receiverAccountId(createSecondAccountResponse.getId())
                .amount(RandomData.getRandomNegativeFloat())
                .build();

        new CrudRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.requestReturnsBadRequestWithText(AlertMessage.TRANSFER_AMOUNT_MUST_BE_AT_LEAST_001.getMessage()))
                .post(transferMoneyRequest);

        //проверка 1 и 2 счета
        List<GetCustomerAccountsResponse> getCustomerAccountsResponseList = UserSteps.getAccounts(
                createUserRequest.getUsername(), createUserRequest.getPassword());

        GetCustomerAccountsResponse firstGetCustomerAccountsResponse = getCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse ->
                        getCustomerAccountResponse.getId() == createFirstAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        GetCustomerAccountsResponse secondGetCustomerAccountsResponse = getCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse ->
                        getCustomerAccountResponse.getId() == createSecondAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(firstGetCustomerAccountsResponse.getBalance()).isEqualTo(MAX_DEPOSIT_AMOUNT.getAmount());
        softly.assertThat(firstGetCustomerAccountsResponse.getId()).isEqualTo(createFirstAccountResponse.getId());
        softly.assertThat(firstGetCustomerAccountsResponse.getTransactions().size()).isEqualTo(1);

        softly.assertThat(secondGetCustomerAccountsResponse.getBalance()).isEqualTo(UserBalanceDefaults.INITIAL_BALANCE.getAmount());
        softly.assertThat(secondGetCustomerAccountsResponse.getId()).isEqualTo(createSecondAccountResponse.getId());
        softly.assertThat(secondGetCustomerAccountsResponse.getTransactions()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(floats = {10000.01f})
    public void checkBoundaryValuesTransferToSomeonesAccountNegativeCase(float amount) {
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

        //депозит
        for (int i = 0; i < 3; i++) {
            UserSteps.depositMaxDepositAmount(
                    createFirstUserAccountResponse.getId(), createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword());
        }

        //трансфер
        TransferMoneyRequest transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(createFirstUserAccountResponse.getId())
                .receiverAccountId(createSecondUserAccountResponse.getId())
                .amount(amount)
                .build();

        new CrudRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.requestReturnsBadRequestWithText(AlertMessage.TRANSFER_AMOUNT_CANNOT_EXCEED_10000.getMessage()))
                .post(transferMoneyRequest);

        //проверка счета 1 пользователя
        List<GetCustomerAccountsResponse> firstUserGetCustomerAccountsResponseList = UserSteps.getAccounts(
                createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword());

        GetCustomerAccountsResponse firstUserGetCustomerAccountsResponse = firstUserGetCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse ->
                        getCustomerAccountResponse.getId() == createFirstUserAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        //проверка счета 2 пользователя
        List<GetCustomerAccountsResponse> secondUserGetCustomerAccountsResponseList = UserSteps.getAccounts(
                createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword());

        GetCustomerAccountsResponse secondUserGetCustomerAccountsResponse = secondUserGetCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse ->
                        getCustomerAccountResponse.getId() == createSecondUserAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(firstUserGetCustomerAccountsResponse.getBalance()).isEqualTo(MAX_DEPOSIT_AMOUNT.getAmount() * 3);
        softly.assertThat(firstUserGetCustomerAccountsResponse.getId()).isEqualTo(createFirstUserAccountResponse.getId());
        softly.assertThat(firstUserGetCustomerAccountsResponse.getTransactions().size()).isEqualTo(3);

        softly.assertThat(secondUserGetCustomerAccountsResponse.getBalance()).isEqualTo(UserBalanceDefaults.INITIAL_BALANCE.getAmount());
        softly.assertThat(secondUserGetCustomerAccountsResponse.getId()).isEqualTo(createSecondUserAccountResponse.getId());
        softly.assertThat(secondUserGetCustomerAccountsResponse.getTransactions()).isEmpty();
    }

    @Test
    public void userCannotTransferAmountBiggerThanAvailableIntoSomeonesAccount() {
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

        //депозит
        UserSteps.depositMaxDepositAmount(
                createFirstUserAccountResponse.getId(), createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword());

        //трансфер
        TransferMoneyRequest transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(createFirstUserAccountResponse.getId())
                .receiverAccountId(createSecondUserAccountResponse.getId())
                .amount(MAX_DEPOSIT_AMOUNT.getAmount() + RandomData.getRandomPositiveFloat())
                .build();

        new CrudRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.requestReturnsBadRequestWithText(AlertMessage.INVALID_TRANSFER.getMessage()))
                .post(transferMoneyRequest);

        //проверка счета 1 пользователя
        List<GetCustomerAccountsResponse> firstUserGetCustomerAccountsResponseList = UserSteps.getAccounts(
                createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword());

        GetCustomerAccountsResponse firstUserGetCustomerAccountsResponse = firstUserGetCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse ->
                        getCustomerAccountResponse.getId() == createFirstUserAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        //проверка счета 2 пользователя
        List<GetCustomerAccountsResponse> secondUserGetCustomerAccountsResponseList = UserSteps.getAccounts(
                createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword());

        GetCustomerAccountsResponse secondUserGetCustomerAccountsResponse = secondUserGetCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse ->
                        getCustomerAccountResponse.getId() == createSecondUserAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(firstUserGetCustomerAccountsResponse.getBalance()).isEqualTo(MAX_DEPOSIT_AMOUNT.getAmount());
        softly.assertThat(firstUserGetCustomerAccountsResponse.getId()).isEqualTo(createFirstUserAccountResponse.getId());
        softly.assertThat(firstUserGetCustomerAccountsResponse.getTransactions().size()).isEqualTo(1);

        softly.assertThat(secondUserGetCustomerAccountsResponse.getBalance()).isEqualTo(UserBalanceDefaults.INITIAL_BALANCE.getAmount());
        softly.assertThat(secondUserGetCustomerAccountsResponse.getId()).isEqualTo(createSecondUserAccountResponse.getId());
        softly.assertThat(secondUserGetCustomerAccountsResponse.getTransactions()).isEmpty();
    }
}

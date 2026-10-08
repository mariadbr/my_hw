package iteration2.api;

import api.generators.RandomData;
import api.generators.RandomModelGenerator;
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

import static api.models.UserBalanceDefaults.MAX_DEPOSIT_AMOUNT;
import static org.assertj.core.api.AssertionsForClassTypes.within;

public class TransferMoneyTest extends BaseTest {

    @Test
    public void userCanTransferValidAmountIntoSomeonesAccount() {
        //создать 1 юзера
        CreateUserRequest createFirstUserRequest = AdminSteps.createUserReturnRequest();

        //создать аккаунт 1 юзера
        CreateAccountResponse createFirstUserAccountResponse = new UserSteps(
                createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()).createAccount();

        //создать 2 юзера
        CreateUserRequest createSecondUserRequest = AdminSteps.createUserReturnRequest();

        //создать аккаунт 2 юзера
        CreateAccountResponse createSecondUserAccountResponse = new UserSteps(
                createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword()).createAccount();

        //депозит
        new UserSteps(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword())
                .depositMaxDepositAmount(createFirstUserAccountResponse.getId());

        //трансфер
        TransferMoneyRequest transferMoneyRequest = RandomModelGenerator.generate(TransferMoneyRequest.class);
        transferMoneyRequest.setSenderAccountId(createFirstUserAccountResponse.getId());
        transferMoneyRequest.setReceiverAccountId(createSecondUserAccountResponse.getId());

        float transferAmount = transferMoneyRequest.getAmount();

        TransferMoneyResponse transferMoneyResponse = new UserSteps(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword())
                .transferMoney(transferMoneyRequest);

        ModelAssertions.assertThatModels(transferMoneyRequest, transferMoneyResponse).match();
        softly.assertThat(transferMoneyResponse.getMessage()).isEqualTo(AlertMessage.TRANSFER_SUCCESSFUL.getMessage());

        //проверка счета 1 пользователя
        List<GetCustomerAccountsResponse> firstUserGetCustomerAccountsResponseList = new UserSteps(
                createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()).getAccounts();

        GetCustomerAccountsResponse firstUserGetCustomerAccountsResponse = firstUserGetCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse ->
                        getCustomerAccountResponse.getId() == createFirstUserAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(firstUserGetCustomerAccountsResponse.getBalance()).isCloseTo(
                MAX_DEPOSIT_AMOUNT.getAmount() - transferAmount, within(MARGIN_OF_ERROR));
        softly.assertThat(firstUserGetCustomerAccountsResponse.getTransactions().size()).isEqualTo(2);

        //проверка счета 2 пользователя
        List<GetCustomerAccountsResponse> secondUserGetCustomerAccountsResponseList = new UserSteps(
                createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword()).getAccounts();

        GetCustomerAccountsResponse secondUserGetCustomerAccountsResponse = secondUserGetCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse ->
                        getCustomerAccountResponse.getId() == createSecondUserAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(secondUserGetCustomerAccountsResponse.getBalance()).isEqualTo(transferAmount);
        softly.assertThat(secondUserGetCustomerAccountsResponse.getTransactions().size()).isEqualTo(1);

        //проверка транзакции
        List<GetTransactionResponse> firstUserGetTransactionResponseList = new UserSteps(
                createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()).getTransactions(createFirstUserAccountResponse.getId());

        GetTransactionResponse firstUserGetTransactionResponse = firstUserGetTransactionResponseList.stream()
                .filter(transactionResponse -> transactionResponse.getType() == TransactionType.TRANSFER_OUT)
                .findFirst()
                .orElseThrow();

        List<GetTransactionResponse> secondUserGetTransactionResponseList = new UserSteps(
                createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword())
                .getTransactions(createSecondUserAccountResponse.getId());

        GetTransactionResponse secondUserGetTransactionResponse = secondUserGetTransactionResponseList.stream()
                .filter(transactionResponse -> transactionResponse.getType() == TransactionType.TRANSFER_IN)
                .findFirst()
                .orElseThrow();

        softly.assertThat(firstUserGetTransactionResponse.getId()).isNotNull();
        softly.assertThat(firstUserGetTransactionResponse.getAmount()).isEqualTo(transferAmount);
        softly.assertThat(firstUserGetTransactionResponse.getRelatedAccountId()).isEqualTo(createSecondUserAccountResponse.getId());

        softly.assertThat(secondUserGetTransactionResponse.getId()).isNotNull();
        softly.assertThat(secondUserGetTransactionResponse.getAmount()).isEqualTo(transferAmount);
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
        CreateAccountResponse createFirstAccountResponse = new UserSteps(createUserRequest.getUsername(), createUserRequest.getPassword())
                .createAccount();

        //создать 2 аккаунт
        CreateAccountResponse createSecondAccountResponse = new UserSteps(createUserRequest.getUsername(), createUserRequest.getPassword())
                .createAccount();

        //депозит денег
        repeat(2, () -> new UserSteps(createUserRequest.getUsername(), createUserRequest.getPassword())
                .depositMaxDepositAmount(createFirstAccountResponse.getId()));

        //трансфер
        TransferMoneyRequest transferMoneyRequest = RandomModelGenerator.generate(TransferMoneyRequest.class);
        transferMoneyRequest.setSenderAccountId(createFirstAccountResponse.getId());
        transferMoneyRequest.setReceiverAccountId(createSecondAccountResponse.getId());
        transferMoneyRequest.setAmount(amount);


        TransferMoneyResponse transferMoneyResponse = new UserSteps(
                createUserRequest.getUsername(), createUserRequest.getPassword()).transferMoney(transferMoneyRequest);

        ModelAssertions.assertThatModels(transferMoneyRequest, transferMoneyResponse).match();
        softly.assertThat(transferMoneyResponse.getMessage()).isEqualTo(AlertMessage.TRANSFER_SUCCESSFUL.getMessage());

        //проверка 1 и 2 счета
        List<GetCustomerAccountsResponse> getCustomerAccountsResponseList = new UserSteps(
                createUserRequest.getUsername(), createUserRequest.getPassword()).getAccounts();

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
                MAX_DEPOSIT_AMOUNT.getAmount() * 2 - amount, within(MARGIN_OF_ERROR));
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
        CreateAccountResponse createFirstAccountResponse = new UserSteps(
                createUserRequest.getUsername(), createUserRequest.getPassword()).createAccount();

        //создать 2 аккаунт
        CreateAccountResponse createSecondAccountResponse = new UserSteps(
                createUserRequest.getUsername(), createUserRequest.getPassword()).createAccount();

        //депозит денег
        new UserSteps(createUserRequest.getUsername(), createUserRequest.getPassword())
                .depositMaxDepositAmount(createFirstAccountResponse.getId());

        //трансфер
        TransferMoneyRequest transferMoneyRequest = RandomModelGenerator.generate(TransferMoneyRequest.class);
        transferMoneyRequest.setSenderAccountId(createFirstAccountResponse.getId());
        transferMoneyRequest.setReceiverAccountId(createSecondAccountResponse.getId());
        transferMoneyRequest.setAmount(RandomData.getRandomNegativeFloat());

        new CrudRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.requestReturnsBadRequestWithText(AlertMessage.TRANSFER_AMOUNT_MUST_BE_AT_LEAST_001.getMessage()))
                .post(transferMoneyRequest);

        //проверка 1 и 2 счета
        List<GetCustomerAccountsResponse> getCustomerAccountsResponseList = new UserSteps(createUserRequest.getUsername(), createUserRequest.getPassword())
                .getAccounts();

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
        CreateAccountResponse createFirstUserAccountResponse = new UserSteps(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword())
                .createAccount();

        //создать 2 юзера
        CreateUserRequest createSecondUserRequest = AdminSteps.createUserReturnRequest();

        //создать аккаунт 2 юзера
        CreateAccountResponse createSecondUserAccountResponse = new UserSteps(createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword())
                .createAccount();

        //депозит
        repeat(3, () -> new UserSteps(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword())
                .depositMaxDepositAmount(createFirstUserAccountResponse.getId()));

        //трансфер
        TransferMoneyRequest transferMoneyRequest = RandomModelGenerator.generate(TransferMoneyRequest.class);
        transferMoneyRequest.setSenderAccountId(createFirstUserAccountResponse.getId());
        transferMoneyRequest.setReceiverAccountId(createSecondUserAccountResponse.getId());
        transferMoneyRequest.setAmount(amount);

        new CrudRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.requestReturnsBadRequestWithText(AlertMessage.TRANSFER_AMOUNT_CANNOT_EXCEED_10000.getMessage()))
                .post(transferMoneyRequest);

        //проверка счета 1 пользователя
        List<GetCustomerAccountsResponse> firstUserGetCustomerAccountsResponseList = new UserSteps(
                createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()).getAccounts();

        GetCustomerAccountsResponse firstUserGetCustomerAccountsResponse = firstUserGetCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse ->
                        getCustomerAccountResponse.getId() == createFirstUserAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        //проверка счета 2 пользователя
        List<GetCustomerAccountsResponse> secondUserGetCustomerAccountsResponseList = new UserSteps(
                createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword()).getAccounts();

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
        CreateAccountResponse createFirstUserAccountResponse = new UserSteps(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword())
                .createAccount();

        //создать 2 юзера
        CreateUserRequest createSecondUserRequest = AdminSteps.createUserReturnRequest();

        //создать аккаунт 2 юзера
        CreateAccountResponse createSecondUserAccountResponse = new UserSteps(createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword())
                .createAccount();

        //депозит
        new UserSteps(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword())
                .depositMaxDepositAmount(createFirstUserAccountResponse.getId());

        //трансфер
        TransferMoneyRequest transferMoneyRequest = RandomModelGenerator.generate(TransferMoneyRequest.class);
        transferMoneyRequest.setSenderAccountId(createFirstUserAccountResponse.getId());
        transferMoneyRequest.setReceiverAccountId(createSecondUserAccountResponse.getId());
        transferMoneyRequest.setAmount(MAX_DEPOSIT_AMOUNT.getAmount() + RandomData.getRandomPositiveFloat());

        new CrudRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.requestReturnsBadRequestWithText(AlertMessage.INVALID_TRANSFER.getMessage()))
                .post(transferMoneyRequest);

        //проверка счета 1 пользователя
        List<GetCustomerAccountsResponse> firstUserGetCustomerAccountsResponseList = new UserSteps(
                createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()).getAccounts();

        GetCustomerAccountsResponse firstUserGetCustomerAccountsResponse = firstUserGetCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse ->
                        getCustomerAccountResponse.getId() == createFirstUserAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        //проверка счета 2 пользователя
        List<GetCustomerAccountsResponse> secondUserGetCustomerAccountsResponseList = new UserSteps(
                createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword()).getAccounts();

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
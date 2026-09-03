package iteration2_middle;

import generators.RandomData;
import io.restassured.common.mapper.TypeRef;
import models.*;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import requests.*;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.AssertionsForClassTypes.within;
import static org.hamcrest.MatcherAssert.assertThat;

public class TransferMoneyTest extends BaseTest {
    @Test
    public void userCanTransferValidAmountIntoSomeonesAccount() {
        CreateUserRequest createFirstUserRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        //создать 1 юзера
        new AdminCreateUserRequester(RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(createFirstUserRequest);

        //создать аккаунт 1 юзера
        CreateAccountResponse createFirstUserAccountResponse = new CreateAccountRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(CreateAccountResponse.class);

        CreateUserRequest createSecondUserRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        //создать 2 юзера
        new AdminCreateUserRequester(RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(createSecondUserRequest);

        //создать аккаунт 2 юзера
        CreateAccountResponse createSecondUserAccountResponse = new CreateAccountRequester(
                RequestSpecs.authAsUser(createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(CreateAccountResponse.class);

        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
                .id(createFirstUserAccountResponse.getId())
                .balance(MAX_DEPOSIT_AMOUNT)
                .build();

        //депозит
        new DepositMoneyRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .post(depositMoneyRequest);

        float randomTransferAmount = RandomData.getRandomPositiveFloat();
        //трансфер
        TransferMoneyRequest transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(createFirstUserAccountResponse.getId())
                .receiverAccountId(createSecondUserAccountResponse.getId())
                .amount(randomTransferAmount)
                .build();

        new TransferMoneyRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .post(transferMoneyRequest)
                .body("message", Matchers.equalTo(AlertMessage.TRANSFER_SUCCESSFUL.getMessage()))
                .body("amount", Matchers.equalTo(randomTransferAmount))
                .body("receiverAccountId", Matchers.equalTo((int) createSecondUserAccountResponse.getId()))
                .body("senderAccountId", Matchers.equalTo((int) createFirstUserAccountResponse.getId()));

        //проверка счета 1 пользователя
        List<AccountResponse> firstUserAccountResponseList = new GetCustomerAccountsRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(new TypeRef<List<AccountResponse>>() {});

        AccountResponse firstUserAccountResponse = firstUserAccountResponseList.stream()
                .filter(accountResponse -> accountResponse.getId() == createFirstUserAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(firstUserAccountResponse.getBalance()).isCloseTo(MAX_DEPOSIT_AMOUNT - randomTransferAmount, within(0.001f));

        //проверка счета 2 пользователя
        List<AccountResponse> secondUserAccountResponseList = new GetCustomerAccountsRequester(
                RequestSpecs.authAsUser(createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(new TypeRef<List<AccountResponse>>() {});

        AccountResponse secondUserAccountResponse = secondUserAccountResponseList.stream()
                .filter(accountResponse -> accountResponse.getId() == createSecondUserAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        assertThat(secondUserAccountResponse.getBalance() ,Matchers.equalTo(randomTransferAmount));

        //проверка транзакции
        List<TransactionResponse> firstUserTransactionResponseList = new GetAccountTransactionsRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK(),
                createFirstUserAccountResponse.getId())
                .get()
                .extract()
                .as(new TypeRef<List<TransactionResponse>>() {});

        TransactionResponse firstUserTransactionResponse = firstUserTransactionResponseList.stream()
                .filter(transactionResponse -> transactionResponse.getType() == TransactionType.TRANSFER_OUT)
                .findFirst()
                .orElseThrow();

        List<TransactionResponse> secondUserTransactionResponseList = new GetAccountTransactionsRequester(
                RequestSpecs.authAsUser(createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK(),
                createSecondUserAccountResponse.getId())
                .get()
                .extract()
                .as(new TypeRef<List<TransactionResponse>>() {});

        TransactionResponse secondUserTransactionResponse = secondUserTransactionResponseList.stream()
                .filter(transactionResponse -> transactionResponse.getType() == TransactionType.TRANSFER_IN)
                .findFirst()
                .orElseThrow();

        softly.assertThat(firstUserTransactionResponse.getId()).isNotNull();
        softly.assertThat(firstUserTransactionResponse.getAmount()).isEqualTo(randomTransferAmount);
        softly.assertThat(firstUserTransactionResponse.getRelatedAccountId()).isEqualTo(createSecondUserAccountResponse.getId());

        softly.assertThat(secondUserTransactionResponse.getId()).isNotNull();
        softly.assertThat(secondUserTransactionResponse.getAmount()).isEqualTo(randomTransferAmount);
        softly.assertThat(secondUserTransactionResponse.getRelatedAccountId()).isEqualTo(createFirstUserAccountResponse.getId());
    }

    @ValueSource(floats =
            //positive cases
            {0.01f, 9999.99f})
    @ParameterizedTest
    public void checkBoundaryValuesTransferBetweenOwnAccountsPositiveCases(float amount) {
        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        //создать юзера
        new AdminCreateUserRequester(RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(createUserRequest);

        //создать 1 аккаунт
        CreateAccountResponse createFirstAccountResponse = new CreateAccountRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(CreateAccountResponse.class);

        //создать 2 аккаунт
        CreateAccountResponse createSecondAccountResponse = new CreateAccountRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(CreateAccountResponse.class);

        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
                .id(createFirstAccountResponse.getId())
                .balance(MAX_DEPOSIT_AMOUNT)
                .build();

        //депозит денег
        for (int i = 0; i < 2; i++) {
            new DepositMoneyRequester(
                    RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                    ResponseSpecs.requestReturnsOK())
                    .post(depositMoneyRequest);
        }

        //трансфер
        TransferMoneyRequest transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(createFirstAccountResponse.getId())
                .receiverAccountId(createSecondAccountResponse.getId())
                .amount(amount)
                .build();

        new TransferMoneyRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .post(transferMoneyRequest)
                .body("message", Matchers.equalTo("Transfer successful"))
                .body("amount", Matchers.equalTo(amount))
                .body("receiverAccountId", Matchers.equalTo((int) createSecondAccountResponse.getId()))
                .body("senderAccountId", Matchers.equalTo((int) createFirstAccountResponse.getId()));

        //проверка 1 и 2 счета
        List<AccountResponse> accountResponseList = new GetCustomerAccountsRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(new TypeRef<List<AccountResponse>>() {});

        AccountResponse firstAccountResponse = accountResponseList.stream()
                .filter(accountResponse -> accountResponse.getId() == createFirstAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        AccountResponse secondAccountResponse = accountResponseList.stream()
                .filter(accountResponse -> accountResponse.getId() == createSecondAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(firstAccountResponse.getBalance()).isCloseTo(MAX_DEPOSIT_AMOUNT * 2 - amount, within(0.001f));
        softly.assertThat(firstAccountResponse.getTransactions()).isNotEmpty();

        softly.assertThat(secondAccountResponse.getBalance()).isEqualTo(amount);
        softly.assertThat(secondAccountResponse.getTransactions()).isNotEmpty();
    }

    @Test
    public void userCannotTransferNegativeAmountIntoOwnAccount() {
        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        //создать юзера
        new AdminCreateUserRequester(RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(createUserRequest);

        //создать 1 аккаунт
        CreateAccountResponse createFirstAccountResponse = new CreateAccountRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(CreateAccountResponse.class);

        //создать 2 аккаунт
        CreateAccountResponse createSecondAccountResponse = new CreateAccountRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(CreateAccountResponse.class);

        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
                .id(createFirstAccountResponse.getId())
                .balance(MAX_DEPOSIT_AMOUNT)
                .build();

        //депозит денег
        new DepositMoneyRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .post(depositMoneyRequest);

        //трансфер
        TransferMoneyRequest transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(createFirstAccountResponse.getId())
                .receiverAccountId(createSecondAccountResponse.getId())
                .amount(RandomData.getRandomNegativeFloat())
                .build();

        new TransferMoneyRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequestWithText(AlertMessage.TRANSFER_AMOUNT_MUST_BE_AT_LEAST_001.getMessage()))
                .post(transferMoneyRequest);

        //проверка 1 и 2 счета
        List<AccountResponse> accountResponseList = new GetCustomerAccountsRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(new TypeRef<List<AccountResponse>>() {});

        AccountResponse firstAccountResponse = accountResponseList.stream()
                .filter(accountResponse -> accountResponse.getId() == createFirstAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        AccountResponse secondAccountResponse = accountResponseList.stream()
                .filter(accountResponse -> accountResponse.getId() == createSecondAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(firstAccountResponse.getBalance()).isEqualTo(MAX_DEPOSIT_AMOUNT);

        softly.assertThat(secondAccountResponse.getBalance()).isEqualTo(0.0f);
        softly.assertThat(secondAccountResponse.getTransactions()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(floats = {10000.01f})
    public void checkBoundaryValuesTransferToSomeonesAccountNegativeCases(float amount) {
        CreateUserRequest createFirstUserRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        //создать 1 юзера
        new AdminCreateUserRequester(RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(createFirstUserRequest);

        //создать аккаунт 1 юзера
        CreateAccountResponse createFirstUserAccountResponse = new CreateAccountRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(CreateAccountResponse.class);

        CreateUserRequest createSecondUserRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        //создать 2 юзера
        new AdminCreateUserRequester(RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(createSecondUserRequest);

        //создать аккаунт 2 юзера
        CreateAccountResponse createSecondUserAccountResponse = new CreateAccountRequester(
                RequestSpecs.authAsUser(createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(CreateAccountResponse.class);

        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
                .id(createFirstUserAccountResponse.getId())
                .balance(MAX_DEPOSIT_AMOUNT)
                .build();

        //депозит
        for (int i = 0; i < 3; i++) {
            new DepositMoneyRequester(
                    RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                    ResponseSpecs.requestReturnsOK())
                    .post(depositMoneyRequest);
        }

        //трансфер
        TransferMoneyRequest transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(createFirstUserAccountResponse.getId())
                .receiverAccountId(createSecondUserAccountResponse.getId())
                .amount(amount)
                .build();

        new TransferMoneyRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequestWithText(AlertMessage.TRANSFER_AMOUNT_CANNOT_EXCEED_10000.getMessage()))
                .post(transferMoneyRequest);

        //проверка счета 1 пользователя
        List<AccountResponse> firstUserAccountResponseList = new GetCustomerAccountsRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(new TypeRef<List<AccountResponse>>() {});

        AccountResponse firstUserAccountResponse = firstUserAccountResponseList.stream()
                .filter(accountResponse -> accountResponse.getId() == createFirstUserAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        //проверка счета 2 пользователя
        List<AccountResponse> secondUserAccountResponseList = new GetCustomerAccountsRequester(
                RequestSpecs.authAsUser(createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(new TypeRef<List<AccountResponse>>() {});

        AccountResponse secondUserAccountResponse = secondUserAccountResponseList.stream()
                .filter(accountResponse -> accountResponse.getId() == createSecondUserAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(firstUserAccountResponse.getBalance()).isEqualTo(MAX_DEPOSIT_AMOUNT * 3);

        softly.assertThat(secondUserAccountResponse.getBalance()).isEqualTo(0.0f);
        softly.assertThat(secondUserAccountResponse.getTransactions()).isEmpty();
    }

    @Test
    public void userCannotTransferAmountBiggerThanAvailableIntoSomeonesAccount() {
        CreateUserRequest createFirstUserRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        //создать 1 юзера
        new AdminCreateUserRequester(RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(createFirstUserRequest);

        //создать аккаунт 1 юзера
        CreateAccountResponse createFirstUserAccountResponse = new CreateAccountRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(CreateAccountResponse.class);

        CreateUserRequest createSecondUserRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        //создать 2 юзера
        new AdminCreateUserRequester(RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(createSecondUserRequest);

        //создать аккаунт 2 юзера
        CreateAccountResponse createSecondUserAccountResponse = new CreateAccountRequester(
                RequestSpecs.authAsUser(createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(CreateAccountResponse.class);

        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
                .id(createFirstUserAccountResponse.getId())
                .balance(MAX_DEPOSIT_AMOUNT)
                .build();

        //депозит
            new DepositMoneyRequester(
                    RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                    ResponseSpecs.requestReturnsOK())
                    .post(depositMoneyRequest);

        //трансфер
        TransferMoneyRequest transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(createFirstUserAccountResponse.getId())
                .receiverAccountId(createSecondUserAccountResponse.getId())
                .amount(MAX_DEPOSIT_AMOUNT + RandomData.getRandomPositiveFloat())
                .build();

        new TransferMoneyRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequestWithText(AlertMessage.INVALID_TRANSFER.getMessage()))
                .post(transferMoneyRequest);

        //проверка счета 1 пользователя
        List<AccountResponse> firstUserAccountResponseList = new GetCustomerAccountsRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(new TypeRef<List<AccountResponse>>() {});

        AccountResponse firstUserAccountResponse = firstUserAccountResponseList.stream()
                .filter(accountResponse -> accountResponse.getId() == createFirstUserAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        //проверка счета 2 пользователя
        List<AccountResponse> secondUserAccountResponseList = new GetCustomerAccountsRequester(
                RequestSpecs.authAsUser(createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(new TypeRef<List<AccountResponse>>() {});

        AccountResponse secondUserAccountResponse = secondUserAccountResponseList.stream()
                .filter(accountResponse -> accountResponse.getId() == createSecondUserAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(firstUserAccountResponse.getBalance()).isEqualTo(MAX_DEPOSIT_AMOUNT);

        softly.assertThat(secondUserAccountResponse.getBalance()).isEqualTo(0.0f);
        softly.assertThat(secondUserAccountResponse.getTransactions()).isEmpty();
    }
}

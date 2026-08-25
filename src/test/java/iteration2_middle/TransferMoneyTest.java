package iteration2_middle;

import generators.RandomData;
import io.restassured.common.mapper.TypeRef;
import models.*;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import requests.*;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.within;
import static org.hamcrest.MatcherAssert.assertThat;

public class TransferMoneyTest extends BaseTest {
    private final float MAX_DEPOSIT_AMOUNT = 5000.0f;

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
                .post(null)
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
                .post(null)
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
                .amount(50.05f)
                .build();

        new TransferMoneyRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .post(transferMoneyRequest)
                .body("message", Matchers.equalTo("Transfer successful"))
                .body("amount", Matchers.equalTo(50.05f))
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

        assertThat(firstUserAccountResponse.getBalance() ,Matchers.equalTo(MAX_DEPOSIT_AMOUNT - 50.05f));

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

        assertThat(secondUserAccountResponse.getBalance() ,Matchers.equalTo(50.05f));

        //проверка транзакции
        List<TransactionResponse> transactionResponseList = new GetAccountTransactionsRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK(),
                createFirstUserAccountResponse.getId())
                .get()
                .extract()
                .as(new TypeRef<List<TransactionResponse>>() {});

        TransactionResponse firstUserTransactionResponse = transactionResponseList.stream()
                .filter(transactionResponse -> transactionResponse.getType() == TransactionType.TRANSFER_OUT)
                .findFirst()
                .orElseThrow();

        softly.assertThat(firstUserTransactionResponse.getId()).isNotNull();
        softly.assertThat(firstUserTransactionResponse.getAmount()).isEqualTo(50.05f);
        softly.assertThat(firstUserTransactionResponse.getRelatedAccountId()).isEqualTo(createSecondUserAccountResponse.getId());
    }

    @CsvSource(value =
            //positive cases
            {"0.01,", "9999.99"})
    @ParameterizedTest
    public void userCanTransferAmountIntoOwnAccount(float amount) {
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
                .post(null)
                .extract()
                .as(CreateAccountResponse.class);

        //создать 2 аккаунт
        CreateAccountResponse createSecondAccountResponse = new CreateAccountRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .as(CreateAccountResponse.class);

        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
                .id(createFirstAccountResponse.getId())
                .balance(5000f)
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
                .post(null)
                .extract()
                .as(CreateAccountResponse.class);

        //создать 2 аккаунт
        CreateAccountResponse createSecondAccountResponse = new CreateAccountRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post(null)
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
                .amount(-1f)
                .build();

        new TransferMoneyRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequestWithText("Transfer amount must be at least 0.01"))
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

    @Test
    public void userCannotTransferInvalidAmountIntoSomeonesAccount() {
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
                .post(null)
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
                .post(null)
                .extract()
                .as(CreateAccountResponse.class);

        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
                .id(createFirstUserAccountResponse.getId())
                .balance(5000f)
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
                .amount(10000.01f)
                .build();

        new TransferMoneyRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequestWithText("Transfer amount cannot exceed 10000"))
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
        softly.assertThat(secondUserAccountResponse.getTransactions().isEmpty());
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
                .post(null)
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
                .post(null)
                .extract()
                .as(CreateAccountResponse.class);

        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
                .id(createFirstUserAccountResponse.getId())
                .balance(100f)
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
                .amount(100.5f)
                .build();

        new TransferMoneyRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequestWithText("Invalid transfer: insufficient funds or invalid accounts"))
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

        softly.assertThat(firstUserAccountResponse.getBalance()).isEqualTo(100f);

        softly.assertThat(secondUserAccountResponse.getBalance()).isEqualTo(0.0f);
        softly.assertThat(secondUserAccountResponse.getTransactions().isEmpty());
    }
}

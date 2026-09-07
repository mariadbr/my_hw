package iteration2_middle;

import generators.RandomData;
import io.restassured.common.mapper.TypeRef;
import models.*;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import requests.*;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.ValidatedCrudRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.List;

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
        List<GetCustomerAccountsResponse> firstUserGetCustomerAccountsResponseList = new GetCustomerAccountsRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(new TypeRef<List<GetCustomerAccountsResponse>>() {});

        GetCustomerAccountsResponse firstUserGetCustomerAccountsResponse = firstUserGetCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse -> getCustomerAccountResponse.getId() == createFirstUserAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(firstUserGetCustomerAccountsResponse.getBalance()).isCloseTo(MAX_DEPOSIT_AMOUNT - randomTransferAmount, within(0.001f));

        //проверка счета 2 пользователя
        List<GetCustomerAccountsResponse> secondUserGetCustomerAccountsResponseList = new GetCustomerAccountsRequester(
                RequestSpecs.authAsUser(createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(new TypeRef<List<GetCustomerAccountsResponse>>() {});

        GetCustomerAccountsResponse secondUserGetCustomerAccountsResponse = secondUserGetCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse -> getCustomerAccountResponse.getId() == createSecondUserAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        assertThat(secondUserGetCustomerAccountsResponse.getBalance() ,Matchers.equalTo(randomTransferAmount));

        //проверка транзакции
        List<GetTransactionResponse> firstUserGetTransactionResponseList =  new ValidatedCrudRequester<GetTransactionResponse>(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                Endpoint.ACCOUNT_TRANSACTIONS,
                ResponseSpecs.requestReturnsOK())
                .getListById(createFirstUserAccountResponse.getId(), new TypeRef<List<GetTransactionResponse>>() {});

//        List<GetTransactionResponse> firstUserGetTransactionResponseList = new GetAccountTransactionsRequester(
//                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
//                ResponseSpecs.requestReturnsOK(),
//                createFirstUserAccountResponse.getId())
//                .get()
//                .extract()
//                .as(new TypeRef<List<GetTransactionResponse>>() {});

        GetTransactionResponse firstUserGetTransactionResponse = firstUserGetTransactionResponseList.stream()
                .filter(transactionResponse -> transactionResponse.getType() == TransactionType.TRANSFER_OUT)
                .findFirst()
                .orElseThrow();

        List<GetTransactionResponse> secondUserGetTransactionResponseList = new GetAccountTransactionsRequester(
                RequestSpecs.authAsUser(createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK(),
                createSecondUserAccountResponse.getId())
                .get()
                .extract()
                .as(new TypeRef<List<GetTransactionResponse>>() {});

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
        List<GetCustomerAccountsResponse> getCustomerAccountsResponseList = new GetCustomerAccountsRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(new TypeRef<List<GetCustomerAccountsResponse>>() {});

        GetCustomerAccountsResponse firstGetCustomerAccountsResponse = getCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse -> getCustomerAccountResponse.getId() == createFirstAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        GetCustomerAccountsResponse secondGetCustomerAccountsResponse = getCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse -> getCustomerAccountResponse.getId() == createSecondAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(firstGetCustomerAccountsResponse.getBalance()).isCloseTo(MAX_DEPOSIT_AMOUNT * 2 - amount, within(0.001f));
        softly.assertThat(firstGetCustomerAccountsResponse.getTransactions()).isNotEmpty();

        softly.assertThat(secondGetCustomerAccountsResponse.getBalance()).isEqualTo(amount);
        softly.assertThat(secondGetCustomerAccountsResponse.getTransactions()).isNotEmpty();
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
        List<GetCustomerAccountsResponse> getCustomerAccountsResponseList = new GetCustomerAccountsRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(new TypeRef<List<GetCustomerAccountsResponse>>() {});

        GetCustomerAccountsResponse firstGetCustomerAccountsResponse = getCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse -> getCustomerAccountResponse.getId() == createFirstAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        GetCustomerAccountsResponse secondGetCustomerAccountsResponse = getCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse -> getCustomerAccountResponse.getId() == createSecondAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(firstGetCustomerAccountsResponse.getBalance()).isEqualTo(MAX_DEPOSIT_AMOUNT);

        softly.assertThat(secondGetCustomerAccountsResponse.getBalance()).isEqualTo(0.0f);
        softly.assertThat(secondGetCustomerAccountsResponse.getTransactions()).isEmpty();
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
        List<GetCustomerAccountsResponse> firstUserGetCustomerAccountsResponseList = new GetCustomerAccountsRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(new TypeRef<List<GetCustomerAccountsResponse>>() {});

        GetCustomerAccountsResponse firstUserGetCustomerAccountsResponse = firstUserGetCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse -> getCustomerAccountResponse.getId() == createFirstUserAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        //проверка счета 2 пользователя
        List<GetCustomerAccountsResponse> secondUserGetCustomerAccountsResponseList = new GetCustomerAccountsRequester(
                RequestSpecs.authAsUser(createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(new TypeRef<List<GetCustomerAccountsResponse>>() {});

        GetCustomerAccountsResponse secondUserGetCustomerAccountsResponse = secondUserGetCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse -> getCustomerAccountResponse.getId() == createSecondUserAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(firstUserGetCustomerAccountsResponse.getBalance()).isEqualTo(MAX_DEPOSIT_AMOUNT * 3);

        softly.assertThat(secondUserGetCustomerAccountsResponse.getBalance()).isEqualTo(0.0f);
        softly.assertThat(secondUserGetCustomerAccountsResponse.getTransactions()).isEmpty();
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
        List<GetCustomerAccountsResponse> firstUserGetCustomerAccountsResponseList = new GetCustomerAccountsRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(new TypeRef<List<GetCustomerAccountsResponse>>() {});

        GetCustomerAccountsResponse firstUserGetCustomerAccountsResponse = firstUserGetCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse -> getCustomerAccountResponse.getId() == createFirstUserAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        //проверка счета 2 пользователя
        List<GetCustomerAccountsResponse> secondUserGetCustomerAccountsResponseList = new GetCustomerAccountsRequester(
                RequestSpecs.authAsUser(createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(new TypeRef<List<GetCustomerAccountsResponse>>() {});

        GetCustomerAccountsResponse secondUserGetCustomerAccountsResponse = secondUserGetCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse -> getCustomerAccountResponse.getId() == createSecondUserAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(firstUserGetCustomerAccountsResponse.getBalance()).isEqualTo(MAX_DEPOSIT_AMOUNT);

        softly.assertThat(secondUserGetCustomerAccountsResponse.getBalance()).isEqualTo(0.0f);
        softly.assertThat(secondUserGetCustomerAccountsResponse.getTransactions()).isEmpty();
    }
}

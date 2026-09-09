package iteration2_middle;

import generators.RandomData;
import generators.RandomModelGenerator;
import models.*;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.skelethon.requesters.ValidatedCrudRequester;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.List;

import static models.UserBalanceDefaults.MAX_DEPOSIT_AMOUNT;
import static org.assertj.core.api.AssertionsForClassTypes.within;
import static org.hamcrest.MatcherAssert.assertThat;

public class TransferMoneyTest extends BaseTest {
    @Test
    public void userCanTransferValidAmountIntoSomeonesAccount() {
//        CreateUserRequest createFirstUserRequest = RandomModelGenerator.generate(CreateUserRequest.class);

        //создать 1 юзера
        CreateUserRequest createFirstUserRequest = AdminSteps.createUserGetRequest();
//        new CrudRequester(RequestSpecs.adminSpec(),
//                Endpoint.ADMIN_USERS,
//                ResponseSpecs.entityWasCreated())
//                .post(createFirstUserRequest);

        //создать аккаунт 1 юзера
        CreateAccountResponse createFirstUserAccountResponse = UserSteps.createAccount(
                createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword());
//        CreateAccountResponse createFirstUserAccountResponse = new ValidatedCrudRequester<CreateAccountResponse>(
//                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
//                Endpoint.ACCOUNTS,
//                ResponseSpecs.entityWasCreated())
//                .post();

//        CreateUserRequest createSecondUserRequest = RandomModelGenerator.generate(CreateUserRequest.class);

        //создать 2 юзера
        CreateUserRequest createSecondUserRequest = AdminSteps.createUserGetRequest();
//        new CrudRequester(RequestSpecs.adminSpec(),
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

//        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
//                .id(createFirstUserAccountResponse.getId())
//                .balance(MAX_DEPOSIT_AMOUNT.getAmount())
//                .build();

        //депозит
        UserSteps.depositMaxDepositAmount(createFirstUserAccountResponse.getId(), createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword());
//        new CrudRequester(
//                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
//                Endpoint.ACCOUNTS_DEPOSIT,
//                ResponseSpecs.requestReturnsOK())
//                .post(depositMoneyRequest);

        float randomTransferAmount = RandomData.getRandomPositiveFloat();

        //трансфер
        TransferMoneyRequest transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(createFirstUserAccountResponse.getId())
                .receiverAccountId(createSecondUserAccountResponse.getId())
                .amount(randomTransferAmount)
                .build();

        TransferMoneyResponse transferMoneyResponse = new ValidatedCrudRequester<TransferMoneyResponse>(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.requestReturnsOK())
                .post(transferMoneyRequest);

        softly.assertThat(transferMoneyResponse.getMessage()).isEqualTo(AlertMessage.TRANSFER_SUCCESSFUL.getMessage());
        softly.assertThat(transferMoneyResponse.getAmount()).isEqualTo(randomTransferAmount);
        softly.assertThat(transferMoneyResponse.getReceiverAccountId()).isEqualTo(createSecondUserAccountResponse.getId());
        softly.assertThat(transferMoneyResponse.getSenderAccountId()).isEqualTo(createFirstUserAccountResponse.getId());

        //проверка счета 1 пользователя
        List<GetCustomerAccountsResponse> firstUserGetCustomerAccountsResponseList = UserSteps.getAccounts(
                createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword());
//        List<GetCustomerAccountsResponse> firstUserGetCustomerAccountsResponseList = new ValidatedCrudRequester<GetCustomerAccountsResponse>(
//                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
//                Endpoint.CUSTOMER_ACCOUNTS,
//                ResponseSpecs.requestReturnsOK())
//                .getList(new TypeRef<List<GetCustomerAccountsResponse>>() {});

        GetCustomerAccountsResponse firstUserGetCustomerAccountsResponse = firstUserGetCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse -> getCustomerAccountResponse.getId() == createFirstUserAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(firstUserGetCustomerAccountsResponse.getBalance()).isCloseTo(MAX_DEPOSIT_AMOUNT.getAmount() - randomTransferAmount, within(0.001f));

        //проверка счета 2 пользователя
        List<GetCustomerAccountsResponse> secondUserGetCustomerAccountsResponseList = UserSteps.getAccounts(
                createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword());
//        List<GetCustomerAccountsResponse> secondUserGetCustomerAccountsResponseList = new ValidatedCrudRequester<GetCustomerAccountsResponse>(
//                RequestSpecs.authAsUser(createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword()),
//                Endpoint.CUSTOMER_ACCOUNTS,
//                ResponseSpecs.requestReturnsOK())
//                .getList(new TypeRef<List<GetCustomerAccountsResponse>>() {});

        GetCustomerAccountsResponse secondUserGetCustomerAccountsResponse = secondUserGetCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse -> getCustomerAccountResponse.getId() == createSecondUserAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        assertThat(secondUserGetCustomerAccountsResponse.getBalance() ,Matchers.equalTo(randomTransferAmount));

        //проверка транзакции
        List<GetTransactionResponse> firstUserGetTransactionResponseList = UserSteps.getTransactions(
                createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword(), createFirstUserAccountResponse.getId());
//        List<GetTransactionResponse> firstUserGetTransactionResponseList =  new ValidatedCrudRequester<GetTransactionResponse>(
//                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
//                Endpoint.ACCOUNT_TRANSACTIONS,
//                ResponseSpecs.requestReturnsOK())
//                .getListById(createFirstUserAccountResponse.getId(), new TypeRef<List<GetTransactionResponse>>() {});

        GetTransactionResponse firstUserGetTransactionResponse = firstUserGetTransactionResponseList.stream()
                .filter(transactionResponse -> transactionResponse.getType() == TransactionType.TRANSFER_OUT)
                .findFirst()
                .orElseThrow();


        List<GetTransactionResponse> secondUserGetTransactionResponseList = UserSteps.getTransactions(
                createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword(), createSecondUserAccountResponse.getId());
//        List<GetTransactionResponse> secondUserGetTransactionResponseList =  new ValidatedCrudRequester<GetTransactionResponse>(
//                RequestSpecs.authAsUser(createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword()),
//                Endpoint.ACCOUNT_TRANSACTIONS,
//                ResponseSpecs.requestReturnsOK())
//                .getListById(createSecondUserAccountResponse.getId(), new TypeRef<List<GetTransactionResponse>>() {});

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

        CreateUserRequest createUserRequest = RandomModelGenerator.generate(CreateUserRequest.class);

        //создать юзера
        new CrudRequester(RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USERS,
                ResponseSpecs.entityWasCreated())
                .post(createUserRequest);

        //создать 1 аккаунт
        CreateAccountResponse createFirstAccountResponse = new ValidatedCrudRequester<CreateAccountResponse>(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated())
                .post();

        //создать 2 аккаунт
        CreateAccountResponse createSecondAccountResponse = new ValidatedCrudRequester<CreateAccountResponse>(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated())
                .post();

//        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
//                .id(createFirstAccountResponse.getId())
//                .balance(MAX_DEPOSIT_AMOUNT.getAmount())
//                .build();

        //депозит денег
        for (int i = 0; i < 2; i++) {
            UserSteps.depositMaxDepositAmount(createFirstAccountResponse.getId(), createUserRequest.getUsername(), createUserRequest.getPassword());
//            new CrudRequester(
//                    RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
//                    Endpoint.ACCOUNTS_DEPOSIT,
//                    ResponseSpecs.requestReturnsOK())
//                    .post(depositMoneyRequest);
        }

        //трансфер
        TransferMoneyRequest transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(createFirstAccountResponse.getId())
                .receiverAccountId(createSecondAccountResponse.getId())
                .amount(amount)
                .build();

        //нужно ли сделать этот трансфер как отдельный степ? ведь конкретно в этом случае все данные типичные и не нужно передавать никаких специальных спецификаций, значений и тд
        TransferMoneyResponse transferMoneyResponse = new ValidatedCrudRequester<TransferMoneyResponse>(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.requestReturnsOK())
                .post(transferMoneyRequest);

        softly.assertThat(transferMoneyResponse.getMessage()).isEqualTo(AlertMessage.TRANSFER_SUCCESSFUL.getMessage());
        softly.assertThat(transferMoneyResponse.getAmount()).isEqualTo(amount);
        softly.assertThat(transferMoneyResponse.getReceiverAccountId()).isEqualTo(createSecondAccountResponse.getId());
        softly.assertThat(transferMoneyResponse.getSenderAccountId()).isEqualTo(createFirstAccountResponse.getId());

        //проверка 1 и 2 счета
        List<GetCustomerAccountsResponse> getCustomerAccountsResponseList = UserSteps.getAccounts(
                createUserRequest.getUsername(), createUserRequest.getPassword());
//        List<GetCustomerAccountsResponse> getCustomerAccountsResponseList = new ValidatedCrudRequester<GetCustomerAccountsResponse>(
//                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
//                Endpoint.CUSTOMER_ACCOUNTS,
//                ResponseSpecs.requestReturnsOK())
//                .getList(new TypeRef<List<GetCustomerAccountsResponse>>() {});

        GetCustomerAccountsResponse firstGetCustomerAccountsResponse = getCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse -> getCustomerAccountResponse.getId() == createFirstAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        GetCustomerAccountsResponse secondGetCustomerAccountsResponse = getCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse -> getCustomerAccountResponse.getId() == createSecondAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(firstGetCustomerAccountsResponse.getBalance()).isCloseTo(MAX_DEPOSIT_AMOUNT.getAmount() * 2 - amount, within(0.001f));
        softly.assertThat(firstGetCustomerAccountsResponse.getTransactions()).isNotEmpty();

        softly.assertThat(secondGetCustomerAccountsResponse.getBalance()).isEqualTo(amount);
        softly.assertThat(secondGetCustomerAccountsResponse.getTransactions()).isNotEmpty();
    }

    @Test
    public void userCannotTransferNegativeAmountIntoOwnAccount() {
//        CreateUserRequest createUserRequest = RandomModelGenerator.generate(CreateUserRequest.class);

        //создать юзера
        CreateUserRequest createUserRequest = AdminSteps.createUserGetRequest();
//        new CrudRequester(RequestSpecs.adminSpec(),
//                Endpoint.ADMIN_USERS,
//                ResponseSpecs.entityWasCreated())
//                .post(createUserRequest);

        //создать 1 аккаунт
        CreateAccountResponse createFirstAccountResponse = UserSteps.createAccount(createUserRequest.getUsername(), createUserRequest.getPassword());
//        CreateAccountResponse createFirstAccountResponse = new ValidatedCrudRequester<CreateAccountResponse>(
//                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
//                Endpoint.ACCOUNTS,
//                ResponseSpecs.entityWasCreated())
//                .post();

        //создать 2 аккаунт
        CreateAccountResponse createSecondAccountResponse = UserSteps.createAccount(createUserRequest.getUsername(), createUserRequest.getPassword());
//        CreateAccountResponse createSecondAccountResponse = new ValidatedCrudRequester<CreateAccountResponse>(
//                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
//                Endpoint.ACCOUNTS,
//                ResponseSpecs.entityWasCreated())
//                .post();

//        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
//                .id(createFirstAccountResponse.getId())
//                .balance(MAX_DEPOSIT_AMOUNT.getAmount())
//                .build();

        //депозит денег
        UserSteps.depositMaxDepositAmount(createFirstAccountResponse.getId(), createUserRequest.getUsername(), createUserRequest.getPassword());
//        new CrudRequester(
//                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
//                Endpoint.ACCOUNTS_DEPOSIT,
//                ResponseSpecs.requestReturnsOK())
//                .post(depositMoneyRequest);

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
//        List<GetCustomerAccountsResponse> getCustomerAccountsResponseList = new ValidatedCrudRequester<GetCustomerAccountsResponse>(
//                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
//                Endpoint.CUSTOMER_ACCOUNTS,
//                ResponseSpecs.requestReturnsOK())
//                .getList(new TypeRef<List<GetCustomerAccountsResponse>>() {});

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

        softly.assertThat(secondGetCustomerAccountsResponse.getBalance()).isEqualTo(0.0f);
        softly.assertThat(secondGetCustomerAccountsResponse.getTransactions()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(floats = {10000.01f})
    public void checkBoundaryValuesTransferToSomeonesAccountNegativeCases(float amount) {
//        CreateUserRequest createFirstUserRequest = RandomModelGenerator.generate(CreateUserRequest.class);

        //создать 1 юзера
        CreateUserRequest createFirstUserRequest = AdminSteps.createUserGetRequest();
//        new CrudRequester(
//                RequestSpecs.adminSpec(),
//                Endpoint.ADMIN_USERS,
//                ResponseSpecs.entityWasCreated())
//                .post(createFirstUserRequest);

        //создать аккаунт 1 юзера
        CreateAccountResponse createFirstUserAccountResponse = UserSteps.createAccount(
                createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword());
//        CreateAccountResponse createFirstUserAccountResponse = new ValidatedCrudRequester<CreateAccountResponse>(
//                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
//                Endpoint.ACCOUNTS,
//                ResponseSpecs.entityWasCreated())
//                .post();

//        CreateUserRequest createSecondUserRequest = CreateUserRequest.builder()
//                .username(RandomData.getUsername())
//                .password(RandomData.getPassword())
//                .role(UserRole.USER.toString())
//                .build();

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

//        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
//                .id(createFirstUserAccountResponse.getId())
//                .balance(MAX_DEPOSIT_AMOUNT.getAmount())
//                .build();

        //депозит
        for (int i = 0; i < 3; i++) {
            UserSteps.depositMaxDepositAmount(
                    createFirstUserAccountResponse.getId(), createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword());
//            new CrudRequester(
//                    RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
//                    Endpoint.ACCOUNTS_DEPOSIT,
//                    ResponseSpecs.requestReturnsOK())
//                    .post(depositMoneyRequest);
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
//        List<GetCustomerAccountsResponse> firstUserGetCustomerAccountsResponseList = new ValidatedCrudRequester<GetCustomerAccountsResponse>(
//                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
//                Endpoint.CUSTOMER_ACCOUNTS,
//                ResponseSpecs.requestReturnsOK())
//                .getList(new TypeRef<List<GetCustomerAccountsResponse>>() {});


        GetCustomerAccountsResponse firstUserGetCustomerAccountsResponse = firstUserGetCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse -> getCustomerAccountResponse.getId() == createFirstUserAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        //проверка счета 2 пользователя
        List<GetCustomerAccountsResponse> secondUserGetCustomerAccountsResponseList = UserSteps.getAccounts(createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword());
//        List<GetCustomerAccountsResponse> secondUserGetCustomerAccountsResponseList = new ValidatedCrudRequester<GetCustomerAccountsResponse>(
//                RequestSpecs.authAsUser(createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword()),
//                Endpoint.CUSTOMER_ACCOUNTS,
//                ResponseSpecs.requestReturnsOK())
//                .getList(new TypeRef<List<GetCustomerAccountsResponse>>() {});

        GetCustomerAccountsResponse secondUserGetCustomerAccountsResponse = secondUserGetCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse -> getCustomerAccountResponse.getId() == createSecondUserAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(firstUserGetCustomerAccountsResponse.getBalance()).isEqualTo(MAX_DEPOSIT_AMOUNT.getAmount() * 3);

        softly.assertThat(secondUserGetCustomerAccountsResponse.getBalance()).isEqualTo(UserBalanceDefaults.INITIAL_BALANCE.getAmount());
        softly.assertThat(secondUserGetCustomerAccountsResponse.getTransactions()).isEmpty();
    }

    @Test
    public void userCannotTransferAmountBiggerThanAvailableIntoSomeonesAccount() {
//        CreateUserRequest createFirstUserRequest = RandomModelGenerator.generate(CreateUserRequest.class);

        //создать 1 юзера
        CreateUserRequest createFirstUserRequest = AdminSteps.createUserGetRequest();
//        new CrudRequester(RequestSpecs.adminSpec(),
//                Endpoint.ADMIN_USERS,
//                ResponseSpecs.entityWasCreated())
//                .post(createFirstUserRequest);

        //создать аккаунт 1 юзера
        CreateAccountResponse createFirstUserAccountResponse = UserSteps.createAccount(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword());
//        CreateAccountResponse createFirstUserAccountResponse = new ValidatedCrudRequester<CreateAccountResponse>(
//                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
//                Endpoint.ACCOUNTS,
//                ResponseSpecs.entityWasCreated())
//                .post();

//        CreateUserRequest createSecondUserRequest = RandomModelGenerator.generate(CreateUserRequest.class);

        //создать 2 юзера
        CreateUserRequest createSecondUserRequest = AdminSteps.createUserGetRequest();
//        new CrudRequester(
//                RequestSpecs.adminSpec(),
//                Endpoint.ADMIN_USERS,
//                ResponseSpecs.entityWasCreated())
//                .post(createSecondUserRequest);

        //создать аккаунт 2 юзера
        CreateAccountResponse createSecondUserAccountResponse = UserSteps.createAccount(createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword());
//        CreateAccountResponse createSecondUserAccountResponse = new ValidatedCrudRequester<CreateAccountResponse>(
//                RequestSpecs.authAsUser(createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword()),
//                Endpoint.ACCOUNTS,
//                ResponseSpecs.entityWasCreated())
//                .post();

//        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
//                .id(createFirstUserAccountResponse.getId())
//                .balance(MAX_DEPOSIT_AMOUNT.getAmount())
//                .build();

        //депозит
        UserSteps.depositMaxDepositAmount(
                createFirstUserAccountResponse.getId(), createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword());
//        new CrudRequester(
//                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
//                Endpoint.ACCOUNTS_DEPOSIT,
//                ResponseSpecs.requestReturnsOK())
//                .post(depositMoneyRequest);

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
        List<GetCustomerAccountsResponse> firstUserGetCustomerAccountsResponseList = UserSteps.getAccounts(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword());
//        List<GetCustomerAccountsResponse> firstUserGetCustomerAccountsResponseList = new ValidatedCrudRequester<GetCustomerAccountsResponse>(
//                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
//                Endpoint.CUSTOMER_ACCOUNTS,
//                ResponseSpecs.requestReturnsOK())
//                .getList(new TypeRef<List<GetCustomerAccountsResponse>>() {});

        GetCustomerAccountsResponse firstUserGetCustomerAccountsResponse = firstUserGetCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse -> getCustomerAccountResponse.getId() == createFirstUserAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        //проверка счета 2 пользователя
        List<GetCustomerAccountsResponse> secondUserGetCustomerAccountsResponseList = UserSteps.getAccounts(createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword());
//        List<GetCustomerAccountsResponse> secondUserGetCustomerAccountsResponseList = new ValidatedCrudRequester<GetCustomerAccountsResponse>(
//                RequestSpecs.authAsUser(createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword()),
//                Endpoint.CUSTOMER_ACCOUNTS,
//                ResponseSpecs.requestReturnsOK())
//                .getList(new TypeRef<List<GetCustomerAccountsResponse>>() {});

        GetCustomerAccountsResponse secondUserGetCustomerAccountsResponse = secondUserGetCustomerAccountsResponseList.stream()
                .filter(getCustomerAccountResponse -> getCustomerAccountResponse.getId() == createSecondUserAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(firstUserGetCustomerAccountsResponse.getBalance()).isEqualTo(MAX_DEPOSIT_AMOUNT.getAmount());

        softly.assertThat(secondUserGetCustomerAccountsResponse.getBalance()).isEqualTo(0.0f);
        softly.assertThat(secondUserGetCustomerAccountsResponse.getTransactions()).isEmpty();
    }
}

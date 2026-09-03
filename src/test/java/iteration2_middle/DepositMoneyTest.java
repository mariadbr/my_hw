package iteration2_middle;

import generators.RandomData;
import io.restassured.common.mapper.TypeRef;
import models.*;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import requests.AdminCreateUserRequester;
import requests.CreateAccountRequester;
import requests.DepositMoneyRequester;
import requests.GetCustomerAccountsRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.List;
import java.util.Random;
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
        new AdminCreateUserRequester(RequestSpecs.adminSpec(), ResponseSpecs.entityWasCreated())
                .post(createUserRequest);

        //создать аккаунт
        CreateAccountResponse createAccountResponse = new CreateAccountRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(CreateAccountResponse.class);

        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
                .id(createAccountResponse.getId())
                .balance(amount)
                .build();

        //депозит денег
        DepositMoneyResponse depositMoneyResponse = new DepositMoneyRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .post(depositMoneyRequest)
                .extract()
                .as(DepositMoneyResponse.class);

        assertThat(depositMoneyResponse.getBalance(), Matchers.equalTo(amount));

        //проверка
        List<AccountResponse> accountResponseList = new GetCustomerAccountsRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(new TypeRef<List<AccountResponse>>() {});

        AccountResponse account = accountResponseList.stream()
                .filter(accountResponse -> accountResponse.getId() == createAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        assertThat(account.getBalance(), Matchers.equalTo(amount));
    }

    @ParameterizedTest
    @ValueSource(floats = {5000.01f})
    public void checkBoundaryValuesDepositNegativeCase(float amount) {
        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        //создать юзера
        new AdminCreateUserRequester(RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(createUserRequest);

        //создать аккаунт
        CreateAccountResponse createAccountResponse = new CreateAccountRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(CreateAccountResponse.class);

        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
                .id(createAccountResponse.getId())
                .balance(amount)
                .build();

        //депозит денег
        new DepositMoneyRequester(RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequestWithText( AlertMessage.DEPOSIT_AMOUNT_CANNOT_EXCEED_5000.getMessage()))
                .post(depositMoneyRequest);

        //проверка
        List<AccountResponse> accountResponseList = new GetCustomerAccountsRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(new TypeRef<List<AccountResponse>>() {});

        AccountResponse account = accountResponseList.stream()
                .filter(accountResponse -> accountResponse.getId() == createAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(account.getBalance()).isEqualTo(0.0f);
        softly.assertThat(account.getTransactions()).isEmpty();
    }

    @Test
    public void userCannotDepositNegativeAmountIntoOwnAccount() {
        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        //создать юзера
        new AdminCreateUserRequester(RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(createUserRequest);

        //создать аккаунт
        CreateAccountResponse createAccountResponse = new CreateAccountRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post()
                .extract()
                .as(CreateAccountResponse.class);

        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
                .id(createAccountResponse.getId())
                .balance(RandomData.getRandomNegativeFloat())
                .build();

        //депозит денег
        new DepositMoneyRequester(RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequestWithText( AlertMessage.DEPOSIT_AMOUNT_MUST_BE_AT_LEAST_001.getMessage()))
                .post(depositMoneyRequest);

        //проверка
        List<AccountResponse> accountResponseList = new GetCustomerAccountsRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(new TypeRef<List<AccountResponse>>() {});

        AccountResponse account = accountResponseList.stream()
                .filter(accountResponse -> accountResponse.getId() == createAccountResponse.getId())
                .findFirst()
                .orElseThrow();

        softly.assertThat(account.getBalance()).isEqualTo(0.0f);
        softly.assertThat(account.getTransactions()).isEmpty();
    }

    @Test
    public void userCannotDepositAmountIntoSomeonesAccount() {
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
        new CreateAccountRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post();

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
                .id(createSecondUserAccountResponse.getId())
                .balance(RandomData.getRandomPositiveFloat())
                .build();

        new DepositMoneyRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsForbidden(AlertMessage.UNAUTHORIZED_ACCESS_TO_ACCOUNT.getMessage()))
                .post(depositMoneyRequest);

        //проверка аккаунта 1 юзера
        List<AccountResponse> firstUserAccountResponseList = new GetCustomerAccountsRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(new TypeRef<List<AccountResponse>>() {});

        softly.assertThat(firstUserAccountResponseList.getFirst().getBalance()).isEqualTo(0.0f);
        softly.assertThat(firstUserAccountResponseList.getFirst().getTransactions()).isEmpty();

        //проверка аккаунта 2 юзера
        List<AccountResponse> secondUserAccountResponseList = new GetCustomerAccountsRequester(
                RequestSpecs.authAsUser(createSecondUserRequest.getUsername(), createSecondUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(new TypeRef<List<AccountResponse>>() {});

        softly.assertThat(secondUserAccountResponseList.getFirst().getBalance()).isEqualTo(0.0f);
        softly.assertThat(secondUserAccountResponseList.getFirst().getTransactions()).isEmpty();
    }

    @Test
    public void userCannotDepositAmountIntoNonExistentAccount() {
        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        //создать юзера
        new AdminCreateUserRequester(RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(createUserRequest);

        //создать аккаунт
        new CreateAccountRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post();

        //депозит денег
        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
                .id(1000)
                .balance(RandomData.getRandomPositiveFloat())
                .build();

        //депозит денег
        new DepositMoneyRequester(RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.requestReturnsForbidden(AlertMessage.UNAUTHORIZED_ACCESS_TO_ACCOUNT.getMessage()))
                .post(depositMoneyRequest);

        List<AccountResponse> accountResponseList = new GetCustomerAccountsRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .get()
                .extract()
                .as(new TypeRef<List<AccountResponse>>() {});

        softly.assertThat(accountResponseList.getFirst().getTransactions()).isEmpty();
        softly.assertThat(accountResponseList.getFirst().getBalance()).isEqualTo(0.0f);
    }
}

package iteration2;

import generators.RandomData;
import io.restassured.http.ContentType;
import models.*;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.AdminCreateUserRequester;
import requests.CreateAccountRequester;
import requests.DepositMoneyRequester;
import requests.LoginUserRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.responseSpecification;
import static org.hamcrest.MatcherAssert.assertThat;

public class DepositMoneyTest extends BaseTest {

    public static Stream<Arguments> validUserAndAmountDataForDeposit() {
        return Stream.of(
                Arguments.of(RandomData.getUsername(), 0.01f),  //почему здесь нужно f?
                Arguments.of(RandomData.getUsername(), 4999.99f));
    }

    @MethodSource("validUserAndAmountDataForDeposit")
    @ParameterizedTest
    public void userCanDepositValidAmountIntoOwnAccount(String username, float amount) {
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
                .post(null)
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
    }

    @Test
    public void userCannotDepositInvalidAmountIntoOwnAccount() {
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
        CreateAccountResponse createAccountResponse = new CreateAccountRequester(RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .as(CreateAccountResponse.class);

        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
                .id(createAccountResponse.getId())
                .balance(5000.01f)
                .build();

        //депозит денег
        new DepositMoneyRequester(RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequestWithText( "Deposit amount cannot exceed 5000"))
                .post(depositMoneyRequest);
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
        CreateAccountResponse createAccountResponse = new CreateAccountRequester(RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .as(CreateAccountResponse.class);

        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
                .id(createAccountResponse.getId())
                .balance(-5f)
                .build();

        //депозит денег
        new DepositMoneyRequester(RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequestWithText( "Deposit amount must be at least 0.01"))
                .post(depositMoneyRequest);
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
                .post(null);

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
                .id(createSecondUserAccountResponse.getId())
                .balance(100.5f)
                .build();

        new DepositMoneyRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsForbidden("Unauthorized access to account"))
                .post(depositMoneyRequest);
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
                .post(null);

        //депозит денег
        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
                .id(95)
                .balance(100.5f)
                .build();

        //депозит денег
        new DepositMoneyRequester(RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.requestReturnsForbidden("Unauthorized access to account"))
                .post(depositMoneyRequest);
    }
}

package iteration2;

import generators.RandomData;
import models.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import requests.AdminCreateUserRequester;
import requests.CreateAccountRequester;
import requests.DepositMoneyRequester;
import requests.TransferMoneyRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import static io.restassured.RestAssured.given;

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
                .amount(50.05f)
                .build();

        new TransferMoneyRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .post(transferMoneyRequest);
        //доделать проверку + скрин

//        //проверка
//        given()
//                .contentType(ContentType.JSON)
//                .accept(ContentType.JSON)
//                .header("authorization", firstUserAuthToken)
//                .pathParam("accountId", firstUserAccountId)
//                .get("http://localhost:4111/api/v1/accounts/{accountId}/transactions")
//                .then()
//                .assertThat()
//                .body("find { it.type == 'TRANSFER_OUT' }.amount", Matchers.equalTo(50.05f))
//                .body("find { it.type == 'TRANSFER_OUT' }.relatedAccountId", Matchers.equalTo(secondUserAccountId));
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
        DepositMoneyResponse firstDepositMoneyResponse = new DepositMoneyRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .post(depositMoneyRequest)
                .extract()
                .as(DepositMoneyResponse.class);

        DepositMoneyResponse secondDepositMoneyResponse = new DepositMoneyRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .post(depositMoneyRequest)
                .extract()
                .as(DepositMoneyResponse.class);

        //трансфер
        TransferMoneyRequest transferMoneyRequest = TransferMoneyRequest.builder()
                .senderAccountId(createFirstAccountResponse.getId())
                .receiverAccountId(createSecondAccountResponse.getId())
                .amount(amount)
                .build();

        new TransferMoneyRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .post(transferMoneyRequest);




//        //проверка
//        given()
//                .contentType(ContentType.JSON)
//                .accept(ContentType.JSON)
//                .header("authorization", userAuthToken)
//                .pathParam("accountId", userFirstAccountId)
//                .get("http://localhost:4111/api/v1/accounts/{accountId}/transactions")
//                .then()
//                .assertThat()
//                .body("find { it.type == 'TRANSFER_OUT' }.amount", Matchers.equalTo(amount))
//                .body("find { it.type == 'TRANSFER_OUT' }.relatedAccountId", Matchers.equalTo(userSecondAccountId));
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
                .balance(600.55f)
                .build();

        //депозит денег
        DepositMoneyResponse firstDepositMoneyResponse = new DepositMoneyRequester(
                RequestSpecs.authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword()),
                ResponseSpecs.requestReturnsOK())
                .post(depositMoneyRequest)
                .extract()
                .as(DepositMoneyResponse.class);

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
                .amount(100.2f)
                .build();

        new TransferMoneyRequester(
                RequestSpecs.authAsUser(createFirstUserRequest.getUsername(), createFirstUserRequest.getPassword()),
                ResponseSpecs.requestReturnsBadRequestWithText("Invalid transfer: insufficient funds or invalid accounts"))
                .post(transferMoneyRequest);
    }
}

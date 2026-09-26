package iteration2.ui;

import api.models.*;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import api.generators.RandomData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.CrudRequester;
import api.requests.steps.AdminSteps;
import api.requests.steps.UserSteps;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

import java.util.Arrays;
import java.util.Map;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.switchTo;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransferMoneyTest {
    @BeforeAll
    public static void setupSelenoid() {
        Configuration.remote = "http://localhost:4444/wd/hub";
        Configuration.baseUrl = "http://192.168.0.124:3000";
        Configuration.browser = "chrome";
        Configuration.browserSize = "1920x1080";

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true)
        );
    }

    @Test
    public void userCanTransferValidAmount() {
        CreateUserRequest firstUser = AdminSteps.createUserReturnRequest();

        CreateAccountResponse createFirstUserAccountResponse = UserSteps.createAccount(firstUser.getUsername(), firstUser.getPassword());

        UserSteps.depositMaxDepositAmount(createFirstUserAccountResponse.getId(), firstUser.getUsername(), firstUser.getPassword());

        CreateUserRequest secondUser = AdminSteps.createUserReturnRequest();

        CreateAccountResponse createSecondUserAccountResponse = UserSteps.createAccount(secondUser.getUsername(), secondUser.getPassword());

        String firstUserAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.AUTH_LOGIN,
                ResponseSpecs.requestReturnsOK())
                .post(LoginUserRequest.builder().username(firstUser.getUsername()).password(firstUser.getPassword()).build())
                .extract()
                .header("authorization");

        Selenide.open("/");

        Selenide.localStorage().setItem("authToken", firstUserAuthHeader);

        Selenide.open("/dashboard");

        $(Selectors.byText("🔄 Make a Transfer")).click();

        $(Selectors.byClassName("account-selector")).click();
        $(".account-selector").selectOptionContainingText(createFirstUserAccountResponse.getAccountNumber());
        $(Selectors.byPlaceholder("Enter recipient account number")).sendKeys(createSecondUserAccountResponse.getAccountNumber());
        float amount = RandomData.getRandomPositiveFloat();
        $(Selectors.byAttribute("placeholder", "Enter amount")).sendKeys(String.valueOf(amount));
        $("#confirmCheck").click();
        $(Selectors.byText("🚀 Send Transfer")).click();

        Alert alert = switchTo().alert();

        assertThat(alert.getText()).contains("✅ Successfully transferred ", createSecondUserAccountResponse.getAccountNumber());

        alert.accept();

        //проверка через api
        GetCustomerAccountsResponse[] firstUserAccounts = given()
                .spec(RequestSpecs.authAsUser(firstUser.getUsername(), firstUser.getPassword()))
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .spec(ResponseSpecs.requestReturnsOK())
                .extract()
                .as(GetCustomerAccountsResponse[].class);

        GetCustomerAccountsResponse firstUserAccount = Arrays.stream(firstUserAccounts).findFirst().orElseThrow();

        assertEquals(UserBalanceDefaults.MAX_DEPOSIT_AMOUNT.getAmount() - amount, firstUserAccount.getBalance());
        assertThat(firstUserAccount.getTransactions()).hasSize(2);

        GetCustomerAccountsResponse[] secondUserAccounts = given()
                .spec(RequestSpecs.authAsUser(secondUser.getUsername(), secondUser.getPassword()))
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .spec(ResponseSpecs.requestReturnsOK())
                .extract()
                .as(GetCustomerAccountsResponse[].class);

        GetCustomerAccountsResponse secondUserAccount = Arrays.stream(secondUserAccounts).findFirst().orElseThrow();

        assertEquals(amount, secondUserAccount.getBalance());
        assertThat(secondUserAccount.getTransactions()).hasSize(1);
    }

    @Test
    public void userCannotTransferInvalidAmount() {
        CreateUserRequest firstUser = AdminSteps.createUserReturnRequest();

        CreateAccountResponse createFirstUserAccountResponse = UserSteps.createAccount(firstUser.getUsername(), firstUser.getPassword());

        UserSteps.depositMaxDepositAmount(createFirstUserAccountResponse.getId(), firstUser.getUsername(), firstUser.getPassword());

        CreateUserRequest secondUser = AdminSteps.createUserReturnRequest();

        CreateAccountResponse createSecondUserAccountResponse = UserSteps.createAccount(secondUser.getUsername(), secondUser.getPassword());

        String firstUserAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.AUTH_LOGIN,
                ResponseSpecs.requestReturnsOK())
                .post(LoginUserRequest.builder().username(firstUser.getUsername()).password(firstUser.getPassword()).build())
                .extract()
                .header("authorization");

        Selenide.open("/");

        Selenide.localStorage().setItem("authToken", firstUserAuthHeader);

        Selenide.open("/dashboard");

        $(Selectors.byText("🔄 Make a Transfer")).click();

        $(Selectors.byClassName("account-selector")).click();
        $(".account-selector").selectOptionContainingText(createFirstUserAccountResponse.getAccountNumber());
        $(Selectors.byPlaceholder("Enter recipient account number")).sendKeys(createSecondUserAccountResponse.getAccountNumber());
        $(Selectors.byAttribute("placeholder", "Enter amount")).sendKeys("-5");
        $("#confirmCheck").click();
        $(Selectors.byText("🚀 Send Transfer")).click();

        Alert alert = switchTo().alert();

        assertEquals("❌ Error: Transfer amount must be at least 0.01", alert.getText());

        alert.accept();

        //проверка через api
        GetCustomerAccountsResponse[] firstUserAccounts = given()
                .spec(RequestSpecs.authAsUser(firstUser.getUsername(), firstUser.getPassword()))
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .spec(ResponseSpecs.requestReturnsOK())
                .extract()
                .as(GetCustomerAccountsResponse[].class);

        GetCustomerAccountsResponse firstUserAccount = Arrays.stream(firstUserAccounts).findFirst().orElseThrow();

        assertThat(firstUserAccount.getBalance()).isEqualTo(UserBalanceDefaults.MAX_DEPOSIT_AMOUNT.getAmount());
        assertThat(firstUserAccount.getTransactions()).hasSize(1);

        GetCustomerAccountsResponse[] secondUserAccounts = given()
                .spec(RequestSpecs.authAsUser(secondUser.getUsername(), secondUser.getPassword()))
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .spec(ResponseSpecs.requestReturnsOK())
                .extract()
                .as(GetCustomerAccountsResponse[].class);

        GetCustomerAccountsResponse secondUserAccount = Arrays.stream(secondUserAccounts).findFirst().orElseThrow();

        assertThat(secondUserAccount.getBalance()).isEqualTo(UserBalanceDefaults.INITIAL_BALANCE.getAmount());
        assertThat(secondUserAccount.getTransactions().size()).isZero();
    }
}

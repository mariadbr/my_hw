package iteration2.ui;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import generators.RandomData;
import models.CreateAccountResponse;
import models.CreateUserRequest;
import models.GetCustomerAccountsResponse;
import models.LoginUserRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.Arrays;
import java.util.Map;

import static com.codeborne.selenide.Selenide.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DepositMoneyTest {
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
    public void userCanDepositValidAmount() {
        CreateUserRequest user = AdminSteps.createUserReturnRequest();

        CreateAccountResponse createAccountResponse =
                UserSteps.createAccount(user.getUsername(), user.getPassword());

        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.AUTH_LOGIN,
                ResponseSpecs.requestReturnsOK())
                .post(LoginUserRequest.builder().username(user.getUsername()).password(user.getPassword()).build())
                .extract()
                .header("authorization");

        Selenide.open("/");

        Selenide.localStorage().setItem("authToken", userAuthHeader);

        Selenide.open("/dashboard");

        $(Selectors.byText("💰 Deposit Money")).click();

        $(Selectors.byClassName("account-selector")).click();
        $(".account-selector").selectOptionContainingText(createAccountResponse.getAccountNumber());
        float amount = RandomData.getRandomPositiveFloat();
        $(Selectors.byAttribute("placeholder", "Enter amount")).sendKeys(String.valueOf(amount));
        $(Selectors.byText("💵 Deposit")).click();

        Alert alert = switchTo().alert();

        assertThat(alert.getText()).contains("✅ Successfully deposited", createAccountResponse.getAccountNumber());

        alert.accept();

        //проверка через api
        GetCustomerAccountsResponse[] accounts = given()
                .spec(RequestSpecs.authAsUser(user.getUsername(), user.getPassword()))
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .spec(ResponseSpecs.requestReturnsOK())
                .extract()
                .as(GetCustomerAccountsResponse[].class);

        GetCustomerAccountsResponse account = Arrays.stream(accounts).findFirst().orElseThrow();

        assertEquals(amount, account.getBalance());
        assertThat(account.getTransactions()).isNotEmpty();
    }

    @Test
    public void userCannotDepositInvalidAmount() {
        CreateUserRequest user = AdminSteps.createUserReturnRequest();

        CreateAccountResponse createAccountResponse =
                UserSteps.createAccount(user.getUsername(), user.getPassword());

        String userAuthHeader = new CrudRequester(
                RequestSpecs.unauthSpec(),
                Endpoint.AUTH_LOGIN,
                ResponseSpecs.requestReturnsOK())
                .post(LoginUserRequest.builder().username(user.getUsername()).password(user.getPassword()).build())
                .extract()
                .header("authorization");

        Selenide.open("/");

        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);

        Selenide.open("/dashboard");

        $(Selectors.byText("💰 Deposit Money")).click();

        $(Selectors.byClassName("account-selector")).click();
        $(".account-selector").selectOptionContainingText(createAccountResponse.getAccountNumber());
        $(Selectors.byAttribute("placeholder", "Enter amount")).sendKeys(String.valueOf(RandomData.getRandomNegativeFloat()));
        $(Selectors.byText("💵 Deposit")).click();

        Alert alert = switchTo().alert();

        assertEquals("❌ Please enter a valid amount.", alert.getText());

        alert.accept();

        //проверка через api
        GetCustomerAccountsResponse[] accounts = given()
                .spec(RequestSpecs.authAsUser(user.getUsername(), user.getPassword()))
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .spec(ResponseSpecs.requestReturnsOK())
                .extract()
                .as(GetCustomerAccountsResponse[].class);

        GetCustomerAccountsResponse account = Arrays.stream(accounts).findFirst().orElseThrow();

        assertThat(account.getBalance()).isZero();
        assertThat(account.getTransactions()).isEmpty();
    }
}

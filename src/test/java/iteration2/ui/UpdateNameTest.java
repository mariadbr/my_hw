package iteration2.ui;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import generators.RandomData;
import models.CreateUserRequest;
import models.GetCustomerProfileResponse;
import models.LoginUserRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.CrudRequester;
import requests.steps.AdminSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.Map;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.switchTo;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class UpdateNameTest {
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
    public void userCanUpdateName() {
        CreateUserRequest user = AdminSteps.createUserReturnRequest();

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

        $("div.user-info").click();
        String newName = RandomData.getRandomValidName();
        $(Selectors.byPlaceholder("Enter new name")).sendKeys(newName);
        $(Selectors.byText("💾 Save Changes")).click();

        Alert alert = switchTo().alert();

        assertEquals("✅ Name updated successfully!", alert.getText());

        alert.accept();

        Selenide.refresh();

        $(".user-name").shouldBe(Condition.visible).shouldHave(Condition.exactText(newName));

        //проверка апи
        GetCustomerProfileResponse response = given()
                .spec(RequestSpecs.authAsUser(user.getUsername(), user.getPassword()))
                .get("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .spec(ResponseSpecs.requestReturnsOK())
                .extract().as(GetCustomerProfileResponse.class);

        assertEquals(newName, response.getName());
    }

    @Test
    public void userCannotUpdateNameWithInvalidValue() {
        CreateUserRequest user = AdminSteps.createUserReturnRequest();

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

        $("div.user-info").click();
        $(Selectors.byPlaceholder("Enter new name")).sendKeys("aa");
        $(Selectors.byText("💾 Save Changes")).click();

        Alert alert = switchTo().alert();

        assertEquals("Name must contain two words with letters only", alert.getText());

        alert.accept();

        Selenide.refresh();

        $(".user-name").shouldBe(Condition.visible).shouldBe(Condition.exactText("Noname"));

        //проверка апи
        GetCustomerProfileResponse response = given()
                .spec(RequestSpecs.authAsUser(user.getUsername(), user.getPassword()))
                .get("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .spec(ResponseSpecs.requestReturnsOK())
                .extract().as(GetCustomerProfileResponse.class);

        assertThat(response.getName()).isNull();
    }
}

package ui;

import api.models.CreateUserRequest;
import api.specs.RequestSpecs;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.Alert;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.switchTo;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class BasePage<T extends BasePage> {
    protected SelenideElement accountSelector = $(".account-selector");

    abstract String url();

    public T open() {
        return Selenide.open(url(), (Class<T>) this.getClass());
    }

    public T checkAlertMessageAndAccept(String... expectedMessages) {
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains(expectedMessages);
        alert.accept();
        return (T) this;
    }

    public <T extends BasePage> T getPage(Class<T> pageClass) {
        return Selenide.page(pageClass);
    }

    public static void authAsUser(String username, String password) {
        String userAuthHeader = RequestSpecs.getAuthHeader(username, password);
        Selenide.open("/");
        Selenide.localStorage().setItem("authToken", userAuthHeader);
    }

    public static void authAsUser(CreateUserRequest createUserRequest) {
        authAsUser(createUserRequest.getUsername(), createUserRequest.getPassword());
    }

}

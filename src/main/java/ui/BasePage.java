package ui;

import api.models.CreateUserRequest;
import api.specs.RequestSpecs;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import common.utils.RetryUtils;
import lombok.Getter;
import org.openqa.selenium.Alert;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.switchTo;
import static org.assertj.core.api.Assertions.assertThat;

@Getter
public abstract class BasePage<T extends BasePage> {
    protected SelenideElement accountSelector = $("select.account-selector");
    protected SelenideElement userName = $("span.user-name");

    abstract String url();

    public T open() {
        return Selenide.open(url(), (Class<T>) this.getClass());
    }

    public T checkAlertMessageAndAccept(String... expectedMessages) {
        Alert alert = RetryUtils.retry(
                () -> {
                    try {
                        return switchTo().alert();
                    } catch (org.openqa.selenium.NoAlertPresentException e) {
                        return null;
                    }
                },
                result -> result != null,
                5,
                1000
        );

        try {
            assertThat(alert.getText()).contains(expectedMessages);
        } finally {
            alert.accept();
        }
        return (T) this;
    }

    public <P extends BasePage> P getPage(Class<P> pageClass) {
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

    public T refreshPage() {
         Selenide.refresh();
         return (T) this;
    }

}

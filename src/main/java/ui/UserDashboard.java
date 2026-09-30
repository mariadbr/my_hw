package ui;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;

import static com.codeborne.selenide.Selenide.$;

@Getter
public class UserDashboard extends BasePage<UserDashboard>{
    private SelenideElement depositMoneyButton = $(Selectors.byText("💰 Deposit Money"));
    private SelenideElement makeTransferButton = $(Selectors.byText("🔄 Make a Transfer"));
    private SelenideElement userInfo = $("div.user-info");
    private SelenideElement userName = $("span.user-name");

    @Override
    String url() {
        return "/dashboard";
    }

    public UserDashboard openDepositPage() {
        depositMoneyButton.click();
        return this;
    }

    public UserDashboard openTransferPage() {
        makeTransferButton.click();
        return this;
    }

    public UserDashboard openEditProfilePage() {
        userInfo.click();
        return this;
    }
}

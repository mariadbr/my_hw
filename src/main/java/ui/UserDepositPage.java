package ui;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class UserDepositPage extends BasePage<UserDepositPage> {
    private SelenideElement enterAmountInput = $(Selectors.byAttribute("placeholder", "Enter amount"));
    private SelenideElement depositButton = $(Selectors.byText("💵 Deposit"));

    public UserDepositPage depositMoney(String accountNumber, String amount) {
        accountSelector.selectOptionContainingText(accountNumber);
        enterAmountInput.sendKeys(amount);
        depositButton.click();
        return this;
    }

    @Override
    String url() {
        return "/deposit";
    }
}

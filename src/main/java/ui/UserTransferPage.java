package ui;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;

import java.time.Duration;

import static com.codeborne.selenide.Selenide.$;

public class UserTransferPage extends BasePage<UserTransferPage>{
    private SelenideElement enterRecipientAccountInput = $(Selectors.byPlaceholder("Enter recipient account number"));
    private SelenideElement enterAmountInput = $(Selectors.byAttribute("placeholder", "Enter amount"));
    private SelenideElement confirmCheckbox = $("#confirmCheck");
    private SelenideElement sendTransferButton = $(Selectors.byText("🚀 Send Transfer"));

    @Override
    String url() {
        return "/transfer";
    }

    public UserTransferPage makeTransfer(String senderAccount, String recipientAccount, String amount) {
        accountSelector.$$("option")
                .findBy(Condition.text(senderAccount))
                .shouldBe(Condition.exist, Duration.ofSeconds(6));

        accountSelector.selectOptionContainingText(senderAccount);

        enterRecipientAccountInput.sendKeys(recipientAccount);
        enterAmountInput.sendKeys(amount);
        confirmCheckbox.click();
        sendTransferButton.click();
        return this;
    }
}

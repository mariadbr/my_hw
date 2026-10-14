package ui;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;

import static com.codeborne.selenide.Selenide.$;

@Getter
public class EditProfilePage extends BasePage<EditProfilePage>{
    private SelenideElement enterNewNameInput = $(Selectors.byPlaceholder("Enter new name"));
    private SelenideElement saveChangesButton = $(Selectors.byText("💾 Save Changes"));

    @Override
    String url() {
        return "/edit-profile";
    }

//    public EditProfilePage editProfileName(String newName) {
//        enterNewNameInput.sendKeys(newName);
//        saveChangesButton.click();
//
//        enterNewNameInput.shouldHave(Condition.value(newName));
//        return this;

    public EditProfilePage enterNameAndWaitUntilStable(String name) {
        long startTime = System.currentTimeMillis();
        long timeout = 10_000;
        int maxAttempts = 10;
        int attempts = 0;

        while (System.currentTimeMillis() - startTime < timeout) {
            String actualValue = enterNewNameInput.getValue();

            if (!name.equals(actualValue)) {
                if (attempts >= maxAttempts) {
                    throw new AssertionError(
                            "Failed to save name in the input field after "
                                    + maxAttempts + " attempts"
                    );
                }

                enterNewNameInput.clear();
                enterNewNameInput.sendKeys(name);
                attempts++;
            }

            Selenide.sleep(1000);
        }

        enterNewNameInput.shouldHave(Condition.value(name));

        return this;
    }

    public EditProfilePage saveChanges() {
        saveChangesButton.click();
        return this;
    }
}

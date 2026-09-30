package ui;

import com.codeborne.selenide.Selectors;
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

    public EditProfilePage editProfileName(String newName) {
        enterNewNameInput.sendKeys(newName);
        saveChangesButton.click();
        return this;
    }
}

package iteration2.ui;

import api.generators.RandomData;
import api.models.CreateUserRequest;
import api.models.GetCustomerProfileResponse;
import api.requests.steps.AdminSteps;
import api.requests.steps.UserSteps;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import ui.BankAlert;
import ui.EditProfilePage;
import ui.UserDashboard;

public class UpdateNameTest extends BaseUiTest {

    @Test
    public void userCanUpdateName() {
        CreateUserRequest user = AdminSteps.createUserReturnRequest();

        authAsUser(user);

        String newName = RandomData.getRandomValidName();

        new UserDashboard().open().openEditProfilePage()
                .getPage(EditProfilePage.class).open().editProfileName(newName)
                .checkAlertMessageAndAccept(BankAlert.NAME_UPDATED_SUCCESSFULLY.getMessage());

        Selenide.refresh();

        new UserDashboard().getUserName().shouldBe(Condition.visible).shouldHave(Condition.exactText(newName));

        //проверка апи
        GetCustomerProfileResponse response = UserSteps.getProfile(user.getUsername(), user.getPassword());

        softly.assertThat(newName).isEqualTo(response.getName());
    }

    @Test
    public void userCannotUpdateNameWithInvalidValue() {
        CreateUserRequest user = AdminSteps.createUserReturnRequest();

        authAsUser(user);

        new UserDashboard().open().openEditProfilePage()
                .getPage(EditProfilePage.class).editProfileName(RandomStringUtils.randomAlphabetic(2))
                .checkAlertMessageAndAccept(BankAlert.NAME_MUST_CONTAIN_TWO_WORDS_WITH_LETTERS_ONLY.getMessage());

        Selenide.refresh();

        new UserDashboard().getUserName().shouldBe(Condition.visible).shouldBe(Condition.exactText("Noname"));

        //проверка апи
        GetCustomerProfileResponse response = UserSteps.getProfile(user.getUsername(), user.getPassword());

        softly.assertThat(response.getName()).isNull();
    }
}

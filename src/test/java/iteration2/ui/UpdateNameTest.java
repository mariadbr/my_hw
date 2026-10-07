package iteration2.ui;

import api.generators.RandomData;
import api.models.GetAllUsersResponse;
import api.models.GetCustomerProfileResponse;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import com.codeborne.selenide.Condition;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import ui.BankAlert;
import ui.EditProfilePage;
import ui.UserDashboard;

import static io.restassured.RestAssured.given;

public class UpdateNameTest extends BaseUiTest {
    @Test
    @UserSession
    public void userCanUpdateName() {
        String newName = RandomData.getRandomValidName();

        new UserDashboard()
                .open()
                .openEditProfilePage()
                .getPage(EditProfilePage.class)
                .editProfileName(newName)
                .checkAlertMessageAndAccept(BankAlert.NAME_UPDATED_SUCCESSFULLY.getMessage())
                .refreshPage()
                .getUserName()
                .shouldBe(Condition.visible).shouldHave(Condition.exactText(newName));

        //проверка апи
        GetCustomerProfileResponse response = SessionStorage.getSessionData().getSteps().getProfile();

        softly.assertThat(newName).isEqualTo(response.getName());

        GetAllUsersResponse[] users = given()
                .spec(RequestSpecs.adminSpec())
                .get("/admin/users")
                .then()
                .assertThat()
                .spec(ResponseSpecs.requestReturnsOK())
                .extract()
                .as(GetAllUsersResponse[].class);

//        for (GetAllUsersResponse user: users) {
//            given()
//                    .spec(RequestSpecs.adminSpec())
//                    .pathParam("id", user.getId())
//                    .delete("/admin/users/{id}")
//                    .then()
//                    .assertThat()
//                    .spec(ResponseSpecs.requestReturnsOK());
//        }

//        given()
//                .spec(RequestSpecs.adminSpec())
//                .pathParam("id", response.getId())
//                .delete("/admin/users/{id}")
//                .then()
//                .assertThat()
//                .spec(ResponseSpecs.requestReturnsOK());

    }

    @Test
    @UserSession
    public void userCannotUpdateNameWithInvalidValue() {
        new UserDashboard()
                .open()
                .openEditProfilePage()
                .getPage(EditProfilePage.class)
                .editProfileName(RandomStringUtils.randomAlphabetic(2))
                .checkAlertMessageAndAccept(BankAlert.NAME_MUST_CONTAIN_TWO_WORDS_WITH_LETTERS_ONLY.getMessage())
                .refreshPage()
                .getUserName()
                .shouldBe(Condition.visible)
                .shouldBe(Condition.exactText(UserDashboard.USER_DEFAULT_NAME));

        //проверка апи
        GetCustomerProfileResponse response = SessionStorage.getSessionData().getSteps().getProfile();

        softly.assertThat(response.getName()).isNull();
    }
}

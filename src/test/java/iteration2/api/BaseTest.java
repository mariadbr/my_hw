package iteration2.api;

import api.models.GetAllUsersResponse;
import api.requests.steps.AdminSteps;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import common.extensions.TimingExtension;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static io.restassured.RestAssured.given;

@ExtendWith(TimingExtension.class)
public class BaseTest {
    protected SoftAssertions softly;
    protected static final float MARGIN_OF_ERROR = 0.001f;

    @BeforeEach
    public void setupTest() {
        this.softly = new SoftAssertions();
    }

    @AfterEach
    public void afterTest() {
        softly.assertAll();
    }

//    @AfterAll
//    public static void cleanAll() {
//        List<GetAllUsersResponse> users = AdminSteps.getAllUsers();
//
//        for (GetAllUsersResponse user : users) {
//            AdminSteps.deleteById(user.getId());
//        }
//    }

    public static void repeat(int times, Runnable action) {
        for (int i = 0; i < times; i++) {
            action.run();
        }
    }
}


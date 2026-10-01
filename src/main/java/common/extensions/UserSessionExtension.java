package common.extensions;

import api.models.CreateUserRequest;
import api.requests.steps.AdminSteps;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import ui.BasePage;

import java.util.ArrayList;
import java.util.List;

public class UserSessionExtension implements BeforeEachCallback {
    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        UserSession annotation = extensionContext.getRequiredTestMethod().getAnnotation(UserSession.class);

        if (annotation != null) {
            int userCount = annotation.value();

            SessionStorage.clear();

            List<CreateUserRequest> users = new ArrayList<>();

            for (int i = 0; i < userCount; i++) {
                CreateUserRequest user = AdminSteps.createUserReturnRequest();
                users.add(user);
            }

            SessionStorage.addUsers(users);

            int authUser = annotation.auth();

            CreateUserRequest user = SessionStorage.getUser(authUser);

            BasePage.authAsUser(user);
        }
    }
}

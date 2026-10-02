package common.extensions;

import api.models.CreateAccountResponse;
import common.annotations.Account;
import common.storage.SessionStorage;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class AccountSessionExtension implements BeforeEachCallback {
    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        Account annotation = extensionContext.getRequiredTestMethod().getAnnotation(Account.class);

        if (annotation != null) {
            int accountCount = annotation.value();

            // нужно ли делать SessionStorage.clear(); ?


            for (int userNumber = 1; userNumber <= SessionStorage.getSessionCount(); userNumber++) {

                for (int i = 0; i < accountCount; i++) {
                    CreateAccountResponse account = SessionStorage.getSession(userNumber)
                            .getSteps()
                            .createAccount();

                    SessionStorage.addAccount(userNumber, account);
                }
            }
        }
    }
}
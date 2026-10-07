package common.extensions;

import api.models.CreateAccountResponse;
import common.annotations.WithAccount;
import common.storage.SessionStorage;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class WithAccountExtension implements BeforeEachCallback {
    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        WithAccount annotation = extensionContext.getRequiredTestMethod().getAnnotation(WithAccount.class);

        if (annotation != null) {
            int accountCount = annotation.value();

            for (int userNumber = 1; userNumber <= SessionStorage.getSessionCount(); userNumber++) {

                for (int i = 0; i < accountCount; i++) {
                    CreateAccountResponse account = SessionStorage.getSessionData(userNumber)
                            .getSteps()
                            .createAccount();

                    SessionStorage.addAccount(userNumber, account);
                }
            }
        }
    }
}
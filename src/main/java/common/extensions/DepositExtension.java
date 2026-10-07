package common.extensions;

import api.models.CreateAccountResponse;
import api.models.DepositMoneyRequest;
import common.annotations.Deposit;
import common.storage.SessionStorage;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DepositExtension implements BeforeEachCallback {
    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        Deposit annotation = extensionContext.getRequiredTestMethod().getAnnotation(Deposit.class);

        if (annotation != null) {
            int userNumber = annotation.user();
            float amount = annotation.amount();

            CreateAccountResponse account = SessionStorage.getSessionData(userNumber).getAccount();

            DepositMoneyRequest request = new DepositMoneyRequest(account.getId(), amount);

            SessionStorage.getSessionData(userNumber).getSteps().depositMoney(request);
        }
    }
}

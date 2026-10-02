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

            CreateAccountResponse account = SessionStorage.getAccount(userNumber); //почему тут userNumber в параметре если в методе там accountNumber?
            // и разве метод не должен быть .getAccount(userNumber, 1) ???

            DepositMoneyRequest request = new DepositMoneyRequest(account.getId(), amount);

            SessionStorage.getSession(userNumber).getSteps().depositMoney(request);
        }
    }
}

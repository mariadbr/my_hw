package iteration2.ui;

import api.generators.RandomData;
import api.models.GetCustomerAccountsResponse;
import common.annotations.WithAccount;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import org.junit.jupiter.api.Test;
import ui.BankAlert;
import ui.UserDashboard;
import ui.UserDepositPage;

public class DepositMoneyTest extends BaseUiTest {
    @Test
    @UserSession
    @WithAccount
    public void userCanDepositValidAmount() {
        String accountNumber = SessionStorage.getAccount().getAccountNumber();

        float amount = RandomData.getRandomPositiveFloat();

        new UserDashboard()
                .open()
                .openDepositPage()
                .getPage(UserDepositPage.class)
                .depositMoney(accountNumber, String.valueOf(amount))
                .checkAlertMessageAndAccept(BankAlert.SUCCESSFULLY_DEPOSITED.getMessage(), accountNumber);

        //проверка через api
        GetCustomerAccountsResponse account = SessionStorage.getSessionData().getSteps().getAccounts()
                .stream().findFirst()
                .orElseThrow();

        softly.assertThat(account.getBalance()).isEqualTo(amount);
        softly.assertThat(account.getTransactions()).isNotEmpty();
    }

    @Test
    @UserSession
    @WithAccount
    public void userCannotDepositInvalidAmount() {
        String accountNumber = SessionStorage.getAccount().getAccountNumber();

        new UserDashboard()
                .open()
                .openDepositPage()
                .getPage(UserDepositPage.class)
                .depositMoney(accountNumber, String.valueOf(RandomData.getRandomNegativeFloat()))
                .checkAlertMessageAndAccept(BankAlert.PLEASE_ENTER_VALID_AMOUNT.getMessage());

        //проверка через api
        GetCustomerAccountsResponse account = SessionStorage.getSessionData().getSteps().getAccounts()
                .stream().findFirst().orElseThrow();

        softly.assertThat(account.getBalance()).isZero();
        softly.assertThat(account.getTransactions()).isEmpty();
    }
}

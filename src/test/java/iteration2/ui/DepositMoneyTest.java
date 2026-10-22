package iteration2.ui;

import api.generators.RandomData;
import api.models.GetCustomerAccountsResponse;
import common.annotations.TestVersion;
import common.annotations.WithAccount;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import org.junit.jupiter.api.Test;
import ui.BankAlert;
import ui.UserDashboard;
import ui.UserDepositPage;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DepositMoneyTest extends BaseUiTest {
    @Test
    @UserSession
    @WithAccount
    @TestVersion({"with_database_with_fix"})
//    "with_deletion"?
    public void userCanDepositValidAmount() {
        String accountNumber = SessionStorage.getAccount().getAccountNumber();

//        float amount = RandomData.getRandomPositiveFloat();
        float amount = BigDecimal.valueOf(RandomData.getRandomPositiveFloat())
                .setScale(2, RoundingMode.HALF_UP)
                .floatValue();

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
    @TestVersion({"with_deletion", "with_database_with_fix"})
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

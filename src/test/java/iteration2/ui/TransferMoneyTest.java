package iteration2.ui;

import api.generators.RandomData;
import api.models.GetCustomerAccountsResponse;
import api.models.UserBalanceDefaults;
import common.annotations.WithAccount;
import common.annotations.Deposit;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import org.junit.jupiter.api.Test;
import ui.BankAlert;
import ui.UserDashboard;
import ui.UserTransferPage;

import static org.assertj.core.api.Assertions.within;

public class TransferMoneyTest extends BaseUiTest {
    private static final float DEPOSIT_AMOUNT = 5000f;

    @Test
    @UserSession(2)
    @WithAccount
    @Deposit(user = 1, amount = DEPOSIT_AMOUNT) //сюда не могу передать енум, нужно compile-time constant
    public void userCanTransferValidAmount() {
        float amount = RandomData.getRandomPositiveFloat();

        String firstUserAccountNum = SessionStorage.getAccount().getAccountNumber();
        String secondUserAccountNum = SessionStorage.getAccount(2, 1).getAccountNumber();

        new UserDashboard()
                .open()
                .openTransferPage()
                .getPage(UserTransferPage.class)
                .makeTransfer(firstUserAccountNum, secondUserAccountNum, String.valueOf(amount))
                .checkAlertMessageAndAccept(BankAlert.SUCCESSFULLY_TRANSFERRED.getMessage(), secondUserAccountNum);

        //проверка через api
        GetCustomerAccountsResponse firstUserAccount = SessionStorage.getSessionData(1).getSteps().getAccounts()
                .stream().findFirst().orElseThrow();

        softly.assertThat(firstUserAccount.getBalance()).isCloseTo(
                UserBalanceDefaults.MAX_DEPOSIT_AMOUNT.getAmount() - amount, within(MARGIN_OF_ERROR));
        softly.assertThat(firstUserAccount.getTransactions()).hasSize(2);


        GetCustomerAccountsResponse secondUserAccount = SessionStorage.getSessionData(2).getSteps().getAccounts()
                .stream().findFirst().orElseThrow();

        softly.assertThat(secondUserAccount.getBalance()).isEqualTo(amount);
        softly.assertThat(secondUserAccount.getTransactions()).hasSize(1);
    }

    @Test
    @UserSession(value = 2, auth = 1)
    @WithAccount
    @Deposit(user = 1, amount = DEPOSIT_AMOUNT)
    public void userCannotTransferInvalidAmount() {
        String firstUserAccountNumber = SessionStorage.getAccount().getAccountNumber();
        String secondUserAccountNumber = SessionStorage.getAccount(2, 1).getAccountNumber();

        new UserDashboard()
                .open()
                .openTransferPage()
                .getPage(UserTransferPage.class)
                .makeTransfer(firstUserAccountNumber, secondUserAccountNumber, String.valueOf(RandomData.getRandomNegativeFloat()))
                .checkAlertMessageAndAccept(BankAlert.TRANSFER_MUST_BE_AT_LEAST_001.getMessage());

        //проверка через api
        GetCustomerAccountsResponse firstUserAccount = SessionStorage.getSessionData(1).getSteps().getAccounts()
                .stream().findFirst().orElseThrow();

        softly.assertThat(firstUserAccount.getBalance()).isEqualTo(UserBalanceDefaults.MAX_DEPOSIT_AMOUNT.getAmount());
        softly.assertThat(firstUserAccount.getTransactions()).hasSize(1);


        GetCustomerAccountsResponse secondUserAccount = SessionStorage.getSessionData(2).getSteps().getAccounts()
                .stream().findFirst().orElseThrow();

        softly.assertThat(secondUserAccount.getBalance()).isEqualTo(UserBalanceDefaults.INITIAL_BALANCE.getAmount());
        softly.assertThat(secondUserAccount.getTransactions().size()).isZero();
    }
}
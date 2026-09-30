package iteration2.ui;

import api.generators.RandomData;
import api.models.CreateAccountResponse;
import api.models.CreateUserRequest;
import api.models.GetCustomerAccountsResponse;
import api.models.UserBalanceDefaults;
import api.requests.steps.AdminSteps;
import api.requests.steps.UserSteps;
import org.junit.jupiter.api.Test;
import ui.BankAlert;
import ui.UserDashboard;
import ui.UserTransferPage;

import static org.assertj.core.api.Assertions.within;

public class TransferMoneyTest extends BaseUiTest {

    @Test
    public void userCanTransferValidAmount() {
        CreateUserRequest firstUser = AdminSteps.createUserReturnRequest();

        CreateAccountResponse firstUserAccountResponse = UserSteps.createAccount(firstUser.getUsername(), firstUser.getPassword());

        UserSteps.depositMaxDepositAmount(firstUserAccountResponse.getId(), firstUser.getUsername(), firstUser.getPassword());

        CreateUserRequest secondUser = AdminSteps.createUserReturnRequest();

        CreateAccountResponse secondUserAccountResponse = UserSteps.createAccount(secondUser.getUsername(), secondUser.getPassword());

        authAsUser(firstUser);

        float amount = RandomData.getRandomPositiveFloat();

        new UserDashboard().open().openTransferPage()
                .getPage(UserTransferPage.class).open().makeTransfer(firstUserAccountResponse.getAccountNumber(),
                        secondUserAccountResponse.getAccountNumber(), String.valueOf(amount))
                .checkAlertMessageAndAccept(BankAlert.SUCCESSFULLY_TRANSFERRED.getMessage(), secondUserAccountResponse.getAccountNumber());

        //проверка через api
        GetCustomerAccountsResponse firstUserAccount = UserSteps.getAccounts(firstUser.getUsername(), firstUser.getPassword())
                .stream().findFirst().orElseThrow();

        softly.assertThat(firstUserAccount.getBalance()).isCloseTo(
                UserBalanceDefaults.MAX_DEPOSIT_AMOUNT.getAmount() - amount, within(0.001f));
        softly.assertThat(firstUserAccount.getTransactions()).hasSize(2);


        GetCustomerAccountsResponse secondUserAccount = UserSteps.getAccounts(secondUser.getUsername(), secondUser.getPassword())
                .stream().findFirst().orElseThrow();

        softly.assertThat(secondUserAccount.getBalance()).isEqualTo(amount);
        softly.assertThat(secondUserAccount.getTransactions()).hasSize(1);
    }

    @Test
    public void userCannotTransferInvalidAmount() {
        CreateUserRequest firstUser = AdminSteps.createUserReturnRequest();

        CreateAccountResponse createFirstUserAccountResponse = UserSteps.createAccount(firstUser.getUsername(), firstUser.getPassword());

        UserSteps.depositMaxDepositAmount(createFirstUserAccountResponse.getId(), firstUser.getUsername(), firstUser.getPassword());

        CreateUserRequest secondUser = AdminSteps.createUserReturnRequest();

        CreateAccountResponse createSecondUserAccountResponse = UserSteps.createAccount(secondUser.getUsername(), secondUser.getPassword());

        authAsUser(firstUser);

        new UserDashboard().open().openTransferPage()
                .getPage(UserTransferPage.class).makeTransfer(createFirstUserAccountResponse.getAccountNumber(),
                        createSecondUserAccountResponse.getAccountNumber(), String.valueOf(RandomData.getRandomNegativeFloat()))
                .checkAlertMessageAndAccept(BankAlert.TRANSFER_MUST_BE_AT_LEAST_001.getMessage());

        //проверка через api
        GetCustomerAccountsResponse firstUserAccount = UserSteps.getAccounts(firstUser.getUsername(), firstUser.getPassword())
                .stream().findFirst().orElseThrow();

        softly.assertThat(firstUserAccount.getBalance()).isEqualTo(UserBalanceDefaults.MAX_DEPOSIT_AMOUNT.getAmount());
        softly.assertThat(firstUserAccount.getTransactions()).hasSize(1);


        GetCustomerAccountsResponse secondUserAccount = UserSteps.getAccounts(secondUser.getUsername(), secondUser.getPassword())
                .stream().findFirst().orElseThrow();

        softly.assertThat(secondUserAccount.getBalance()).isEqualTo(UserBalanceDefaults.INITIAL_BALANCE.getAmount());
        softly.assertThat(secondUserAccount.getTransactions().size()).isZero();
    }
}

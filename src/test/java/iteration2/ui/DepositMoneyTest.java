package iteration2.ui;

import api.generators.RandomData;
import api.models.CreateAccountResponse;
import api.models.CreateUserRequest;
import api.models.GetCustomerAccountsResponse;
import api.requests.steps.AdminSteps;
import api.requests.steps.UserSteps;
import org.junit.jupiter.api.Test;
import ui.BankAlert;
import ui.UserDashboard;
import ui.UserDepositPage;

public class DepositMoneyTest extends BaseUiTest {

    @Test
    public void userCanDepositValidAmount() {
        CreateUserRequest user = AdminSteps.createUserReturnRequest();

        CreateAccountResponse createAccountResponse = UserSteps.createAccount(user.getUsername(), user.getPassword());

        authAsUser(user);

        float amount = RandomData.getRandomPositiveFloat();

        new UserDashboard().open().openDepositPage()
                .getPage(UserDepositPage.class).depositMoney(createAccountResponse.getAccountNumber(), String.valueOf(amount))
                .checkAlertMessageAndAccept(BankAlert.SUCCESSFULLY_DEPOSITED.getMessage(),
                        createAccountResponse.getAccountNumber());

        //проверка через api
        GetCustomerAccountsResponse account = UserSteps.getAccounts(user.getUsername(), user.getPassword())
                .stream().findFirst()
                .orElseThrow();

        softly.assertThat(account.getBalance()).isEqualTo(amount);
        softly.assertThat(account.getTransactions()).isNotEmpty();
    }

    @Test
    public void userCannotDepositInvalidAmount() {
        CreateUserRequest user = AdminSteps.createUserReturnRequest();

        CreateAccountResponse createAccountResponse = UserSteps.createAccount(user.getUsername(), user.getPassword());

        authAsUser(user);

        new UserDashboard().open().openDepositPage()
                .getPage(UserDepositPage.class).depositMoney(createAccountResponse.getAccountNumber(),
                        String.valueOf(RandomData.getRandomNegativeFloat()))
                .checkAlertMessageAndAccept(BankAlert.PLEASE_ENTER_VALID_AMOUNT.getMessage());

        //проверка через api
        GetCustomerAccountsResponse account = UserSteps.getAccounts(user.getUsername(), user.getPassword())
                .stream().findFirst().orElseThrow();

        softly.assertThat(account.getBalance()).isZero();
        softly.assertThat(account.getTransactions()).isEmpty();
    }
}

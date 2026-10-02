package iteration2.ui;

import api.generators.RandomData;
import api.models.CreateAccountResponse;
import api.models.CreateUserRequest;
import api.models.GetCustomerAccountsResponse;
import api.models.UserBalanceDefaults;
import api.requests.steps.AdminSteps;
import api.requests.steps.UserSteps;
import common.annotations.Account;
import common.annotations.Deposit;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import org.junit.jupiter.api.Test;
import ui.BankAlert;
import ui.UserDashboard;
import ui.UserTransferPage;

import static org.assertj.core.api.Assertions.within;

public class TransferMoneyTest extends BaseUiTest {
//    @Test
//    public void userCanTransferValidAmount1() {
//        CreateUserRequest firstUser = AdminSteps.createUserReturnRequest();
//
//        CreateAccountResponse firstUserAccountResponse = UserSteps.createAccount(firstUser.getUsername(), firstUser.getPassword());
//
//        UserSteps.depositMaxDepositAmount(firstUserAccountResponse.getId(), firstUser.getUsername(), firstUser.getPassword());
//
//        CreateUserRequest secondUser = AdminSteps.createUserReturnRequest();
//
//        CreateAccountResponse secondUserAccountResponse = UserSteps.createAccount(secondUser.getUsername(), secondUser.getPassword());
//
//        authAsUser(firstUser);
//
//        float amount = RandomData.getRandomPositiveFloat();
//
//        new UserDashboard().open().openTransferPage()
//                .getPage(UserTransferPage.class).open().makeTransfer(firstUserAccountResponse.getAccountNumber(),
//                        secondUserAccountResponse.getAccountNumber(), String.valueOf(amount))
//                .checkAlertMessageAndAccept(BankAlert.SUCCESSFULLY_TRANSFERRED.getMessage(), secondUserAccountResponse.getAccountNumber());
//
//        //проверка через api
//        GetCustomerAccountsResponse firstUserAccount = UserSteps.getAccounts(firstUser.getUsername(), firstUser.getPassword())
//                .stream().findFirst().orElseThrow();
//
//        softly.assertThat(firstUserAccount.getBalance()).isCloseTo(
//                UserBalanceDefaults.MAX_DEPOSIT_AMOUNT.getAmount() - amount, within(0.001f));
//        softly.assertThat(firstUserAccount.getTransactions()).hasSize(2);
//
//
//        GetCustomerAccountsResponse secondUserAccount = UserSteps.getAccounts(secondUser.getUsername(), secondUser.getPassword())
//                .stream().findFirst().orElseThrow();
//
//        softly.assertThat(secondUserAccount.getBalance()).isEqualTo(amount);
//        softly.assertThat(secondUserAccount.getTransactions()).hasSize(1);
//    }

    @Test
    @UserSession(2)
    @Account()
    @Deposit(user = 1, amount = 5000f) //сюда не могу передать енум, нужно compile-time constant, вынести в переменную класса?
    public void userCanTransferValidAmount() {
//        CreateUserRequest firstUser = AdminSteps.createUserReturnRequest();

//        CreateAccountResponse firstUserAccountResponse = UserSteps.createAccount(firstUser.getUsername(), firstUser.getPassword());

//        UserSteps.depositMaxDepositAmount(firstUserAccountResponse.getId(), firstUser.getUsername(), firstUser.getPassword());

//        CreateUserRequest secondUser = AdminSteps.createUserReturnRequest();

//        CreateAccountResponse secondUserAccountResponse = UserSteps.createAccount(secondUser.getUsername(), secondUser.getPassword());

//        authAsUser(firstUser);

        float amount = RandomData.getRandomPositiveFloat();

        String firstUserAccountNum = SessionStorage.getAccount(1).getAccountNumber(); //или лучше возвращать весь accountResponse и дальше вычленять accountNumber?
        String secondUserAccountNum = SessionStorage.getAccount(2, 1).getAccountNumber();

        new UserDashboard().open().openTransferPage().getPage(UserTransferPage.class).open().makeTransfer(firstUserAccountNum,
                        secondUserAccountNum, String.valueOf(amount))
                .checkAlertMessageAndAccept(BankAlert.SUCCESSFULLY_TRANSFERRED.getMessage(), secondUserAccountNum);

        //проверка через api
        GetCustomerAccountsResponse firstUserAccount = SessionStorage.getSession(1).getSteps().getAccounts()
                .stream().findFirst().orElseThrow();

        softly.assertThat(firstUserAccount.getBalance()).isCloseTo(
                UserBalanceDefaults.MAX_DEPOSIT_AMOUNT.getAmount() - amount, within(0.001f));
        softly.assertThat(firstUserAccount.getTransactions()).hasSize(2);


        GetCustomerAccountsResponse secondUserAccount = SessionStorage.getSession(2).getSteps().getAccounts()
                .stream().findFirst().orElseThrow();

        softly.assertThat(secondUserAccount.getBalance()).isEqualTo(amount);
        softly.assertThat(secondUserAccount.getTransactions()).hasSize(1);
    }

    @Test
    @UserSession(value = 2, auth = 1)
    @Account
    @Deposit(user = 1, amount = 5000f)
    public void userCannotTransferInvalidAmount() {
//        CreateUserRequest firstUser = AdminSteps.createUserReturnRequest();

//        CreateAccountResponse createFirstUserAccountResponse = UserSteps.createAccount(firstUser.getUsername(), firstUser.getPassword());

//        UserSteps.depositMaxDepositAmount(createFirstUserAccountResponse.getId(), firstUser.getUsername(), firstUser.getPassword());

//        CreateUserRequest secondUser = AdminSteps.createUserReturnRequest();

//        CreateAccountResponse createSecondUserAccountResponse = UserSteps.createAccount(secondUser.getUsername(), secondUser.getPassword());

//        authAsUser(firstUser);

        String firstUserAccountNumber = SessionStorage.getAccount(1, 1).getAccountNumber();
        String secondUserAccountNumber = SessionStorage.getAccount(2, 1).getAccountNumber();

        new UserDashboard().open().openTransferPage()
                .getPage(UserTransferPage.class).makeTransfer(firstUserAccountNumber,
                        secondUserAccountNumber, String.valueOf(RandomData.getRandomNegativeFloat()))
                .checkAlertMessageAndAccept(BankAlert.TRANSFER_MUST_BE_AT_LEAST_001.getMessage());

        //проверка через api
        GetCustomerAccountsResponse firstUserAccount = SessionStorage.getSession(1).getSteps().getAccounts()
                .stream().findFirst().orElseThrow();

        softly.assertThat(firstUserAccount.getBalance()).isEqualTo(UserBalanceDefaults.MAX_DEPOSIT_AMOUNT.getAmount());
        softly.assertThat(firstUserAccount.getTransactions()).hasSize(1);


        GetCustomerAccountsResponse secondUserAccount = SessionStorage.getSession(2).getSteps().getAccounts()
                .stream().findFirst().orElseThrow();

        softly.assertThat(secondUserAccount.getBalance()).isEqualTo(UserBalanceDefaults.INITIAL_BALANCE.getAmount());
        softly.assertThat(secondUserAccount.getTransactions().size()).isZero();
    }
}
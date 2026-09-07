package requests.skelethon;

import lombok.AllArgsConstructor;
import lombok.Getter;
import models.*;

@AllArgsConstructor
@Getter
public enum Endpoint {
    ADMIN_USERS(
            "/admin/users",
            CreateUserRequest.class,
            CreateUserResponse.class
    ),
    AUTH_LOGIN(
            "/auth/login",
            LoginUserRequest.class,
            LoginUserResponse.class
    ),
    ACCOUNTS(
            "/accounts",
            BaseModel.class,
            CreateAccountResponse.class
    ),
    ACCOUNTS_DEPOSIT(
            "/accounts/deposit",
            DepositMoneyRequest.class,
            DepositMoneyResponse.class
    ),
    //список
    CUSTOMER_ACCOUNTS(
            "/customer/accounts",
            BaseModel.class,
            GetCustomerAccountsResponse.class
    ),
    ACCOUNT_TRANSACTIONS(
            "/accounts/{accountId}/transactions",
            BaseModel.class,
            GetTransactionResponse.class
    );

    private final String url;
    private final Class<? extends BaseModel> requestModel;
    private final Class<? extends BaseModel> responseModel;
}

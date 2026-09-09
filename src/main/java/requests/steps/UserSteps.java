package requests.steps;

import io.restassured.common.mapper.TypeRef;
import models.*;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.ValidatedCrudRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;


import java.util.List;

public class UserSteps {
    public static CreateAccountResponse createAccount(String username, String password) {
        return new ValidatedCrudRequester<CreateAccountResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated())
                .post();
    }

    public static DepositMoneyResponse depositMoney(String username, String password, DepositMoneyRequest depositMoneyRequest) {
        return new ValidatedCrudRequester<DepositMoneyResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsOK())
                .post(depositMoneyRequest);
    }

    public static DepositMoneyResponse depositMaxDepositAmount(long accountId, String username, String password) {
        DepositMoneyRequest depositMoneyRequest = DepositMoneyRequest.builder()
                .id(accountId)
                .balance(UserBalanceDefaults.MAX_DEPOSIT_AMOUNT.getAmount())
                .build();

        return new ValidatedCrudRequester<DepositMoneyResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsOK())
                .post(depositMoneyRequest);
    }

    public static TransferMoneyResponse transferMoney(String username, String password, TransferMoneyRequest transferMoneyRequest) {
        return new ValidatedCrudRequester<TransferMoneyResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.requestReturnsOK())
                .post(transferMoneyRequest);
    }

    public static UpdateCustomerProfileResponse updateProfile(String username, String password, UpdateCustomerProfileRequest request) {
        return new ValidatedCrudRequester<UpdateCustomerProfileResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.UPDATE_CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsOK())
                .put(request);
    }

    public static GetCustomerProfileResponse getProfile(String username, String password) {
        return new ValidatedCrudRequester<GetCustomerProfileResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.GET_CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsOK())
                .get();
    }

    public static List<GetCustomerAccountsResponse> getAccounts(String username, String password) {
        return new ValidatedCrudRequester<GetCustomerAccountsResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK())
                .getList(new TypeRef<List<GetCustomerAccountsResponse>>() {});
    }

    public static List<GetTransactionResponse> getTransactions(String username, String password, long id) {
        return new ValidatedCrudRequester<GetTransactionResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.ACCOUNT_TRANSACTIONS,
                ResponseSpecs.requestReturnsOK())
                .getListById(id, new TypeRef<List<GetTransactionResponse>>() {});
    }
}

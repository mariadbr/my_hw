package api.requests.steps;

import api.models.*;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.ValidatedCrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import io.restassured.common.mapper.TypeRef;

import java.util.List;

public class UserStepsObj {
    private final String username;
    private final String password;

    public UserStepsObj(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public CreateAccountResponse createAccount() {
        return new ValidatedCrudRequester<CreateAccountResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated())
                .post();
    }

    public DepositMoneyResponse depositMoney(DepositMoneyRequest depositMoneyRequest) {
        return new ValidatedCrudRequester<DepositMoneyResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.ACCOUNTS_DEPOSIT,
                ResponseSpecs.requestReturnsOK())
                .post(depositMoneyRequest);
    }

    public DepositMoneyResponse depositMaxDepositAmount(long accountId) {
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

    public TransferMoneyResponse transferMoney(TransferMoneyRequest transferMoneyRequest) {
        return new ValidatedCrudRequester<TransferMoneyResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.ACCOUNTS_TRANSFER,
                ResponseSpecs.requestReturnsOK())
                .post(transferMoneyRequest);
    }

    public UpdateCustomerProfileResponse updateProfile(UpdateCustomerProfileRequest request) {
        return new ValidatedCrudRequester<UpdateCustomerProfileResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.UPDATE_CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsOK())
                .put(request);
    }

    public GetCustomerProfileResponse getProfile() {
        return new ValidatedCrudRequester<GetCustomerProfileResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.GET_CUSTOMER_PROFILE,
                ResponseSpecs.requestReturnsOK())
                .get();
    }

    public List<GetCustomerAccountsResponse> getAccounts() {
        return new ValidatedCrudRequester<GetCustomerAccountsResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.CUSTOMER_ACCOUNTS,
                ResponseSpecs.requestReturnsOK())
                .getList(new TypeRef<List<GetCustomerAccountsResponse>>() {});
    }

    public List<GetTransactionResponse> getTransactions(long id) {
        return new ValidatedCrudRequester<GetTransactionResponse>(
                RequestSpecs.authAsUser(username, password),
                Endpoint.ACCOUNT_TRANSACTIONS,
                ResponseSpecs.requestReturnsOK())
                .getListById(id, new TypeRef<List<GetTransactionResponse>>() {});
    }
}

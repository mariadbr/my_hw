package api.requests.steps;

import api.generators.RandomModelGenerator;
import api.models.*;
import io.restassured.common.mapper.TypeRef;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.ValidatedCrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import lombok.AllArgsConstructor;


import java.util.List;


@AllArgsConstructor
public class UserSteps {
    private String username;
    private String password;

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
        DepositMoneyRequest depositMoneyRequest = RandomModelGenerator.generate(DepositMoneyRequest.class);
        depositMoneyRequest.setId(accountId);
        depositMoneyRequest.setBalance(UserBalanceDefaults.MAX_DEPOSIT_AMOUNT.getAmount());

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

package common.storage;

import api.models.CreateAccountResponse;
import api.models.CreateUserRequest;
import api.requests.steps.UserStepsObj;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class UserSessionData {
    private final CreateUserRequest user;
    private final UserStepsObj steps;
    private final List<CreateAccountResponse> accounts = new ArrayList<>();

    public UserSessionData(CreateUserRequest user) {
        this.user = user;
        this.steps = new UserStepsObj(user.getUsername(), user.getPassword());
    }

    public void addAccount(CreateAccountResponse account) {
        accounts.add(account);
    }

    public CreateAccountResponse getAccount(int number) {
        return accounts.get(number - 1);
    }

    public CreateAccountResponse getAccount() {
        return getAccount(1);
    }
}
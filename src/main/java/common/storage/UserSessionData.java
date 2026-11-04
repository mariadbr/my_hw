package common.storage;

import api.models.CreateAccountResponse;
import api.models.CreateUserRequest;
import api.requests.steps.UserSteps;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class UserSessionData {
    private final CreateUserRequest user;
    private final UserSteps steps;
    private final List<CreateAccountResponse> accounts = new ArrayList<>();
    private final Long id;

    public UserSessionData(CreateUserRequest user, Long id) {
        this.user = user;
        this.steps = new UserSteps(user.getUsername(), user.getPassword());
        this.id = id;
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
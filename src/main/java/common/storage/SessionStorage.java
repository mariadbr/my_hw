package common.storage;

import api.models.CreateAccountResponse;
import api.models.CreateUserRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class SessionStorage {
    private static ThreadLocal<SessionStorage> INSTANCE = ThreadLocal.withInitial(SessionStorage::new);

    private LinkedHashMap<CreateUserRequest, UserSessionData> usersMap = new LinkedHashMap<>();

    private SessionStorage() {}

    public static void addUsers(List<CreateUserRequest> users ) {
        for (CreateUserRequest user : users) {
            INSTANCE.get().usersMap.put(user, new UserSessionData(user));
        }
    }

    /**
     * Возвращаем объект CreateUserRequest по его порядковому номеру в списке созданных пользователей.
     * @param number Порядковый номер, начиная с 1 (а не с 0).
     * @return Объект CreateUserRequest, соответствующий указанному порядковому номеру.
     */
    public static CreateUserRequest getUser(int number) {
        return new ArrayList<>(INSTANCE.get().usersMap.keySet()).get(number - 1);
    }

    public static CreateUserRequest  getUser() {
        return getUser(1);
    }

    public static UserSessionData getSessionData(int userNumber) {
        return new ArrayList<>(INSTANCE.get().usersMap.values()).get(userNumber - 1);
    }

    public static UserSessionData getSessionData() {
        return getSessionData(1);
    }

    public static void addAccount(int userNumber, CreateAccountResponse account) {
        getSessionData(userNumber).addAccount(account);
    }

    public static CreateAccountResponse getAccount(int userNumber, int accountNumber) {
        return getSessionData(userNumber).getAccount(accountNumber);
    }

    public static CreateAccountResponse getAccount() {
        return getAccount(1, 1);
    }

    public static void clear() {
        INSTANCE.get().usersMap.clear();
    }

    public static int getSessionCount() {
        return INSTANCE.get().usersMap.size();
    }

}

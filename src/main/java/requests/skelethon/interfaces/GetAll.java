package requests.skelethon.interfaces;

import io.restassured.response.ValidatableResponse;

public interface GetAll {
    ValidatableResponse getList();
    ValidatableResponse getListById(long id);
}

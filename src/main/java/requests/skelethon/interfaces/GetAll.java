package requests.skelethon.interfaces;

import io.restassured.response.ValidatableResponse;

public interface GetAll {
    //List<? extends BaseModel> getList();
    ValidatableResponse getList();
    ValidatableResponse getListById(long id);
}

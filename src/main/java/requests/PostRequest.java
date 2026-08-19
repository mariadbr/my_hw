package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.BaseModel;

public abstract class PostRequest<T extends BaseModel> extends Request {
    public PostRequest(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    //общий запрос, абстрактный чтобы каждый из детей был обязан имплементировать
    public abstract ValidatableResponse post(T model);
}

package requests.skelethon.requesters;

import io.restassured.common.mapper.TypeRef;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.BaseModel;
import requests.skelethon.Endpoint;
import requests.skelethon.HttpRequest;
import requests.skelethon.interfaces.CrudEndpointInterface;

import java.util.List;

import static io.restassured.RestAssured.given;

public class ValidatedCrudRequester<T extends BaseModel> extends HttpRequest implements CrudEndpointInterface{
    private CrudRequester crudRequester;

    public ValidatedCrudRequester(RequestSpecification requestSpecification, Endpoint endpoint, ResponseSpecification responseSpecification) {
        super(requestSpecification, endpoint, responseSpecification);
        this.crudRequester = new CrudRequester(requestSpecification, endpoint, responseSpecification);
    }

    @Override
    public T post(BaseModel model) {
        return (T) crudRequester.post(model).extract().as(endpoint.getResponseModel());
    }

    public T post() {
        return post(null);
    }

    @Override
    public T get(long id) {
        return null;
    }

    @Override
    public Object update(long id, BaseModel model) {
        return null;
    }

    @Override
    public Object delete(long id) {
        return null;
    }

//    @Override
//    public Object getList() {
//        return crudRequester.getList().extract().as(new TypeRef<List<GetCustomerAccountResponse>>() {}); //или нужен дженерик? как сделать его универсальным?
//
//    }

    public <R> List<R> getList(TypeRef<List<R>> typeRef) {
        return crudRequester
                .getList()
                .extract()
                .as(typeRef);
    }

    public <R> List<R> getListById(long id, TypeRef<List<R>> typeRef) {
        return crudRequester
                .getListById(id)
                .extract()
                .as(typeRef);
    }
}

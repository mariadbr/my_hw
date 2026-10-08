package api.requests.steps;

import api.generators.RandomModelGenerator;
import api.models.CreateUserRequest;
import api.models.CreateUserResponse;
import api.models.GetAllUsersResponse;
import api.requests.skelethon.Endpoint;
import api.requests.skelethon.requesters.CrudRequester;
import api.requests.skelethon.requesters.ValidatedCrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import io.restassured.common.mapper.TypeRef;

import java.util.List;

public class AdminSteps {
    public static CreateUserRequest createUserReturnRequest() {
        CreateUserRequest createUserRequest =
                RandomModelGenerator.generate(CreateUserRequest.class);

         new CrudRequester(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USERS,
                ResponseSpecs.entityWasCreated())
                .post(createUserRequest);

         return createUserRequest;
    }

    public static CreateUserResponse createUserReturnResponse(CreateUserRequest createUserRequest) {

        return new ValidatedCrudRequester<CreateUserResponse>(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USERS,
                ResponseSpecs.entityWasCreated())
                .post(createUserRequest);
    }

    public static List<GetAllUsersResponse> getAllUsers() {
        return new ValidatedCrudRequester<GetAllUsersResponse>(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USERS,
                ResponseSpecs.requestReturnsOK())
                .getList(new TypeRef<List<GetAllUsersResponse>>() {});
    }

    public static void deleteById(long id) {
        new CrudRequester(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USERS_ID,
                ResponseSpecs.requestReturnsOK())
                .delete(id);
    }
}

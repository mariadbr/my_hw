package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import static io.restassured.RestAssured.given;

public class GetAccountTransactionsRequester extends GetRequest{
    private final long accountId;

    public GetAccountTransactionsRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification, long accountId) {
        super(requestSpecification, responseSpecification);
        this.accountId = accountId;
    }

    @Override
    public ValidatableResponse get() {
        return  given()
                .spec(requestSpecification)
                .pathParam("accountId", accountId)
                .get("/api/v1/accounts/{accountId}/transactions")
                .then()
                .assertThat()
                .spec(responseSpecification);
    }
}

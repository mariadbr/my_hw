package specs;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;

public class ResponseSpecs {
    private ResponseSpecs(){}

    private static ResponseSpecBuilder defaultResponseBuilder() {
        return new ResponseSpecBuilder();
    }

    public static ResponseSpecification entityWasCreated() {
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_CREATED)
                .build();
    }

    public static ResponseSpecification requestReturnsOK() {
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_OK)
                .build();
    }

    //все ошибки формата 400 в программе одинаковые, иметь errorKey, errorValue -> часть тестрирвоания дизайна api
    public static ResponseSpecification requestReturnsBadRequest(String errorKey, String errorValue) {
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                .expectBody(errorKey, Matchers.hasItem(errorValue))
                //или можно:
                //.body(errorKey, Matchers.equalTo(List.of(errorValue)));
                .build();
    }

    public static ResponseSpecification requestReturnsBadRequestWithText(String errorValue) {
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                .expectBody(Matchers.equalTo(errorValue))
                .build();
    }

    public static ResponseSpecification requestReturnsForbidden(String errorValue) {
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_FORBIDDEN)
                .expectBody(Matchers.equalTo(errorValue))
                .build();
    }

}

package uit.carbon_shop.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import uit.carbon_shop.config.BaseIT;

@Sql({"/data/clearAll.sql", "/data/companyData.sql", "/data/appUserData.sql"})
public class AppUserResourceTest extends BaseIT {

    private static String mediatorAccessToken = "";

    @Test
    @Order(1)
    void mediatorAuthenticate() {
        Response post = RestAssured
                .given()
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(readResource("/requests/mediator_authenticate.json"))
                .when()
                    .post("/authenticate");
        post.then()
                .statusCode(HttpStatus.OK.value())
                .body("accessToken", Matchers.notNullValue());

        mediatorAccessToken = post.jsonPath().getString("accessToken");
    }

    @Test
    void getAllAppUsers_filtered() {
        RestAssured
                .given()
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/appUsers?filter=1301")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("page.totalElements", Matchers.equalTo(1))
                    .body("content.get(0).userId", Matchers.equalTo(1301));
    }

    @Test
    void getAppUser_success() {
        RestAssured
                .given()
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/appUsers/1300")
                .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("name", Matchers.equalTo("Sed diam voluptua."));
    }

    @Test
    void getAppUser_notFound() {
        RestAssured
                .given()
                    .accept(ContentType.JSON)
                .when()
                    .get("/api/appUsers/1966")
                .then()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .body("code", Matchers.equalTo("NOT_FOUND"));
    }

    @Test
    void createAppUser_success() {
        RestAssured
                .given()
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(readResource("/requests/mediator_authenticate.json"))
                .when()
                    .post("/api/appUsers")
                .then()
                    .statusCode(HttpStatus.CREATED.value());
        assertEquals(3, appUserRepository.count());
    }

    @Test
    void createAppUser_missingField() {
        RestAssured
                .given()
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(readResource("/requests/appUserDTORequest_missingField.json"))
                .when()
                    .post("/api/appUsers")
                .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("code", Matchers.equalTo("VALIDATION_FAILED"))
                    .body("fieldErrors.get(0).property", Matchers.equalTo("password"))
                    .body("fieldErrors.get(0).code", Matchers.equalTo("REQUIRED_NOT_NULL"));
    }

    @Test
    void updateAppUser_success() {
        RestAssured
                .given()
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(readResource("/requests/mediator_authenticate.json"))
                .when()
                    .put("/api/appUsers/1300")
                .then()
                    .statusCode(HttpStatus.OK.value());
        assertEquals("Duis autem vel.", appUserRepository.findById(((long)1300)).orElseThrow().getName());
        assertEquals(2, appUserRepository.count());
    }

    @Test
    void deleteAppUser_success() {
        RestAssured
                .given()
                    .accept(ContentType.JSON)
                .when()
                    .delete("/api/appUsers/1300")
                .then()
                    .statusCode(HttpStatus.NO_CONTENT.value());
        assertEquals(1, appUserRepository.count());
    }

}

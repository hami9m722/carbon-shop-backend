package uit.carbon_shop.rest;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import uit.carbon_shop.config.BaseIntegrationTestByEmbeddedInfrastructure;
import uit.carbon_shop.model.UserStatus;
import uit.carbon_shop.repos.AppUserRepository;

@TestMethodOrder(OrderAnnotation.class)
@Sql(value = {"/data/companyData.sql", "/data/appUserData.sql"}, executionPhase = ExecutionPhase.BEFORE_TEST_CLASS)
@Tag("Embedded")
public class EmbeddedInfrastructureTest extends BaseIntegrationTestByEmbeddedInfrastructure {

    private static String mediatorAccessToken = "";

    @Autowired
    AppUserRepository appUserRepository;

    @Test
    @Order(1)
    void registerNewUser() {
        RestAssured
                .given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(readResource("/requests/registerNewUser.json"))
                .when()
                .post("/register")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    @Order(2)
    void newUserLogin_whenNotApproved() {
        Response post = RestAssured
                .given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(readResource("/requests/newUserAuthenticateWhenNotApproved.json"))
                .when()
                .post("/authenticate");
        post.then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("accessToken", Matchers.nullValue());
    }

    @Test
    @Order(3)
    void mediatorAuthenticate() {
        Response post = RestAssured
                .given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(readResource("/requests/mediatorAuthenticate.json"))
                .when()
                .post("/authenticate");
        post.then()
                .statusCode(HttpStatus.OK.value())
                .body("accessToken", Matchers.notNullValue());

        mediatorAccessToken = post.jsonPath().getString("accessToken");
    }

    @Test
    @Order(4)
    void mediatorApproveNewUser() {
        Long notApprovedUserId = appUserRepository.findByStatus(UserStatus.INIT, Pageable.unpaged())
                .stream()
                .findAny()
                .orElseThrow()
                .getId();
        RestAssured
                .given()
                .accept(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + mediatorAccessToken)
                .when()
                .patch("/api/mediator/audit/user/{userId}/approve", notApprovedUserId)
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @Test
    @Order(5)
    void newUserLogin_whenApproved() {
        RestAssured
                .given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(readResource("/requests/newUserAuthenticateWhenNotApproved.json"))
                .when()
                .post("/authenticate")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("accessToken", Matchers.notNullValue());
    }

}

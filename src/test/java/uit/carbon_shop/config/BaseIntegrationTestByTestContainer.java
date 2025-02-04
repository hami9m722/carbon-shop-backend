package uit.carbon_shop.config;

import io.restassured.RestAssured;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.util.StreamUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import uit.carbon_shop.CarbonShopApplication;


/**
 * Abstract base class to be extended by every IT test. Starts the Spring Boot context with a Datasource connected to
 * the Testcontainers Docker instance. The instance is reused for all tests, with all data wiped out before each test.
 */
@SpringBootTest(
        classes = CarbonShopApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("it")
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
public abstract class BaseIntegrationTestByTestContainer {

    @ServiceConnection
    private static final PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:17.2");
    private static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:6.2");
    private static final GenericContainer<?> mailpitContainer = new GenericContainer<>("axllent/mailpit:v1.21");
    public static String smtpHost;
    public static Integer smtpPort;
    public static String messagesUrl;

    static {
        postgreSQLContainer.withReuse(true)
                .start();
        mailpitContainer.withExposedPorts(1025, 8025)
                .waitingFor(Wait.forLogMessage(".*accessible via.*", 1))
                .withReuse(true)
                .start();
        smtpHost = mailpitContainer.getHost();
        smtpPort = mailpitContainer.getMappedPort(1025);
        messagesUrl = "http://" + smtpHost + ":" + mailpitContainer.getMappedPort(8025) + "/api/v1/messages";

        redisContainer.withReuse(true)
                .withExposedPorts(6379)
                .start();
    }

    @LocalServerPort
    public int serverPort;

    @PostConstruct
    public void initRestAssured() {
        RestAssured.port = serverPort;
        RestAssured.urlEncodingEnabled = false;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @DynamicPropertySource
    public static void setDynamicProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", () -> smtpHost);
        registry.add("spring.mail.port", () -> smtpPort);
        registry.add("spring.mail.properties.mail.smtp.auth", () -> false);
        registry.add("spring.mail.properties.mail.smtp.starttls.enable", () -> false);
        registry.add("spring.mail.properties.mail.smtp.starttls.required", () -> false);

        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);
    }

    @SneakyThrows
    public String readResource(final String resourceName) {
        return StreamUtils.copyToString(getClass().getResourceAsStream(resourceName), StandardCharsets.UTF_8);
    }

}

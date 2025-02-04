package uit.carbon_shop.config;

import io.restassured.RestAssured;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.util.StreamUtils;
import redis.embedded.RedisServer;
import uit.carbon_shop.CarbonShopApplication;

@SpringBootTest(
        classes = CarbonShopApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("it")
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
public abstract class BaseIntegrationTestByEmbeddedInfrastructure {

    private static int redisPort = 6500;

    static {
        // Find a free port
        try {
            ServerSocket serverSocket = new ServerSocket(0);
            redisPort = serverSocket.getLocalPort();
            serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        RedisServer redisServer = RedisServer.builder()
                .port(redisPort)
                .setting("maxmemory 128M")
                .build();
        redisServer.start();
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
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:carbon-shop");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "admin");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> redisPort);
    }

    @SneakyThrows
    public String readResource(final String resourceName) {
        return StreamUtils.copyToString(getClass().getResourceAsStream(resourceName), StandardCharsets.UTF_8);
    }

}

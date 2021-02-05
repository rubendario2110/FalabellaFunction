package helloworld;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.SessionFactory;
import okhttp3.*;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import static org.hibernate.cfg.AvailableSettings.*;
import static org.hibernate.cfg.AvailableSettings.PASS;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private SessionFactory sessionFactory = createSessionFactory();

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        try {
            ConsumerServices service = new ConsumerServices(sessionFactory, new ConsumerDao());
            return routeRequest(input, service);
        } finally {
            flushConnectionPool();
        }
    }

    private APIGatewayProxyResponseEvent routeRequest(APIGatewayProxyRequestEvent input, ConsumerServices service) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);
        try {
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, input.getBody());
            Request request = new Request.Builder()
                    .url("https://api-staging.segurosfalabella.com/api-datos-dinamicos-sarlaft/getDataSarlaft")
                    .method("POST", body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", input.getHeaders().get("Authorization"))
                    .build();
            Response result = client.newCall(request).execute();
            Object saveResponse = service.saveResponse(fromJson(result.body().string(), Consumer.class));
            return response
                    .withStatusCode(200)
                    .withBody(toJson(saveResponse));
        } catch (IOException e) {
            return response.withStatusCode(500).withBody("{}");
        }

    }

    private static <T> String toJson(T object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> T fromJson(String json, Class<T> type) {
        try {
            return OBJECT_MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static SessionFactory createSessionFactory() {
        Map<String, String> settings = new HashMap<>();
        settings.put(URL, System.getenv("DB_URL"));
        settings.put(DIALECT, "org.hibernate.dialect.MySQL5Dialect");
        settings.put(DEFAULT_SCHEMA, "falabella");
        settings.put(DRIVER, "com.mysql.cj.jdbc.Driver");
        settings.put(USER, System.getenv("DB_USER"));
        settings.put(PASS, System.getenv("DB_PASSWORD"));
        settings.put("hibernate.hikari.connectionTimeout", "20000");
        settings.put("hibernate.hikari.minimumIdle", "1");
        settings.put("hibernate.hikari.maximumPoolSize", "2");
        settings.put("hibernate.hikari.idleTimeout", "30000");

        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySettings(settings)
                .build();

        return new MetadataSources(registry)
                .addAnnotatedClass(Consumer.class)
                .buildMetadata()
                .buildSessionFactory();
    }

    private void flushConnectionPool() {
        ConnectionProvider connectionProvider = sessionFactory.getSessionFactoryOptions()
                .getServiceRegistry()
                .getService(ConnectionProvider.class);
        HikariDataSource hikariDataSource = connectionProvider.unwrap(HikariDataSource.class);
        hikariDataSource.getHikariPoolMXBean().softEvictConnections();
    }
}



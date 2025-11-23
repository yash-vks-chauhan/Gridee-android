package com.parking.app.integration;

import com.parking.app.constants.Role;
import com.parking.app.model.Users;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Base class for all integration tests
 * Provides common setup for REST Assured and test configuration
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = {
        "spring.profiles.active=local",
        "server.port=8443",
        "app.locking.provider=mongodb",
        "logging.level.com.parking.app=INFO",
        "spring.data.mongodb.uri=mongodb://localhost:27017/parkingdbtest"  // Use test database
    }
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

    protected static final Logger logger = LoggerFactory.getLogger(BaseIntegrationTest.class);

    protected static final String BASE_URL = "https://localhost:8443";
    protected static final String API_PREFIX = "/api";

    // Admin credentials for tests
    protected static final String TEST_ADMIN_EMAIL = "test-admin@parking.com";
    protected static final String TEST_ADMIN_PASSWORD = "TestAdmin@123";
    protected static final String TEST_ADMIN_NAME = "Test Admin";

    // API Endpoints
    protected static final String AUTH_REGISTER = API_PREFIX + "/auth/register";
    protected static final String AUTH_LOGIN = API_PREFIX + "/auth/login";
    protected static final String PARKING_LOTS = API_PREFIX + "/parking-lots";
    protected static final String PARKING_SPOTS = API_PREFIX + "/parking-spots";
    protected static final String BOOKINGS = API_PREFIX + "/bookings";

    protected RequestSpecification requestSpec;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeAll
    void setupRestAssured() {
        logger.info("═══════════════════════════════════════════════════════════");
        logger.info("🧹 Starting Test Database Cleanup");
        logger.info("═══════════════════════════════════════════════���═══════════");

        // Clean up all collections before tests
        cleanupDatabase();

        // Create admin user for tests
        createTestAdminUser();

        RestAssured.baseURI = BASE_URL;
        RestAssured.useRelaxedHTTPSValidation(); // For self-signed certificates

        requestSpec = new RequestSpecBuilder()
            .setBaseUri(BASE_URL)
            .setContentType(ContentType.JSON)
            .addFilter(new AllureRestAssured())
            .addFilter(new RequestLoggingFilter())
            .addFilter(new ResponseLoggingFilter())
            .build();

        logger.info("🚀 Integration Test Setup Complete - Base URL: {}", BASE_URL);
        logger.info("📝 Test will call REAL APIs with REAL authentication");
        logger.info("🗄️  Using test database: parkingdbtest");
        logger.info("═══════════════════════════════════════════════════════════\n");
    }

    /**
     * Clean up all test data from MongoDB collections
     */
    private void cleanupDatabase() {
        try {
            logger.info("🧹 Cleaning up MongoDB collections...");

            // Delete all documents from each collection
            long usersDeleted = mongoTemplate.getCollection("users").deleteMany(new org.bson.Document()).getDeletedCount();
            long lotsDeleted = mongoTemplate.getCollection("parking_lots").deleteMany(new org.bson.Document()).getDeletedCount();
            long spotsDeleted = mongoTemplate.getCollection("parking_spots").deleteMany(new org.bson.Document()).getDeletedCount();
            long bookingsDeleted = mongoTemplate.getCollection("bookings").deleteMany(new org.bson.Document()).getDeletedCount();
            long walletsDeleted = mongoTemplate.getCollection("wallets").deleteMany(new org.bson.Document()).getDeletedCount();
            long transactionsDeleted = mongoTemplate.getCollection("transactions").deleteMany(new org.bson.Document()).getDeletedCount();

            logger.info("   ✅ Deleted {} users", usersDeleted);
            logger.info("   ✅ Deleted {} parking lots", lotsDeleted);
            logger.info("   ✅ Deleted {} parking spots", spotsDeleted);
            logger.info("   ✅ Deleted {} bookings", bookingsDeleted);
            logger.info("   ✅ Deleted {} wallets", walletsDeleted);
            logger.info("   ✅ Deleted {} transactions", transactionsDeleted);
            logger.info("✅ Database cleanup completed\n");

        } catch (Exception e) {
            logger.error("❌ Error during database cleanup: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to cleanup database", e);
        }
    }

    /**
     * Create admin user for integration tests
     */
    private void createTestAdminUser() {
        try {
            logger.info("👤 Creating test admin user...");

            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

            Users adminUser = new Users();
            adminUser.setName(TEST_ADMIN_NAME);
            adminUser.setEmail(TEST_ADMIN_EMAIL);
            adminUser.setPasswordHash(passwordEncoder.encode(TEST_ADMIN_PASSWORD));
            adminUser.setPhone("9999999999");
            adminUser.setRole(Role.ADMIN.name());
            adminUser.setActive(true);
            adminUser.setFirstUser(false);
            adminUser.setWalletCoins(0);

            mongoTemplate.save(adminUser);

            logger.info("   ✅ Admin user created successfully");
            logger.info("   📧 Email: {}", TEST_ADMIN_EMAIL);
            logger.info("   🔑 Password: {}", TEST_ADMIN_PASSWORD);
            logger.info("   👤 Name: {}", TEST_ADMIN_NAME);
            logger.info("✅ Admin user setup completed\n");

        } catch (Exception e) {
            logger.error("❌ Error creating test admin user: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create test admin user", e);
        }
    }

    /**
     * Create request specification with bearer token
     */
    protected RequestSpecification withAuth(String token) {
        return RestAssured.given(requestSpec)
            .header("Authorization", "Bearer " + token);
    }

    /**
     * Create request specification without authentication
     */
    protected RequestSpecification withoutAuth() {
        return RestAssured.given(requestSpec);
    }
}

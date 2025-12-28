package com.praktikum.database.testing.library.performance;

// Import classes
import com.github.javafaker.Faker;
import com.praktikum.database.testing.library.BaseDatabaseTest;
import com.praktikum.database.testing.library.config.DatabaseConfig;
import com.praktikum.database.testing.library.dao.BookDAO;
import com.praktikum.database.testing.library.dao.UserDAO;
import com.praktikum.database.testing.library.model.Book;
import com.praktikum.database.testing.library.model.User;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance Test Suite
 * Menguji kinerja database untuk operasi-operasi kritis
 * Mengukur response time, throughput, dan resource usage
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Database Performance Test Suite")
public class DatabasePerformanceTest extends BaseDatabaseTest {

    private static UserDAO userDAO;
    private static BookDAO bookDAO;
    private static Faker faker;
    private static List<Integer> createdUserIds;
    private static List<Integer> createdBookIds;

    @BeforeAll
    static void setUpAll() {
        logger.info("Starting Database Performance Tests");
        userDAO = new UserDAO();
        bookDAO = new BookDAO();
        faker = new Faker();
        createdUserIds = new ArrayList<>();
        createdBookIds = new ArrayList<>();
    }

    @AfterAll
    static void tearDownAll() {
        logger.info("Performance Tests Completed. Cleaning up...");
        // Cleanup data is handled by BaseDatabaseTest transaction rollback mechanism
        // or manual cleanup if needed
    }

    // ==========================================
    // BULK INSERT PERFORMANCE
    // ==========================================

    @Test
    @Order(1)
    @DisplayName("TC501: Bulk INSERT performance - 100 users (< 5 detik)")
    void testBulkInsertPerformance_Users() throws SQLException {
        int userCount = 100;
        long startTime = System.nanoTime();

        for (int i = 0; i < userCount; i++) {
            User user = createTestUser();
            User created = userDAO.create(user);
            createdUserIds.add(created.getUserId());
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        logger.info("Inserted " + userCount + " users in " + durationMs + "ms");
        logger.info("Average time per user: " + (durationMs / (double) userCount) + "ms");

        // Assert that 100 inserts take less than 5 seconds (5000 ms)
        assertThat(durationMs).isLessThan(5000);
    }

    @Test
    @Order(2)
    @DisplayName("TC502: Bulk INSERT performance - 100 books (< 5 detik)")
    void testBulkInsertPerformance_Books() throws SQLException {
        int bookCount = 100;
        long startTime = System.nanoTime();

        for (int i = 0; i < bookCount; i++) {
            Book book = createTestBook();
            Book created = bookDAO.create(book);
            createdBookIds.add(created.getBookId());
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        logger.info("Inserted " + bookCount + " books in " + durationMs + "ms");
        logger.info("Average time per book: " + (durationMs / (double) bookCount) + "ms");

        // Assert that 100 inserts take less than 5 seconds
        assertThat(durationMs).isLessThan(5000);
    }

    // ==========================================
    // QUERY PERFORMANCE
    // ==========================================

    @Test
    @Order(3)
    @DisplayName("TC503: SELECT ALL performance - Find all users (< 1 detik)")
    void testSelectAllPerformance_Users() throws SQLException {
        // Ensure we have data
        if (createdUserIds.isEmpty()) {
            testBulkInsertPerformance_Users();
        }

        long startTime = System.nanoTime();
        List<User> users = userDAO.findAll();
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        logger.info("Retrieved " + users.size() + " users in " + durationMs + "ms");

        assertThat(users).isNotEmpty();
        assertThat(durationMs).isLessThan(1000); // Should be very fast
    }

    @Test
    @Order(4)
    @DisplayName("TC504: SELECT ALL performance - Find all books (< 1 detik)")
    void testSelectAllPerformance_Books() throws SQLException {
        // Ensure we have data
        if (createdBookIds.isEmpty()) {
            testBulkInsertPerformance_Books();
        }

        long startTime = System.nanoTime();
        List<Book> books = bookDAO.findAll();
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        logger.info("Retrieved " + books.size() + " books in " + durationMs + "ms");

        assertThat(books).isNotEmpty();
        assertThat(durationMs).isLessThan(1000);
    }

    @Test
    @Order(5)
    @DisplayName("TC505: Individual SELECT performance - Find user by ID (< 100ms average)")
    void testIndividualSelectPerformance_UserById() throws SQLException {
        if (createdUserIds.isEmpty()) return;

        int iterations = 100;
        long totalTime = 0;
        Random random = new Random();

        for (int i = 0; i < iterations; i++) {
            Integer randomId = createdUserIds.get(random.nextInt(createdUserIds.size()));

            long start = System.nanoTime();
            userDAO.findById(randomId);
            long end = System.nanoTime();

            totalTime += (end - start);
        }

        long avgTimeMs = (totalTime / iterations) / 1_000_000;
        logger.info("Average User SELECT time: " + avgTimeMs + "ms over " + iterations + " iterations");

        assertThat(avgTimeMs).isLessThan(100);
    }

    @Test
    @Order(6)
    @DisplayName("TC506: Individual SELECT performance - Find book by ID (< 100ms average)")
    void testIndividualSelectPerformance_BookById() throws SQLException {
        if (createdBookIds.isEmpty()) return;

        int iterations = 100;
        long totalTime = 0;
        Random random = new Random();

        for (int i = 0; i < iterations; i++) {
            Integer randomId = createdBookIds.get(random.nextInt(createdBookIds.size()));

            long start = System.nanoTime();
            bookDAO.findById(randomId);
            long end = System.nanoTime();

            totalTime += (end - start);
        }

        long avgTimeMs = (totalTime / iterations) / 1_000_000;
        logger.info("Average Book SELECT time: " + avgTimeMs + "ms over " + iterations + " iterations");

        assertThat(avgTimeMs).isLessThan(100);
    }

    // ==========================================
    // UPDATE PERFORMANCE
    // ==========================================

    @Test
    @Order(7)
    @DisplayName("TC507: Bulk UPDATE performance - Update 50 users")
    void testBulkUpdatePerformance_Users() throws SQLException {
        if (createdUserIds.size() < 50) return;

        int updateCount = 50;
        long startTime = System.nanoTime();

        for (int i = 0; i < updateCount; i++) {
            Integer userId = createdUserIds.get(i);
            User user = userDAO.findById(userId).get();
            user.setPhone(faker.phoneNumber().cellPhone());
            userDAO.update(user);
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        logger.info("Updated " + updateCount + " users in " + durationMs + "ms");
        assertThat(durationMs).isLessThan(3000); // 3 seconds budget
    }

    @Test
    @Order(8)
    @DisplayName("TC508: Bulk UPDATE performance - Update book copies")
    void testBulkUpdatePerformance_Books() throws SQLException {
        if (createdBookIds.size() < 50) return;

        int updateCount = 50;
        long startTime = System.nanoTime();

        for (int i = 0; i < updateCount; i++) {
            Integer bookId = createdBookIds.get(i);
            bookDAO.updateAvailableCopies(bookId, faker.number().numberBetween(1, 10));
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        logger.info("Updated " + updateCount + " books in " + durationMs + "ms");
        assertThat(durationMs).isLessThan(3000);
    }

    // ==========================================
    // SEARCH PERFORMANCE
    // ==========================================

    @Test
    @Order(9)
    @DisplayName("TC509: SEARCH performance - Search books by title (< 200ms)")
    void testSearchPerformance() throws SQLException {
        // Search term that likely exists or partially exists
        String searchTerm = "Test";

        // Warmup
        bookDAO.searchByTitle(searchTerm);

        int iterations = 20;
        long totalTime = 0;

        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            bookDAO.searchByTitle(searchTerm);
            long end = System.nanoTime();
            totalTime += (end - start);
        }

        long avgTimeMs = (totalTime / iterations) / 1_000_000;
        logger.info("Average Search time: " + avgTimeMs + "ms");

        assertThat(avgTimeMs).isLessThan(200);
    }

    // ==========================================
    // CONNECTION PERFORMANCE
    // ==========================================

    @Test
    @Order(10)
    @DisplayName("TC510: Connection performance - Multiple connection cycles")
    void testConnectionPerformance() {
        int cycles = 50;
        long startTime = System.nanoTime();

        for (int i = 0; i < cycles; i++) {
            try (Connection conn = DatabaseConfig.getConnection()) {
                assertThat(conn.isValid(1)).isTrue();
            } catch (SQLException e) {
                logger.severe("Connection failed: " + e.getMessage());
            }
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;

        logger.info("Opened/Closed " + cycles + " connections in " + durationMs + "ms");

        // Getting connection from pool should be fast
        assertThat(durationMs).isLessThan(2000);
    }

    // ==========================================
    // MEMORY & SCALABILITY
    // ==========================================

    @Test
    @Order(11)
    @DisplayName("TC511: Memory usage - Large result set handling")
    void testMemoryUsage_LargeResultSet() throws SQLException {
        Runtime runtime = Runtime.getRuntime();

        // Garbage collect before test
        runtime.gc();
        long usedMemoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // Load all data
        List<User> users = userDAO.findAll();
        List<Book> books = bookDAO.findAll();

        long usedMemoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = usedMemoryAfter - usedMemoryBefore;
        long memoryIncreaseMB = memoryIncrease / (1024 * 1024);

        logger.info("Memory usage increased by: " + memoryIncreaseMB + " MB");
        logger.info("Loaded " + users.size() + " users and " + books.size() + " books");

        // Basic sanity check - shouldn't explode memory for small dataset
        // This threshold depends on your JVM settings
        assertThat(memoryIncreaseMB).isLessThan(50);
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private User createTestUser() {
        return User.builder()
                .username("perf_user_" + System.nanoTime() + "_" + faker.number().digits(4))
                .email(faker.internet().emailAddress())
                .fullName(faker.name().fullName())
                .phone(faker.phoneNumber().cellPhone())
                .role("member")
                .status("active")
                .build();
    }

    private Book createTestBook() {
        return Book.builder()
                .isbn("978" + System.nanoTime())
                .title(faker.book().title() + " " + System.nanoTime())
                .authorId(1) // Assumes author 1 exists
                .publisherId(1)
                .categoryId(1)
                .publicationYear(2023)
                .pages(faker.number().numberBetween(100, 1000))
                .language("Indonesian")
                .description("Performance test book")
                .totalCopies(10)
                .availableCopies(10)
                .price(new BigDecimal("100000.00"))
                .location("A1")
                .status("available")
                .build();
    }
}
package com.praktikum.database.testing.library.integration;

import com.github.javafaker.Faker;
import com.praktikum.database.testing.library.BaseDatabaseTest;
import com.praktikum.database.testing.library.dao.BookDAO;
import com.praktikum.database.testing.library.dao.BorrowingDAO;
import com.praktikum.database.testing.library.dao.UserDAO;
import com.praktikum.database.testing.library.model.Book;
import com.praktikum.database.testing.library.model.Borrowing;
import com.praktikum.database.testing.library.model.User;
import com.praktikum.database.testing.library.service.BorrowingService;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration Test Suite untuk Borrowing Workflow
 * Menguji interaksi antara Service, DAO, dan Database
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Borrowing Service Integration Test Suite")
public class BorrowingIntegrationTest extends BaseDatabaseTest {

    // Dependencies
    // PENTING: Gunakan tipe DAO asli, BUKAN class Test
    private static UserDAO userDAO;
    private static BookDAO bookDAO;
    private static BorrowingDAO borrowingDAO;
    private static BorrowingService borrowingService;
    private static Faker faker;

    // Test Data
    private User testUser;
    private Book testBook;

    @BeforeAll
    static void setUpAll() {
        logger.info("Starting Borrowing Integration Tests");
        userDAO = new UserDAO();
        bookDAO = new BookDAO();
        borrowingDAO = new BorrowingDAO();
        // Inject DAOs ke Service
        borrowingService = new BorrowingService(userDAO, bookDAO, borrowingDAO);
        faker = new Faker();
    }

    @BeforeEach
    void setUpData() throws SQLException {
        // Setup data bersih sebelum setiap test
        testUser = User.builder()
                .username("int_user_" + System.currentTimeMillis())
                .email("int_" + faker.internet().emailAddress())
                .fullName(faker.name().fullName())
                .phone(faker.phoneNumber().cellPhone())
                .role("member")
                .status("active")
                .build();
        testUser = userDAO.create(testUser);

        testBook = Book.builder()
                .isbn("INT-" + System.currentTimeMillis())
                .title("Integration Test Book")
                .authorId(1)
                .publisherId(1)
                .categoryId(1)
                .publicationYear(2023)
                .pages(200)
                .totalCopies(3)
                .availableCopies(3)
                .price(new BigDecimal("50000"))
                .status("available")
                .build();
        testBook = bookDAO.create(testBook);
    }

    @Test
    @Order(1)
    @DisplayName("TC401: Complete borrowing workflow - Success scenario")
    void testCompleteBorrowingWorkflow_Success() throws SQLException {
        // ACT: User meminjam buku
        Borrowing borrowing = borrowingService.borrowBook(testUser.getUserId(), testBook.getBookId(), 7);

        // ASSERT: Borrowing record created
        assertThat(borrowing).isNotNull();
        assertThat(borrowing.getBorrowingId()).isNotNull();
        assertThat(borrowing.getStatus()).isEqualTo("borrowed");

        // VERIFY: Available copies berkurang
        Optional<Book> updatedBook = bookDAO.findById(testBook.getBookId());
        assertThat(updatedBook).isPresent();
        assertThat(updatedBook.get().getAvailableCopies()).isEqualTo(2); // 3 - 1 = 2

        logger.info("TC401 PASSED: Workflow peminjaman berhasil");
    }

    @Test
    @Order(2)
    @DisplayName("TC402: Complete return workflow - Success scenario")
    void testCompleteReturnWorkflow_Success() throws SQLException {
        // ARRANGE: Buat peminjaman dulu
        Borrowing borrowing = borrowingService.borrowBook(testUser.getUserId(), testBook.getBookId(), 7);

        // ACT: User mengembalikan buku
        boolean returned = borrowingService.returnBook(borrowing.getBorrowingId());

        // ASSERT
        assertThat(returned).isTrue();

        // VERIFY: Status borrowing berubah
        Optional<Borrowing> updatedBorrowing = borrowingDAO.findById(borrowing.getBorrowingId());
        assertThat(updatedBorrowing.get().getStatus()).isEqualTo("returned");
        assertThat(updatedBorrowing.get().getReturnDate()).isNotNull();

        // VERIFY: Available copies bertambah kembali
        Optional<Book> updatedBook = bookDAO.findById(testBook.getBookId());
        assertThat(updatedBook.get().getAvailableCopies()).isEqualTo(3); // Kembali ke awal

        logger.info("TC402 PASSED: Workflow pengembalian berhasil");
    }

    @Test
    @Order(3)
    @DisplayName("TC403: Borrow book dengan inactive user - Should Fail")
    void testBorrowBook_InactiveUser_ShouldFail() throws SQLException {
        // ARRANGE: Set user status inactive
        testUser.setStatus("inactive");
        userDAO.update(testUser);

        // ACT & ASSERT
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            borrowingService.borrowBook(testUser.getUserId(), testBook.getBookId(), 7);
        });

        assertThat(exception.getMessage()).contains("tidak active");
        logger.info("TC403 PASSED: Inactive user blocked");
    }

    @Test
    @Order(4)
    @DisplayName("TC404: Borrow unavailable book - Should Fail")
    void testBorrowBook_UnavailableBook_ShouldFail() throws SQLException {
        // ARRANGE: Set available copies ke 0
        bookDAO.updateAvailableCopies(testBook.getBookId(), 0);

        // ACT & ASSERT
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            borrowingService.borrowBook(testUser.getUserId(), testBook.getBookId(), 7);
        });

        assertThat(exception.getMessage()).contains("Tidak ada kopi");
        logger.info("TC404 PASSED: Unavailable book blocked");
    }

    @Test
    @Order(5)
    @DisplayName("TC407: Borrowing limit enforcement - Maximum 5 books")
    void testBorrowingLimitEnforcement() throws SQLException {
        // ARRANGE: User meminjam 5 buku (batas maks)
        // Kita butuh 5 buku berbeda atau stok yang cukup
        testBook.setAvailableCopies(10);
        testBook.setTotalCopies(10);
        // Manual update karena method update lengkap tidak ada di interface standard DAO
        bookDAO.updateAvailableCopies(testBook.getBookId(), 10);

        for (int i = 0; i < 5; i++) {
            // Kita gunakan buku yang sama untuk simplifikasi tes limit,
            // atau buat buku baru jika ada constraint unique user-book-active
            Book tempBook = createTempBook();
            borrowingService.borrowBook(testUser.getUserId(), tempBook.getBookId(), 7);
        }

        // ACT & ASSERT: Coba pinjam buku ke-6
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            borrowingService.borrowBook(testUser.getUserId(), testBook.getBookId(), 7);
        });

        assertThat(exception.getMessage()).contains("batas peminjaman");
        logger.info("TC407 PASSED: Borrowing limit enforced");
    }

    // Helper untuk membuat buku temporary
    private Book createTempBook() throws SQLException {
        Book b = Book.builder()
                .isbn("TEMP-" + System.nanoTime())
                .title("Temp Book " + System.nanoTime())
                .authorId(1).publisherId(1).categoryId(1)
                .publicationYear(2020).pages(100)
                .totalCopies(5).availableCopies(5)
                .status("available")
                .build();
        return bookDAO.create(b);
    }
}
package com.borrowbox.borrowbox.repository;

import com.borrowbox.borrowbox.model.BorrowRequest;
import com.borrowbox.borrowbox.model.Item;
import com.borrowbox.borrowbox.model.RequestStatus;
import com.borrowbox.borrowbox.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:testdb")
@Transactional // each test is rolled back afterwards
class BorrowRequestRepositoryTest {

    @Autowired UserRepository userRepository;
    @Autowired ItemRepository itemRepository;
    @Autowired BorrowRequestRepository requestRepository;

    private User owner;
    private User borrower;
    private Item item;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(new User("Asha", "asha@test.example"));
        borrower = userRepository.save(new User("Ravi", "ravi@test.example"));
        item = itemRepository.save(new Item("Scientific Calculator", "Casio fx-991", "Stationery", owner));
    }

    @Test
    void savesAndReadsBackRelationships() {
        BorrowRequest saved = requestRepository.save(
                new BorrowRequest(item, borrower, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 15)));

        BorrowRequest found = requestRepository.findById(saved.getId()).orElseThrow();

        assertEquals(RequestStatus.PENDING, found.getStatus());
        assertEquals("Ravi", found.getBorrower().getName());
        assertEquals("Scientific Calculator", found.getItem().getName());
        assertEquals("Asha", found.getItem().getOwner().getName());
    }

    @Test
    void findsRequestsByBorrowerAndByItemOwner() {
        requestRepository.save(
                new BorrowRequest(item, borrower, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 15)));

        assertEquals(1, requestRepository.findByBorrowerId(borrower.getId()).size());
        assertEquals(1, requestRepository.findByItemOwnerId(owner.getId()).size());
        assertTrue(requestRepository.findByBorrowerId(owner.getId()).isEmpty());
    }

    @Test
    void overlapQueryDetectsConflictsOnlyForBlockingStatuses() {
        BorrowRequest approved = new BorrowRequest(
                item, borrower, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 15));
        approved.setStatus(RequestStatus.APPROVED);
        requestRepository.save(approved);

        List<RequestStatus> blocking = List.of(RequestStatus.APPROVED, RequestStatus.ACTIVE);

        // Overlaps the end of the approved loan (12th-20th vs 10th-15th)
        assertEquals(1, requestRepository
                .findByItemAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        item, blocking, LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 12)).size());

        // Starts the day after the loan ends: no conflict
        assertTrue(requestRepository
                .findByItemAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        item, blocking, LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 16)).isEmpty());

        // Same dates, but only PENDING counts as blocking: no conflict
        assertTrue(requestRepository
                .findByItemAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        item, List.of(RequestStatus.PENDING),
                        LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 12)).isEmpty());
    }
}

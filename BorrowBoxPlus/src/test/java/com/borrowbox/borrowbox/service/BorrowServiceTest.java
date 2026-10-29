package com.borrowbox.borrowbox.service;

import com.borrowbox.borrowbox.model.BorrowRequest;
import com.borrowbox.borrowbox.model.Item;
import com.borrowbox.borrowbox.model.RequestStatus;
import com.borrowbox.borrowbox.model.User;
import com.borrowbox.borrowbox.repository.ItemRepository;
import com.borrowbox.borrowbox.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:servicetestdb")
@Transactional
class BorrowServiceTest {

    @Autowired BorrowService service;
    @Autowired UserRepository userRepository;
    @Autowired ItemRepository itemRepository;

    private User owner;
    private User borrower;
    private User stranger;
    private Item item;

    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        owner = userRepository.save(new User("Asha", "asha@test.example"));
        borrower = userRepository.save(new User("Ravi", "ravi@test.example"));
        stranger = userRepository.save(new User("Meena", "meena@test.example"));
        item = itemRepository.save(new Item("Calculator", "Casio fx-991", "Stationery", owner));
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        assertThrows(BorrowRuleException.class, () ->
                service.createRequest(item.getId(), borrower.getId(),
                        today.plusDays(5), today.plusDays(2)));
    }

    @Test
    void rejectsStartDateInThePast() {
        assertThrows(BorrowRuleException.class, () ->
                service.createRequest(item.getId(), borrower.getId(),
                        today.minusDays(1), today.plusDays(2)));
    }

    @Test
    void ownerCannotBorrowOwnItem() {
        assertThrows(BorrowRuleException.class, () ->
                service.createRequest(item.getId(), owner.getId(),
                        today.plusDays(1), today.plusDays(3)));
    }

    @Test
    void cannotRequestUnavailableItem() {
        item.setAvailable(false);
        itemRepository.save(item);
        assertThrows(BorrowRuleException.class, () ->
                service.createRequest(item.getId(), borrower.getId(),
                        today.plusDays(1), today.plusDays(3)));
    }

    @Test
    void onlyOwnerCanApprove() {
        BorrowRequest request = service.createRequest(item.getId(), borrower.getId(),
                today.plusDays(1), today.plusDays(3));
        assertThrows(BorrowRuleException.class,
                () -> service.approve(request.getId(), stranger.getId()));
        assertThrows(BorrowRuleException.class,
                () -> service.approve(request.getId(), borrower.getId()));
    }

    @Test
    void cannotApproveOverlappingLoans() {
        BorrowRequest first = service.createRequest(item.getId(), borrower.getId(),
                today.plusDays(1), today.plusDays(5));
        BorrowRequest second = service.createRequest(item.getId(), stranger.getId(),
                today.plusDays(4), today.plusDays(8));

        service.approve(first.getId(), owner.getId());

        assertThrows(BorrowRuleException.class,
                () -> service.approve(second.getId(), owner.getId()));
    }

    @Test
    void canApproveNonOverlappingLoans() {
        BorrowRequest first = service.createRequest(item.getId(), borrower.getId(),
                today.plusDays(1), today.plusDays(5));
        BorrowRequest second = service.createRequest(item.getId(), stranger.getId(),
                today.plusDays(6), today.plusDays(8));

        service.approve(first.getId(), owner.getId());
        BorrowRequest approved = service.approve(second.getId(), owner.getId());

        assertEquals(RequestStatus.APPROVED, approved.getStatus());
    }

    @Test
    void fullLifecycleFollowsAllowedTransitions() {
        BorrowRequest request = service.createRequest(item.getId(), borrower.getId(),
                today.plusDays(1), today.plusDays(3));
        assertEquals(RequestStatus.PENDING, request.getStatus());

        assertEquals(RequestStatus.APPROVED,
                service.approve(request.getId(), owner.getId()).getStatus());
        assertEquals(RequestStatus.ACTIVE,
                service.markHandedOver(request.getId(), owner.getId()).getStatus());
        assertEquals(RequestStatus.RETURNED,
                service.markReturned(request.getId(), owner.getId()).getStatus());
    }

    @Test
    void cannotSkipOrRepeatStatusSteps() {
        BorrowRequest request = service.createRequest(item.getId(), borrower.getId(),
                today.plusDays(1), today.plusDays(3));

        // PENDING cannot jump straight to ACTIVE or RETURNED
        assertThrows(BorrowRuleException.class,
                () -> service.markHandedOver(request.getId(), owner.getId()));
        assertThrows(BorrowRuleException.class,
                () -> service.markReturned(request.getId(), owner.getId()));

        service.reject(request.getId(), owner.getId());

        // A rejected request cannot be approved afterwards
        assertThrows(BorrowRuleException.class,
                () -> service.approve(request.getId(), owner.getId()));
    }
}
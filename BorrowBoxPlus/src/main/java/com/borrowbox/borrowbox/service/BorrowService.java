package com.borrowbox.borrowbox.service;

import com.borrowbox.borrowbox.model.BorrowRequest;
import com.borrowbox.borrowbox.model.Item;
import com.borrowbox.borrowbox.model.RequestStatus;
import com.borrowbox.borrowbox.model.User;
import com.borrowbox.borrowbox.repository.BorrowRequestRepository;
import com.borrowbox.borrowbox.repository.ItemRepository;
import com.borrowbox.borrowbox.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class BorrowService {

    private static final List<RequestStatus> BLOCKING =
            List.of(RequestStatus.APPROVED, RequestStatus.ACTIVE);

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BorrowRequestRepository requestRepository;

    public BorrowService(ItemRepository itemRepository,
                         UserRepository userRepository,
                         BorrowRequestRepository requestRepository) {
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
    }

    public BorrowRequest createRequest(Long itemId, Long borrowerId,
                                       LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new BorrowRuleException("Start and end dates are required");
        }
        if (end.isBefore(start)) {
            throw new BorrowRuleException("End date cannot be before start date");
        }
        if (start.isBefore(LocalDate.now())) {
            throw new BorrowRuleException("Start date cannot be in the past");
        }

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BorrowRuleException("Item not found"));
        User borrower = userRepository.findById(borrowerId)
                .orElseThrow(() -> new BorrowRuleException("User not found"));

        if (!item.isAvailable()) {
            throw new BorrowRuleException("This item is not currently available for lending");
        }
        if (item.getOwner().getId().equals(borrowerId)) {
            throw new BorrowRuleException("You cannot borrow your own item");
        }

        return requestRepository.save(new BorrowRequest(item, borrower, start, end));
    }

    public BorrowRequest approve(Long requestId, Long actingUserId) {
        BorrowRequest request = loadForOwner(requestId, actingUserId);
        requireStatus(request, RequestStatus.PENDING);

        List<BorrowRequest> conflicts = requestRepository
                .findByItemAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        request.getItem(), BLOCKING,
                        request.getEndDate(), request.getStartDate());
        if (!conflicts.isEmpty()) {
            throw new BorrowRuleException(
                    "These dates overlap with an approved or active loan for this item");
        }

        request.setStatus(RequestStatus.APPROVED);
        return requestRepository.save(request);
    }

    public BorrowRequest reject(Long requestId, Long actingUserId) {
        BorrowRequest request = loadForOwner(requestId, actingUserId);
        requireStatus(request, RequestStatus.PENDING);
        request.setStatus(RequestStatus.REJECTED);
        return requestRepository.save(request);
    }

    public BorrowRequest markHandedOver(Long requestId, Long actingUserId) {
        BorrowRequest request = loadForOwner(requestId, actingUserId);
        requireStatus(request, RequestStatus.APPROVED);
        request.setStatus(RequestStatus.ACTIVE);
        return requestRepository.save(request);
    }

    public BorrowRequest markReturned(Long requestId, Long actingUserId) {
        BorrowRequest request = loadForOwner(requestId, actingUserId);
        requireStatus(request, RequestStatus.ACTIVE);
        request.setStatus(RequestStatus.RETURNED);
        return requestRepository.save(request);
    }

    // ---- helpers ----

    private BorrowRequest loadForOwner(Long requestId, Long actingUserId) {
        BorrowRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new BorrowRuleException("Request not found"));
        if (!request.getItem().getOwner().getId().equals(actingUserId)) {
            throw new BorrowRuleException("Only the item owner can do this");
        }
        return request;
    }

    private void requireStatus(BorrowRequest request, RequestStatus expected) {
        if (request.getStatus() != expected) {
            throw new BorrowRuleException(
                    "Request must be " + expected + " but is " + request.getStatus());
        }
    }
}
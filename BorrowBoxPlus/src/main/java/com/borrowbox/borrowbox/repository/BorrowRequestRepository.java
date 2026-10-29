package com.borrowbox.borrowbox.repository;

import com.borrowbox.borrowbox.model.BorrowRequest;
import com.borrowbox.borrowbox.model.Item;
import com.borrowbox.borrowbox.model.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BorrowRequestRepository extends JpaRepository<BorrowRequest, Long> {

    List<BorrowRequest> findByBorrowerId(Long borrowerId);

    List<BorrowRequest> findByItemOwnerId(Long ownerId);

    List<BorrowRequest> findByItemAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Item item, List<RequestStatus> statuses, LocalDate end, LocalDate start);

    @Query("""
           select r from BorrowRequest r
           join fetch r.item i join fetch i.owner join fetch r.borrower
           where r.borrower.id = :userId
           order by r.startDate desc
           """)
    List<BorrowRequest> findMyRequests(@Param("userId") Long userId);

    @Query("""
           select r from BorrowRequest r
           join fetch r.item i join fetch i.owner join fetch r.borrower
           where i.owner.id = :userId
           order by r.startDate desc
           """)
    List<BorrowRequest> findIncoming(@Param("userId") Long userId);
}
package com.borrowbox.borrowbox.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Entity
public class BorrowRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Item item;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User borrower;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @NotNull
    private RequestStatus status = RequestStatus.PENDING;

    protected BorrowRequest() { }

    public BorrowRequest(Item item, User borrower, LocalDate startDate, LocalDate endDate) {
        this.item = item;
        this.borrower = borrower;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Long getId() { return id; }
    public Item getItem() { return item; }
    public User getBorrower() { return borrower; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
}
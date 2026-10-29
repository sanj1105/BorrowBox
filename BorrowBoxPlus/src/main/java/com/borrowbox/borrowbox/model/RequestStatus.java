package com.borrowbox.borrowbox.model;

public enum RequestStatus {
    PENDING,    // borrower has asked
    APPROVED,   // owner said yes, not yet handed over
    REJECTED,   // owner said no
    ACTIVE,     // item handed over, loan in progress
    RETURNED    // item came back, loan complete
}
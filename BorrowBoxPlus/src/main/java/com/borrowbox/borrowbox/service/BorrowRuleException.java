package com.borrowbox.borrowbox.service;

public class BorrowRuleException extends RuntimeException {
    public BorrowRuleException(String message) {
        super(message);
    }
}
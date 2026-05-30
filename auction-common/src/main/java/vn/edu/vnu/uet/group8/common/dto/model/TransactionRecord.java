package vn.edu.vnu.uet.group8.common.dto.model;

import java.math.BigDecimal;
import java.time.Instant;

import vn.edu.vnu.uet.group8.common.enums.TransactionStatus;
import vn.edu.vnu.uet.group8.common.enums.TransactionType;

public class TransactionRecord {
    private String transactionId;
    private int userId;
    private BigDecimal amount;
    private TransactionType transactionType;
    private Instant createdAt;
    private TransactionStatus status;
    private String description;
    private Integer sessionId;

    public TransactionRecord() {}

    public TransactionRecord(String transactionId, int userId, BigDecimal amount, TransactionType transactionType, Instant createdAt, TransactionStatus status, String description, Integer sessionId) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.amount = amount;
        this.transactionType = transactionType;
        this.createdAt = createdAt;
        this.status = status;
        this.description = description;
        this.sessionId = sessionId;
    }

    public String transactionId() { return transactionId; }
    public int userId() { return userId; }
    public BigDecimal amount() { return amount; }
    public TransactionType transactionType() { return transactionType; }
    public Instant createdAt() { return createdAt; }
    public TransactionStatus status() { return status; }
    public String description() { return description; }
    public Integer sessionId() { return sessionId; }
}
package vn.edu.vnu.uet.group8.common.dto.model;

import java.math.BigDecimal;
import java.time.Instant;

import vn.edu.vnu.uet.group8.common.enums.TransactionType;

public class TransactionHistoryEntry {
    private String transactionId;
    private BigDecimal amount;
    private TransactionType type;
    private String description;
    private Instant createdAt;

    public TransactionHistoryEntry() {}

    public TransactionHistoryEntry(String transactionId, BigDecimal amount, TransactionType type, String description, Instant createdAt) {
        this.transactionId = transactionId;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.createdAt = createdAt;
    }

    public String transactionId() { return transactionId; }
    public BigDecimal amount() { return amount; }
    public TransactionType type() { return type; }
    public String description() { return description; }
    public Instant createdAt() { return createdAt; }
}

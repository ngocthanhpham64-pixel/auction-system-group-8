package vn.edu.vnu.uet.group8.common.dto.model;

import java.math.BigDecimal;
import java.time.Instant;

import vn.edu.vnu.uet.group8.common.enums.TransactionType;

public record TransactionHistoryEntry(
    String transactionId,
    BigDecimal amount,
    TransactionType type,
    Instant createdAt) {}

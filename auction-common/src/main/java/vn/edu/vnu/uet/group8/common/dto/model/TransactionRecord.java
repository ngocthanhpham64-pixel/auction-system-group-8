package vn.edu.vnu.uet.group8.common.dto.model;

import java.math.BigDecimal;
import java.time.Instant;

import vn.edu.vnu.uet.group8.common.enums.TransactionStatus;
import vn.edu.vnu.uet.group8.common.enums.TransactionType;

public record TransactionRecord(
    String transactionId,
    int userId,
    BigDecimal amount,
    TransactionType transactionType,
    Instant createdAt,
    TransactionStatus status,
    String description,
    Integer sessionId
  ) {}
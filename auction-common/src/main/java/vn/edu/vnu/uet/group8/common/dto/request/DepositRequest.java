package vn.edu.vnu.uet.group8.common.dto.request;

import java.math.BigDecimal;

public class DepositRequest {
    private BigDecimal amount;

    private DepositRequest() {}

    public DepositRequest(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0");
        }
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}

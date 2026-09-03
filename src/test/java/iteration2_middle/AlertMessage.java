package iteration2_middle;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AlertMessage {
    DEPOSIT_AMOUNT_CANNOT_EXCEED_5000("Deposit amount cannot exceed 5000"),
    DEPOSIT_AMOUNT_MUST_BE_AT_LEAST_001("Deposit amount must be at least 0.01"),
    UNAUTHORIZED_ACCESS_TO_ACCOUNT("Unauthorized access to account"),

    TRANSFER_AMOUNT_MUST_BE_AT_LEAST_001("Transfer amount must be at least 0.01"),
    TRANSFER_AMOUNT_CANNOT_EXCEED_10000("Transfer amount cannot exceed 10000"),
    INVALID_TRANSFER("Invalid transfer: insufficient funds or invalid accounts"),

    TRANSFER_SUCCESSFUL("Transfer successful");

    private final String message;
}

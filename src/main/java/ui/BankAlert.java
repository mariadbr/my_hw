package ui;

import lombok.Getter;

@Getter
public enum BankAlert {
    SUCCESSFULLY_DEPOSITED("✅ Successfully deposited "),
    PLEASE_ENTER_VALID_AMOUNT("❌ Please enter a valid amount."),
    SUCCESSFULLY_TRANSFERRED("✅ Successfully transferred "),
    ERROR_INVALID_TRANSFER_INSUFFICIENT_FUNDS_OR_INVALID_ACCOUNTS("❌ Error: Invalid transfer: insufficient funds or invalid accounts"),
    NAME_UPDATED_SUCCESSFULLY("✅ Name updated successfully!"),
    NAME_MUST_CONTAIN_TWO_WORDS_WITH_LETTERS_ONLY("Name must contain two words with letters only");

    private final String message;

    BankAlert(String message) {
        this.message = message;
    }
}

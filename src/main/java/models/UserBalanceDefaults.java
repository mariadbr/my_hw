package models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum UserBalanceDefaults {
    MAX_DEPOSIT_AMOUNT(5000.0f),

    INITIAL_BALANCE(0.0f);

    private final float amount;
}

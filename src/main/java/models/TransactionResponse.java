package models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse { //или переименовать Transaction? ситуация как с AccountResponse?
    private long id; //транзакции
    private float amount;
    private TransactionType type; //enum?
    private String timestamp;
    private long relatedAccountId;
}

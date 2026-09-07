package models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetTransactionResponse extends BaseModel{
    private long id; //транзакции
    private float amount;
    private TransactionType type; //enum?
    private String timestamp;
    private long relatedAccountId;
}

package api.models;

import lombok.*;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetTransactionResponse extends BaseModel{
    private long id; //транзакции
    private float amount;
    private TransactionType type;
    private String timestamp;
    private long relatedAccountId;
}

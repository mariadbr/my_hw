package api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
//@JsonIgnoreProperties(ignoreUnknown = true)
public class GetTransactionResponse extends BaseModel{
    private long id; //транзакции
    private float amount;
    private TransactionType type;
    private String timestamp;
    private String timestampAsString;
    private long relatedAccountId;
    private double amountAsDouble;
}

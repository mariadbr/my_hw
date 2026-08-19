package models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    private long id;
    private float amount;
    private String type; //enum
    private String timestamp;
    private long relatedAccountId;
}

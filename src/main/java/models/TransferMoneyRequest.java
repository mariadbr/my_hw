package models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferMoneyRequest extends BaseModel{
    private long senderAccountId;
    private long receiverAccountId;
    private float amount;
}

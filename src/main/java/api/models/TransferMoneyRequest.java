package api.models;

import lombok.*;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferMoneyRequest extends BaseModel{
    private long senderAccountId;
    private long receiverAccountId;
    private float amount;
}

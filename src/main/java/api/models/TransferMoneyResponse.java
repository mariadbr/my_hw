package api.models;

import lombok.*;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferMoneyResponse extends BaseModel{
    private long senderAccountId;
    private long receiverAccountId;
    private float amount;
    private String message;
}

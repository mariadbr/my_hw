package api.models;

import lombok.*;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DepositMoneyResponse extends BaseModel{
    private long id;
    private String accountNumber;
    private float balance;
    private List<GetTransactionResponse> transactions;
}

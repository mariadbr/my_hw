package api.models;

import lombok.*;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAccountResponse extends BaseModel{
    private long id;
    private String accountNumber;
    private float balance;
    private List<GetTransactionResponse> transactions;
}

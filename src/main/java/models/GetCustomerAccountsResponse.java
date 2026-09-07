package models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
//GetCustomerAccounts
public class GetCustomerAccountsResponse extends BaseModel{
    private long id; //счета
    private String accountNumber;
    private float balance;
    private List<GetTransactionResponse> transactions;
}

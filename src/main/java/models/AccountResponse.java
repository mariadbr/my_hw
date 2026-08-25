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
public class AccountResponse extends BaseModel{ //нужно ли BaseModel; стоит ли переименовать в Account, тк это не целый response, а только часть
    //или GetCustomerAccountsResponse ?
    private long id;
    private String accountNumber;
    private float balance;
    private List<TransactionResponse> transactions;
}

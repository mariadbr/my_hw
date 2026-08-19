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
public class AccountResponse extends BaseModel{ //нужно ли BaseModel
    private long id;
    private String accountNumber;
    private double balance;
    private List<String> transactions;
}

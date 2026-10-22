package api.dao;

import lombok.*;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountDao extends BaseDao{
    private Long id;
    private String accountNumber;
    private float balance;
    private Long customerId;
}

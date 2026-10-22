package api.dao;

import api.models.TransactionType;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionDao extends BaseDao{
    private long id; //транзакции
    private float amount;
    private TransactionType type;
    private long related_account_id;
    }

package api.models;

import lombok.*;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GetCustomerProfileResponse extends BaseModel{
    private long id;
    private String username;
    private String password;
    private String name;
    private UserRole role;
    private List<GetCustomerAccountsResponse> accounts;
}

package models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
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

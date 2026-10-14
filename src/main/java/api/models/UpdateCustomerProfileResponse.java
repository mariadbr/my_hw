package api.models;

import lombok.*;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCustomerProfileResponse extends BaseModel{
    private String message;
    private GetCustomerProfileResponse customer;
}

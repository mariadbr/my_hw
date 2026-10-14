package api.models;

import lombok.*;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCustomerProfileRequest extends BaseModel{
    private String name;
}

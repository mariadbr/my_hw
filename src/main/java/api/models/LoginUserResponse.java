package api.models;

import lombok.*;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginUserResponse extends BaseModel{
    private String username;
    private String role;
}

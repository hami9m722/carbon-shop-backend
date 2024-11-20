package uit.carbon_shop.base.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import uit.carbon_shop.base.util.WebUtils;


@Getter
@Setter
public class UserDTO {

    private UUID userId;

    @Size(max = 255)
    @Email(regexp = WebUtils.EMAIL_PATTERN)
    private String password;

    @Size(max = 255)
    private String passwordSalt;

    @Size(max = 255)
    private String resetPasswordUid;

    private OffsetDateTime resetPasswordStart;

    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String phone;

    @Size(max = 255)
    private String email;

    @UserCompanyUnique
    private UUID company;

}

package uit.carbon_shop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import uit.carbon_shop.util.WebUtils;


@Getter
@Setter
public class AppUserDTO {

    private Long userId;

    @NotNull
    @Size(max = 255)
    @Schema(hidden = true)
    private String password;

    @Size(max = 255)
    @Schema(hidden = true)
    private String resetPasswordUid;

    @Schema(hidden = true)
    private OffsetDateTime resetPasswordStart;

    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String phone;

    @Size(max = 255)
    @Email(regexp = WebUtils.EMAIL_PATTERN)
    private String email;

    private Long avatar;

    @NotNull
    private UserRole role;

    private LocalDateTime approvedAt;

    private LocalDateTime rejectedAt;

    private UserStatus status;

    @AppUserCompanyUnique
    private Long company;

    private List<Long> favoriteProjects;

    private List<Long> likedCompanyReviews;

    private List<Long> likeProjectReviews;

}

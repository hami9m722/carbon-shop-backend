package uit.carbon_shop.model;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class MediatorProcessOrderDTO {

    private Long orderId;

    @Size(max = 255)
    private String message;

}

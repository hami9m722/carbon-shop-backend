package uit.carbon_shop.model;

import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class MediatorDoneOrderDTO {

    private Long contractFile;

    private List<Long> certImages;

    private Long paymentBillFile;

    @Size(max = 255)
    private String message;

}

package uit.carbon_shop.model;

import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class OrderDTO {

    private Long orderId;

    private Long creditAmount;

    @Size(max = 255)
    private String unit;

    @Size(max = 255)
    private String price;

    @Size(max = 255)
    private String total;

    private OrderStatus status;

    private Long paymentBillFile;

    private Long contractFile;

    private List<Long> certImages;

    private Long project;

    private Long processBy;

    private Long createdBy;

    private LocalDateTime contractSignDate;

    private LocalDateTime payDate;

    private LocalDateTime deliveryDate;

}

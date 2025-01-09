package uit.carbon_shop.model;


public enum OrderStatus {

    INIT,
    PROCESSING,
    CANCELLED,
    DONE,
    ;

    public boolean canUpdateTo(OrderStatus status) {
        return switch (this) {
            case INIT -> status == PROCESSING || status == CANCELLED;
            case PROCESSING -> status == DONE || status == CANCELLED;
            default -> false;
        };
    }

}

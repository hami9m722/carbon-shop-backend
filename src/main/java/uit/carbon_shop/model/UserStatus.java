package uit.carbon_shop.model;


public enum UserStatus {

    INIT,
    APPROVED,
    REJECTED,
    ;

    public boolean canUpdateTo(UserStatus status) {
        return switch (this) {
            case INIT -> status == APPROVED || status == REJECTED;
            default -> false;
        };
    }

}

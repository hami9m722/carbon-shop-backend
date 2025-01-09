package uit.carbon_shop.model;


public enum CompanyStatus {

    INIT,
    APPROVED,
    REJECTED,
    ;

    public boolean canUpdateTo(CompanyStatus status) {
        return switch (this) {
            case INIT -> status == APPROVED || status == REJECTED;
            default -> false;
        };
    }

}

package uit.carbon_shop.model;


public enum ProjectStatus {

    INIT,
    APPROVED,
    REJECTED,
    ;

    public boolean canUpdateTo(ProjectStatus status) {
        return switch (this) {
            case INIT -> status == APPROVED || status == REJECTED;
            default -> false;
        };
    }

}

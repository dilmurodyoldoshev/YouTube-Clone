package uz.app.pdptube.enums;

public enum NotificationType {
    NEW_VIDEO_POSTED("New video posted by channel"),
    NEW_FOLLOWER("New user followed you");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getString() {
        return displayName;
    }

}

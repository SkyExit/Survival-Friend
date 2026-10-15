package de.laurinhummel.survivalfriend.compat;

public enum ServerPlatform {
    SPIGOT, PAPER, FOLIA;

    private static final ServerPlatform CURRENT = detect();

    public static boolean isFolia() { return CURRENT == FOLIA; }

    public static boolean isPaper() { return CURRENT == PAPER || CURRENT == FOLIA; }

    private static ServerPlatform detect() {
        if (hasClass("io.papermc.paper.threadedregions.RegionizedServer")) return FOLIA;
        if (hasClass("io.papermc.paper.configuration.GlobalConfiguration")) return PAPER;
        return SPIGOT;
    }

    private static boolean hasClass(String name) {
        try {
            Class.forName(name, false, ServerPlatform.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}

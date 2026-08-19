package com.everest.x.api;

/**
 * Tipo deste servidor na rede. Cada plugin de modo usa o que precisar;
 * o Core só informa. Servidor avulso ou desconhecido cai em {@link #OTHER}.
 */
public enum ServerType {
    LOBBY,
    DUELS,
    BEDWARS,
    SURVIVAL,
    PARKOUR,
    OTHER;

    public static ServerType fromConfig(String raw) {
        if (raw == null || raw.isBlank()) {
            return OTHER;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return OTHER;
        }
    }
}

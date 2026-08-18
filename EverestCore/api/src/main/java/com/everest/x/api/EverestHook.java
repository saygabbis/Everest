package com.everest.x.api;

/**
 * Um plugin Everest se apresenta ao Core. Sem isso o Core não importa
 * classe de Bedwars/Duels — o filho que se registra, se quiser.
 */
public interface EverestHook {

    /** id estável, ex.: {@code bedwars}, {@code duels}, {@code lobby} */
    String getId();

    String getDisplayName();

    String getVersion();
}

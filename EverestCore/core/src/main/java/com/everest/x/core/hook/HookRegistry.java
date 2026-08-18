package com.everest.x.core.hook;

import com.everest.x.api.EverestHook;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HookRegistry {

    private final Map<String, EverestHook> hooks = new ConcurrentHashMap<>();

    public void register(EverestHook hook) {
        hooks.put(hook.getId().toLowerCase(), hook);
    }

    public void unregister(EverestHook hook) {
        hooks.remove(hook.getId().toLowerCase());
    }

    public Collection<EverestHook> all() {
        return Collections.unmodifiableCollection(hooks.values());
    }

    public void clear() {
        hooks.clear();
    }

    public String summary() {
        if (hooks.isEmpty()) {
            return "nenhum";
        }
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (EverestHook hook : hooks.values()) {
            if (!first) {
                builder.append(", ");
            }
            builder.append(hook.getDisplayName());
            first = false;
        }
        return builder.toString();
    }
}

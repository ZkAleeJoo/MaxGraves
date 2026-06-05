package org.zkaleejoo.commands;

import java.util.Optional;
import java.util.UUID;

public record InfoMenuClickAction(InfoMenuAction action, UUID graveId) {

    public Optional<UUID> graveIdOptional() {
        return Optional.ofNullable(graveId);
    }
}

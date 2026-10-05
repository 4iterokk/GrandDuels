package org.chiterok.grandDuels.command;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A single sub-command. {@code args} passed to the methods exclude the sub-command label itself. */
public interface SubCommand {

    String name();

    default List<String> aliases() {
        return List.of();
    }

    /** @return the required permission or {@code null} if none. */
    default @Nullable String permission() {
        return null;
    }

    default boolean playerOnly() {
        return false;
    }

    void execute(CommandSender sender, String[] args);

    default List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}

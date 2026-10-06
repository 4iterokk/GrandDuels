package org.chiterok.grandDuels.match;

import org.bukkit.scheduler.BukkitTask;
import org.chiterok.grandDuels.kit.Kit;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A pending duel invitation. {@code kit} is a standard kit or a kit made by the sender. {@code arena} is the id of the
 * arena the sender picked ({@code null} = any free arena). {@code expiryTask} is cancelled when the request is resolved.
 */
public record DuelRequest(UUID senderId, String senderName, UUID targetId, String targetName,
                          Kit kit, MatchSettings settings, @Nullable String arena, BukkitTask expiryTask) {}

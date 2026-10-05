package org.chiterok.grandDuels.match;

import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

/** A pending duel invitation. {@code expiryTask} is cancelled when the request is resolved. */
public record DuelRequest(UUID senderId, String senderName, UUID targetId, String targetName,
                          String kitId, MatchSettings settings, BukkitTask expiryTask) {}

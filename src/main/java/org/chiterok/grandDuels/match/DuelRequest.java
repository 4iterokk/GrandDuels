package org.chiterok.grandDuels.match;

import org.bukkit.scheduler.BukkitTask;
import org.chiterok.grandDuels.kit.Kit;

import java.util.UUID;

/** A pending duel invitation. {@code kit} is a standard kit or a kit made by the sender. {@code expiryTask} is cancelled when the request is resolved. */
public record DuelRequest(UUID senderId, String senderName, UUID targetId, String targetName,
                          Kit kit, MatchSettings settings, BukkitTask expiryTask) {}

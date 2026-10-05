package org.chiterok.grandDuels.match;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.chiterok.grandDuels.GrandDuels;
import org.chiterok.grandDuels.config.Messages;
import org.chiterok.grandDuels.kit.Kit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Pending duel requests. At most one outgoing request per sender. Main thread only. */
public final class RequestManager {

    public enum SendResult { SENT, ALREADY_PENDING }

    private final GrandDuels plugin;
    private final Map<UUID, DuelRequest> bySender = new LinkedHashMap<>();

    public RequestManager(GrandDuels plugin) {
        this.plugin = plugin;
    }

    public SendResult send(Player sender, Player target, Kit kit, MatchSettings settings) {
        if (bySender.containsKey(sender.getUniqueId())) return SendResult.ALREADY_PENDING;

        int seconds = plugin.settings().duel().requestExpireSeconds();
        UUID senderId = sender.getUniqueId();
        BukkitTask expiry = Bukkit.getScheduler().runTaskLater(plugin, () -> expire(senderId), seconds * 20L);
        DuelRequest request = new DuelRequest(senderId, sender.getName(), target.getUniqueId(), target.getName(),
                kit.id(), settings, expiry);
        bySender.put(senderId, request);

        Messages messages = plugin.messages();
        messages.send(sender, "duels.request-sent", "target", target.getName(), "kit", kit.displayName(),
                "seconds", seconds);
        target.sendMessage(requestComponent(request, kit, seconds));
        return SendResult.SENT;
    }

    private Component requestComponent(DuelRequest request, Kit kit, int seconds) {
        Messages m = plugin.messages();
        Component text = m.prefixed("duels.request-received", Messages.ph(
                "sender", request.senderName(), "kit", kit.displayName(), "seconds", seconds,
                "gapples", plainState(request.settings().allowGapples()),
                "cooldowns", plainState(request.settings().customCooldowns())));
        Component accept = m.get("duels.request-accept-button")
                .clickEvent(ClickEvent.runCommand("/duel accept " + request.senderName()))
                .hoverEvent(HoverEvent.showText(m.get("duels.request-accept-hover")));
        Component deny = m.get("duels.request-deny-button")
                .clickEvent(ClickEvent.runCommand("/duel deny " + request.senderName()))
                .hoverEvent(HoverEvent.showText(m.get("duels.request-deny-hover")));
        return text.append(Component.newline()).append(accept).append(Component.space()).append(deny);
    }

    private String plainState(boolean on) {
        return plugin.messages().string(on ? "duels.state-on" : "duels.state-off");
    }

    private void expire(UUID senderId) {
        DuelRequest request = bySender.remove(senderId);
        if (request == null) return;
        Player sender = Bukkit.getPlayer(request.senderId());
        Player target = Bukkit.getPlayer(request.targetId());
        if (sender != null) plugin.messages().send(sender, "duels.request-expired", "target", request.targetName());
        if (target != null) {
            plugin.messages().send(target, "duels.request-expired-target", "sender", request.senderName());
        }
    }

    public Optional<DuelRequest> outgoing(UUID senderId) {
        return Optional.ofNullable(bySender.get(senderId));
    }

    public List<DuelRequest> incoming(UUID targetId) {
        List<DuelRequest> list = new ArrayList<>();
        for (DuelRequest request : bySender.values()) {
            if (request.targetId().equals(targetId)) list.add(request);
        }
        return list;
    }

    /** Removes and returns the request of {@code senderId}, cancelling its expiry. */
    public Optional<DuelRequest> consume(UUID senderId) {
        DuelRequest request = bySender.remove(senderId);
        if (request == null) return Optional.empty();
        request.expiryTask().cancel();
        return Optional.of(request);
    }

    /** Drops every request involving the player (quit, duel start). */
    public void purge(UUID playerId) {
        List<UUID> senders = new ArrayList<>();
        for (DuelRequest request : bySender.values()) {
            if (request.senderId().equals(playerId) || request.targetId().equals(playerId)) {
                senders.add(request.senderId());
            }
        }
        for (UUID sender : senders) consume(sender);
    }

    public void clear() {
        for (DuelRequest request : bySender.values()) request.expiryTask().cancel();
        bySender.clear();
    }
}

package org.chiterok.grandDuels.runtime;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.inventory.ItemStack;
import org.chiterok.grandDuels.GrandDuels;
import org.chiterok.grandDuels.config.Settings;
import org.chiterok.grandDuels.cooldown.CooldownType;
import org.chiterok.grandDuels.match.Match;

/** Stateful duel rules: item cooldowns, gapple toggle, riptide restriction and heavy-weapon tuning. */
public final class PvPRulesListener implements Listener {

    private static final float MACE_SMASH_MIN_FALL = 1.5f;

    private final GrandDuels plugin;

    public PvPRulesListener(GrandDuels plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.matches().of(player);
        if (match == null) return;

        Material material = event.getItem().getType();
        boolean gapple = material == Material.GOLDEN_APPLE || material == Material.ENCHANTED_GOLDEN_APPLE;
        if (gapple && !match.settings().allowGapples()) {
            event.setCancelled(true);
            plugin.messages().actionBar(player, "duels.gapples-disabled");
            return;
        }
        CooldownType type = CooldownType.forConsumable(material);
        if (type == null || !match.usesCustomCooldowns()) return;
        if (plugin.cooldowns().isOnCooldown(player, type)) {
            event.setCancelled(true);
            plugin.cooldowns().notifyBlocked(player, type);
            return;
        }
        plugin.cooldowns().apply(player, type);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;
        Match match = plugin.matches().of(player);
        if (match == null || !match.usesCustomCooldowns()) return;
        CooldownType type = CooldownType.forProjectile(event.getEntityType());
        if (type == null) return;
        if (plugin.cooldowns().isOnCooldown(player, type)) {
            event.setCancelled(true);
            plugin.cooldowns().notifyBlocked(player, type);
            return;
        }
        plugin.cooldowns().apply(player, type);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRiptide(PlayerRiptideEvent event) {
        Player player = event.getPlayer();
        Match match = plugin.matches().of(player);
        if (match != null && match.usesCustomCooldowns()) plugin.cooldowns().apply(player, CooldownType.TRIDENT);
    }

    /** Right-click with a Riptide trident would launch the player; deny it when riptide is disabled. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (plugin.settings().rules().riptideEnabled()) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (plugin.matches().of(player) == null) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.TRIDENT || !item.containsEnchantment(Enchantment.RIPTIDE)) return;
        event.setUseItemInHand(Event.Result.DENY);
        event.setCancelled(true);
        plugin.messages().actionBar(player, "duels.riptide-blocked");
    }

    /** Mace smash and spear charge tuning: optional damage cap and knockback multiplier. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHeavyAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof Player victim)) return;
        Match match = plugin.matches().of(attacker);
        if (match == null || !match.isFighting()) return;

        Material held = attacker.getInventory().getItemInMainHand().getType();
        Settings.WeaponRule rule;
        if (held == Material.MACE && attacker.getFallDistance() > MACE_SMASH_MIN_FALL) {
            rule = plugin.settings().rules().mace();
        } else if (held.name().endsWith("_SPEAR") && attacker.isSprinting()) {
            rule = plugin.settings().rules().spear();
        } else {
            return;
        }

        double finalDamage = event.getFinalDamage();
        if (rule.maxDamage() > 0.0 && finalDamage > rule.maxDamage()) {
            // Armor/enchant reductions are applied on top of the base damage, so scale the base proportionally.
            event.setDamage(event.getDamage() * (rule.maxDamage() / finalDamage));
        }
        double multiplier = rule.knockbackMultiplier();
        if (Math.abs(multiplier - 1.0) > 1.0E-6) scaleKnockback(victim, multiplier);
    }

    private void scaleKnockback(LivingEntity victim, double multiplier) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (victim.isValid()) victim.setVelocity(victim.getVelocity().multiply(multiplier));
        });
    }
}

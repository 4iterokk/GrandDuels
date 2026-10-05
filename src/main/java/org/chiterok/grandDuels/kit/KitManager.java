package org.chiterok.grandDuels.kit;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.chiterok.grandDuels.GrandDuels;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/** Loads kits from kits.yml and saves kits from a player's current loadout. */
public final class KitManager {

    private static final Pattern VALID_ID = Pattern.compile("[a-z0-9_-]{1,32}");

    private final GrandDuels plugin;
    private final KitItemCodec codec;
    private volatile Map<String, Kit> kits = Map.of();

    public KitManager(GrandDuels plugin) {
        this.plugin = plugin;
        this.codec = new KitItemCodec(plugin);
    }

    public static boolean isValidId(String id) {
        return VALID_ID.matcher(id).matches();
    }

    public void reload() {
        Map<String, Kit> loaded = new LinkedHashMap<>();
        ConfigurationSection root = plugin.configs().kits().getConfigurationSection("kits");
        if (root != null) {
            for (String rawId : root.getKeys(false)) {
                ConfigurationSection section = root.getConfigurationSection(rawId);
                if (section == null) continue;
                String id = rawId.toLowerCase(Locale.ROOT);
                try {
                    loaded.put(id, parse(id, section));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Skipping kit '" + rawId + "': " + e.getMessage());
                }
            }
        }
        this.kits = loaded;
        plugin.getLogger().info("Loaded " + loaded.size() + " kit(s).");
    }

    public @Nullable Kit get(String id) {
        return kits.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<Kit> all() {
        return List.copyOf(kits.values());
    }

    public List<String> ids() {
        return new ArrayList<>(kits.keySet());
    }

    private Kit parse(String id, ConfigurationSection s) {
        Set<String> used = new TreeSet<>();

        Map<Integer, ItemStack> items = new LinkedHashMap<>();
        ConfigurationSection itemSection = s.getConfigurationSection("items");
        if (itemSection != null) {
            for (String key : itemSection.getKeys(false)) {
                int slot;
                try {
                    slot = Integer.parseInt(key);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("item slot '" + key + "' is not a number");
                }
                if (slot < 0 || slot > 35) throw new IllegalArgumentException("item slot " + slot + " is outside 0-35");
                items.put(slot, decodeSection(itemSection, key, "items." + key, used));
            }
        }

        ConfigurationSection armor = s.getConfigurationSection("armor");
        ItemStack helmet = armor == null ? null : decodeOptional(armor, "helmet", used);
        ItemStack chestplate = armor == null ? null : decodeOptional(armor, "chestplate", used);
        ItemStack leggings = armor == null ? null : decodeOptional(armor, "leggings", used);
        ItemStack boots = armor == null ? null : decodeOptional(armor, "boots", used);
        ItemStack offhand = decodeOptional(s, "offhand", used);

        List<PotionEffect> effects = new ArrayList<>();
        for (Map<?, ?> effect : s.getMapList("effects")) effects.add(codec.parseEffect(effect));

        ItemStack icon = parseIcon(s, items, used);
        return new Kit(id, s.getString("display-name", "&f" + id), icon, List.copyOf(s.getStringList("description")),
                Map.copyOf(items), helmet, chestplate, leggings, boots, offhand, List.copyOf(effects),
                Set.copyOf(used));
    }

    private ItemStack decodeSection(ConfigurationSection parent, String key, String path, Set<String> used) {
        ConfigurationSection section = parent.getConfigurationSection(key);
        if (section == null) throw new IllegalArgumentException(path + " must be a section");
        try {
            return codec.decode(section, used);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(path + ": " + e.getMessage(), e);
        }
    }

    private @Nullable ItemStack decodeOptional(ConfigurationSection parent, String key, Set<String> used) {
        return parent.isConfigurationSection(key) ? decodeSection(parent, key, key, used) : null;
    }

    private ItemStack parseIcon(ConfigurationSection s, Map<Integer, ItemStack> items, Set<String> used) {
        if (s.isConfigurationSection("icon")) return decodeSection(s, "icon", "icon", used);
        String name = s.getString("icon");
        if (name != null) {
            Material material = Material.matchMaterial(name);
            if (material == null || material.isAir() || !material.isItem()) {
                throw new IllegalArgumentException("icon: unknown material '" + name + "'");
            }
            return new ItemStack(material);
        }
        if (!items.isEmpty()) return new ItemStack(items.values().iterator().next().getType());
        return new ItemStack(Material.IRON_SWORD);
    }

    /** Overwrites items/armor/offhand/effects of kit {@code id} (creating it if needed) from the player's loadout. */
    public void save(String id, Player player) {
        FileConfiguration yaml = plugin.configs().kits();
        ConfigurationSection kit = yaml.getConfigurationSection("kits." + id);
        if (kit == null) {
            kit = yaml.createSection("kits." + id);
            kit.set("display-name", "&f" + id);
            ItemStack inHand = player.getInventory().getItemInMainHand();
            kit.set("icon", inHand.getType().isAir() ? Material.IRON_SWORD.name() : inHand.getType().name());
            kit.set("description", List.of("&7Saved from a player loadout."));
        }
        kit.set("items", null);
        kit.set("armor", null);
        kit.set("offhand", null);
        kit.set("effects", null);

        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (isEmpty(item)) continue;
            codec.encode(kit.createSection("items." + slot), item);
        }
        writeArmor(kit, "helmet", inventory.getHelmet());
        writeArmor(kit, "chestplate", inventory.getChestplate());
        writeArmor(kit, "leggings", inventory.getLeggings());
        writeArmor(kit, "boots", inventory.getBoots());
        ItemStack offhand = inventory.getItemInOffHand();
        if (!isEmpty(offhand)) codec.encode(kit.createSection("offhand"), offhand);

        List<Map<String, Object>> effects = new ArrayList<>();
        for (PotionEffect effect : player.getActivePotionEffects()) effects.add(codec.encodeEffect(effect));
        if (!effects.isEmpty()) kit.set("effects", effects);

        plugin.configs().kitsFile().save();
        reload();
    }

    private void writeArmor(ConfigurationSection kit, String slot, @Nullable ItemStack item) {
        if (!isEmpty(item)) codec.encode(kit.createSection("armor." + slot), item);
    }

    private static boolean isEmpty(@Nullable ItemStack item) {
        return item == null || item.getType().isAir();
    }
}

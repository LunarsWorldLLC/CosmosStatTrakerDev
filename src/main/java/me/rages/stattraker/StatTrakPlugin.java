package me.rages.stattraker;

import com.google.common.collect.ImmutableSet;
import me.lucko.helper.item.ItemStackBuilder;
import me.lucko.helper.plugin.ExtendedJavaPlugin;
import me.lucko.helper.plugin.ap.Plugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Plugin(
        name = "StatTrak",
        hardDepends = {"helper"},
        softDepends = {"Augments"},
        apiVersion = "1.18"
)
public final class StatTrakPlugin extends ExtendedJavaPlugin {

    public static final String ITEM_KEY = "stat-traker";
    private NamespacedKey statTrakItemKey;

    private ItemStack removerItemStack;
    public static Set<NamespacedKey> TRACKER_KEYS = new HashSet<>();

    // Indexed 1..maxLevel; index 0 unused so level numbers map directly to array slots.
    private String[] levelColors;
    private int maxLevel;

    private final ImmutableSet<Material> validItems = ImmutableSet.of(
            Material.DIAMOND_AXE, Material.NETHERITE_AXE, Material.IRON_AXE,
            Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.IRON_SWORD,
            Material.WOODEN_SWORD, Material.CROSSBOW, Material.BOW, Material.TRIDENT,
            Material.FISHING_ROD, Material.MACE,
            Material.WOODEN_SPEAR, Material.STONE_SPEAR, Material.GOLDEN_SPEAR,
            Material.COPPER_SPEAR, Material.IRON_SPEAR, Material.DIAMOND_SPEAR,
            Material.NETHERITE_SPEAR
    );

    @Override
    protected void enable() {
        TRACKER_KEYS.clear();
        this.statTrakItemKey = new NamespacedKey(this, ITEM_KEY);
        this.saveDefaultConfig();
        loadLevelPalette();
        this.bindModule(new StatTrakManager(this));

        this.removerItemStack = ItemStackBuilder.of(Material.valueOf(getConfig().getString("stack-trak-remover.type")))
                .name(getConfig().getString("stack-trak-remover.name"))
                .lore(getConfig().getStringList("stack-trak-remover.lore"))
                .build();

    }

    private void loadLevelPalette() {
        this.maxLevel = getConfig().getInt("stat-trak-levels.max-level", 5);
        if (maxLevel < 1) maxLevel = 1;
        this.levelColors = new String[maxLevel + 1];
        List<String> defaults = new ArrayList<>();
        defaults.add("&b"); defaults.add("&c"); defaults.add("&6"); defaults.add("&e"); defaults.add("&6&l");
        for (int i = 1; i <= maxLevel; i++) {
            String fromConfig = getConfig().getString("stat-trak-levels.colors." + i);
            levelColors[i] = fromConfig != null ? fromConfig
                    : (i - 1 < defaults.size() ? defaults.get(i - 1) : "&b");
        }
    }

    /** Returns the color code (may include multiple &-codes) for the given tracker level. Clamps to [1, maxLevel]. */
    public String getLevelColor(int level) {
        if (level < 1) level = 1;
        if (level > maxLevel) level = maxLevel;
        return levelColors[level];
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public ItemStack getRemoverItemStack() {
        return removerItemStack;
    }

    public boolean isRemoverItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        if (!item.hasItemMeta() || !removerItemStack.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        ItemMeta removerMeta = removerItemStack.getItemMeta();

        // Compare type
        if (item.getType() != removerItemStack.getType()) return false;

        // Compare display name
        if (meta.hasDisplayName() && removerMeta.hasDisplayName()) {
            return meta.getDisplayName().equals(removerMeta.getDisplayName());
        } else {
            return false;
        }
    }

    public NamespacedKey getStatTrakItemKey() {
        return statTrakItemKey;
    }

    public ImmutableSet<Material> getValidItems() {
        return validItems;
    }
}

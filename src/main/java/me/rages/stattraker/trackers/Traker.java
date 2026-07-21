package me.rages.stattraker.trackers;

import me.lucko.helper.text3.Text;
import me.rages.stattraker.StatTrakPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.List;

/**
 * @author : Michael
 * @since : 10/7/2022, Friday
 **/
public class Traker {

    private ItemStack itemStack;
    private NamespacedKey itemKey;
    // Companion PDC key that stores the tracker's current level (1..maxLevel). Absent = 1.
    private NamespacedKey levelKey;
    private String dataLore;
    private String prefixLore;
    // Color-code-free prefix used to identify our counter line on the item — level-independent, since
    // the stored line's color code varies with the tracker's current level.
    private String strippedMatchPrefix;
    private StatTrakPlugin plugin;


    private HashSet<Material> acceptableItems = new HashSet<>();

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public void setItemKey(NamespacedKey itemKey) {
        this.itemKey = itemKey;
        StatTrakPlugin.TRACKER_KEYS.add(itemKey);
    }

    public NamespacedKey getItemKey() {
        return itemKey;
    }

    public NamespacedKey getLevelKey() {
        return levelKey;
    }

    public void setDataLore(String dataLore) {
        this.dataLore = dataLore;
    }

    public String getDataLore() {
        return dataLore;
    }

    public void setPrefixLore(String prefixLore) {
        this.prefixLore = prefixLore;
    }

    public String getPrefixLore() {
        return prefixLore;
    }

    public HashSet<Material> getAcceptableItems() {
        return acceptableItems;
    }

    /**
     * Wire up level support after the subclass has set dataLore + itemKey.
     * If dataLore has no %color% placeholder, insert it at the start so every tracker
     * supports the level-color feature without requiring existing configs to be edited.
     * Also caches a color-stripped match prefix used by {@link #matchesLine(String)}.
     */
    protected void initLevelSupport(StatTrakPlugin plugin) {
        this.plugin = plugin;
        this.levelKey = new NamespacedKey(plugin, itemKey.getKey() + "_LEVEL");
        if (dataLore != null && !dataLore.contains("%color%")) {
            setDataLore("%color%" + dataLore);
        }
        String preAmount = dataLore != null ? dataLore.split("%amount%")[0] : "";
        // Strip both color codes and the unresolved %color% token so match is level-agnostic.
        String bare = preAmount.replace("%color%", "");
        this.strippedMatchPrefix = ChatColor.stripColor(Text.colorize(bare));
    }

    /** Fully colorize the counter line for a given level + total. Safe to call each rebuild. */
    public String renderLine(int level, int total) {
        String color = plugin != null ? plugin.getLevelColor(level) : "";
        return Text.colorize(dataLore
                .replace("%color%", color)
                .replace("%amount%", String.format("%,d", total)));
    }

    /** True when the given stored lore line is this tracker's counter line, regardless of level color. */
    public boolean matchesLine(String storedLine) {
        if (storedLine == null || strippedMatchPrefix == null) return false;
        return ChatColor.stripColor(storedLine).startsWith(strippedMatchPrefix);
    }

    public int getLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta() || levelKey == null) return 1;
        return item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(levelKey, PersistentDataType.INTEGER, 1);
    }

    public int getLevel(ItemMeta meta) {
        if (meta == null || levelKey == null) return 1;
        return meta.getPersistentDataContainer()
                .getOrDefault(levelKey, PersistentDataType.INTEGER, 1);
    }

    public void setLevel(ItemMeta meta, int level) {
        if (meta == null || levelKey == null) return;
        meta.getPersistentDataContainer().set(levelKey, PersistentDataType.INTEGER, level);
    }

    public int getMaxLevel() {
        return plugin != null ? plugin.getMaxLevel() : 5;
    }

    /**
     * Rebuild the tracker's counter line using the currently-stored level + total.
     * Used when the level changes but the count does not (e.g. leveling-up on click).
     */
    public ItemStack rewriteTrakerLine(ItemStack item) {
        if (item == null || !item.hasItemMeta() || itemKey == null) return item;
        ItemMeta meta = item.getItemMeta();
        int level = getLevel(meta);
        int total = meta.getPersistentDataContainer().getOrDefault(itemKey, PersistentDataType.INTEGER, 0);
        List<String> lore = meta.getLore();
        if (lore == null) return item;
        String newLine = renderLine(level, total);
        boolean changed = false;
        for (int i = 0; i < lore.size(); i++) {
            if (matchesLine(lore.get(i)) && !newLine.equals(lore.get(i))) {
                lore.set(i, newLine);
                changed = true;
            }
        }
        if (changed) {
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}

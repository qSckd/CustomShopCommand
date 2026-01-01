package dev.customshop;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomShopCommand extends JavaPlugin
        implements CommandExecutor, TabCompleter, Listener {

    private final Map<Integer, String> slotToShop = new HashMap<>();

    @Override
    public void onEnable() {
        PluginCommand command = Objects.requireNonNull(getCommand("shop"));
        command.setExecutor(this);
        command.setTabCompleter(this);

        Bukkit.getPluginManager().registerEvents(this, this);

        slotToShop.put(13, "gear");
        slotToShop.put(14, "crystalpvp");
        slotToShop.put(12, "swordpvp");
        slotToShop.put(15, "potions");
        slotToShop.put(11, "currency");
        slotToShop.put(16, "crops");
        slotToShop.put(10, "specials");

        getLogger().info("CustomShopCommand enabled with GUI and subcommands!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can open the shop.");
            return true;
        }

        if (!player.hasPermission("customshopcommand.use")) {
            player.sendMessage("§cYou don’t have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            openShopGUI(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        int id = getShopId(sub);

        if (id == -1) {
            player.sendMessage("§cUnknown shop: " + sub);
            return true;
        }

        Bukkit.getScheduler().runTask(this, () ->
                Bukkit.dispatchCommand(
                        Bukkit.getConsoleSender(),
                        "shopkeeper remote " + id + " " + player.getName()
                )
        );

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1
                ? Arrays.asList("gear", "crystalpvp", "swordpvp", "potions", "currency", "crops", "specials")
                : Collections.emptyList();
    }

    private void openShopGUI(Player player) {
        Inventory gui = Bukkit.createInventory((InventoryHolder) null, 27, "§7ꜱʜᴏᴘ");

        gui.setItem(13, createGlowingItem(Material.DIAMOND_HELMET, "§dɢᴇᴀʀ",
                List.of("§r§fClick to open the ɢᴇᴀʀ shop")));
        gui.setItem(14, createItem(Material.END_CRYSTAL, "§dᴄʀʏꜱᴛᴀʟ ᴘᴠᴘ",
                List.of("§r§fClick to open the ᴄʀʏꜱᴛᴀʟ ᴘᴠᴘ shop")));
        gui.setItem(12, createItem(Material.DIAMOND_SWORD, "§dꜱᴡᴏʀᴅ ᴘᴠᴘ",
                List.of("§r§fClick to open the ꜱᴡᴏʀᴅ ᴘᴠᴘ shop")));
        gui.setItem(15, createItem(Material.POTION, "§dᴘᴏᴛɪᴏɴꜱ",
                List.of("§r§fClick to open the ᴘᴏᴛɪᴏɴ shop")));
        gui.setItem(11, createItem(Material.NETHERITE_INGOT, "§dᴄᴜʀʀᴇɴᴄʏ",
                List.of("§r§fClick to open the ᴄᴜʀʀᴇɴᴄʏ shop")));
        gui.setItem(16, createItem(Material.WHEAT, "§dᴄʀᴏᴘꜱ",
                List.of("§r§fClick to open the ᴄʀᴏᴘꜱ shop")));
        gui.setItem(10, createItem(Material.AMETHYST_SHARD, "§dꜱᴘᴇᴄɪᴀʟꜱ",
                List.of("§r§fClick to open the ꜱᴘᴇᴄɪᴀʟꜱ shop")));

        player.openInventory(gui);
    }

    private ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createGlowingItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }

        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity clicker = event.getWhoClicked();
        if (!(clicker instanceof Player player)) return;

        if (!event.getView().getTitle().equals("§7ꜱʜᴏᴘ")) return;

        event.setCancelled(true);

        if (event.getClickedInventory() == null) return;
        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        String shop = slotToShop.get(event.getRawSlot());
        if (shop == null) return;

        int id = getShopId(shop);
        if (id == -1) return;

        Bukkit.getScheduler().runTask(this, () ->
                Bukkit.dispatchCommand(
                        Bukkit.getConsoleSender(),
                        "shopkeeper remote " + id + " " + player.getName()
                )
        );

        player.closeInventory();
    }

    private int getShopId(String shop) {
        return switch (shop.toLowerCase()) {
            case "gear" -> 1;
            case "crystalpvp" -> 2;
            case "crops" -> 3;
            case "currency" -> 7;
            case "swordpvp" -> 9;
            case "specials" -> 10;
            case "potions" -> 11;
            default -> -1;
        };
    }
}

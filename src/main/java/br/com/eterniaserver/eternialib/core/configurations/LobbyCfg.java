package br.com.eterniaserver.eternialib.core.configurations;

import br.com.eterniaserver.eternialib.Constants;
import br.com.eterniaserver.eternialib.EterniaLib;
import br.com.eterniaserver.eternialib.core.enums.*;
import br.com.eterniaserver.eternialib.core.interfaces.ReloadableConfiguration;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class LobbyCfg implements ReloadableConfiguration {

    private final NamespacedKey serverKey;

    private final String[] strings;
    private final boolean[] booleans;
    private final int[] integers;

    private final List<ItemStack> itemStacks;

    public LobbyCfg(final EterniaLib plugin, final String[] strings, final boolean[] booleans, final int[] integers, final List<ItemStack> itemStacks) {
        this.serverKey = new NamespacedKey(plugin, "eternialib-lobby");
        this.strings = strings;
        this.booleans = booleans;
        this.integers = integers;
        this.itemStacks = itemStacks;
    }

    @Override
    public ConfigurationCategory category() {
        return ConfigurationCategory.GENERIC;
    }

    @Override
    public void executeConfig() {

        // Load the configuration
        FileConfiguration config = YamlConfiguration.loadConfiguration(new File(Constants.LOBBY_FILE_PATH));

        integers[Integers.GUI_SIZE.ordinal()] = config.getInt("gui-size", 9);

        strings[Strings.SELECT_TITLE_NAME.ordinal()] = config.getString("select-title", "$6$lServidores".replace('$', (char) 0x00A7));
        strings[Strings.PICKUP_PERM.ordinal()] = config.getString("perms.pickup", "eternia.lobby.pickup");
        strings[Strings.DROP_PERM.ordinal()] = config.getString("perms.drop", "eternia.lobby.drop");

        booleans[Booleans.DISABLE_ITEM_PICKUP.ordinal()] = config.getBoolean("disable-item-pickup", true);
        booleans[Booleans.DISABLE_ITEM_DROP.ordinal()] = config.getBoolean("disable-item-drop", true);
        booleans[Booleans.CLEAR_INV.ordinal()] = config.getBoolean("clear-inv", true);
        booleans[Booleans.BLOCK_SWAP_ITEMS.ordinal()] = config.getBoolean("block-swap-items", true);

        itemStacks.clear();

        for (int i = 0; i < integers[Integers.GUI_SIZE.ordinal()]; i++) {

            itemStacks.add(null);
            final String server = config.getString("servers." + i + ".name");

            if (server == null) {
                continue;
            }

            final String itemName = config.getString("servers." + i + ".display-name");
            final List<String> itemLore = config.getStringList("servers." + i + ".lore");

            for (int j = 0; j < itemLore.size(); j++) {
                itemLore.set(0, ChatColor.translateAlternateColorCodes('&', itemLore.get(0)));
            }

            final String materialName = config.getString("servers." + i + ".material");
            final ItemStack slotItem = new ItemStack(Material.getMaterial(materialName));
            final ItemMeta itemMeta = slotItem.getItemMeta();
            itemMeta.getPersistentDataContainer().set(serverKey, PersistentDataType.STRING, server);
            itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', itemName));
            itemMeta.setLore(itemLore);
            slotItem.setItemMeta(itemMeta);
            itemStacks.set(i, slotItem);

        }

        boolean allEmpty = true;

        for (int i = 0; i < integers[Integers.GUI_SIZE.ordinal()]; i++) {
            if (itemStacks.get(i) != null && itemStacks.get(i).getType() != Material.AIR) {
                allEmpty = false;
                break;
            }
        }

        if (allEmpty) {
            final ItemStack slotItem = new ItemStack(Material.GRASS_BLOCK);
            final ItemMeta itemMeta = slotItem.getItemMeta();
            itemMeta.getPersistentDataContainer().set(serverKey, PersistentDataType.STRING, "survival");
            itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aVenha jogar no nosso survival!"));
            slotItem.setItemMeta(itemMeta);
            itemStacks.set(4, slotItem);
        }

        // Save the configuration
        FileConfiguration outConfig = new YamlConfiguration();

        for (int i = 0; i < integers[Integers.GUI_SIZE.ordinal()]; i++) {

            if (itemStacks.get(i) == null) {
                continue;
            }

            final ItemMeta itemMeta = itemStacks.get(i).getItemMeta();

            if (itemMeta == null) {
                outConfig.set("servers." + i + ".name", "error");
                outConfig.set("servers." + i + ".display-name", "error");
                outConfig.set("servers." + i + ".lore", "error");
                outConfig.set("servers." + i + ".material", "error");
                return;
            }

            final String nameItem = itemMeta.getPersistentDataContainer().get(serverKey, PersistentDataType.STRING) != null ?
                    itemMeta.getPersistentDataContainer().get(serverKey, PersistentDataType.STRING) : "error";
            boolean error = true;
            final List<String> stringList;
            try {
                if (itemMeta.getLore() != null) {
                    error = false;
                }
            } catch (NullPointerException ignored) {
            } finally {
                stringList = error ? List.of("error") : itemMeta.getLore();
            }

            outConfig.set("servers." + i + ".name", nameItem);
            outConfig.set("servers." + i + ".display-name", itemMeta.getDisplayName());
            outConfig.set("servers." + i + ".lore", stringList);
            outConfig.set("servers." + i + ".material", itemStacks.get(i).getType().name());

        }

        outConfig.set("select-title", strings[Strings.SELECT_TITLE_NAME.ordinal()]);
        outConfig.set("perms.pickup", strings[Strings.PICKUP_PERM.ordinal()]);
        outConfig.set("perms.drop", strings[Strings.DROP_PERM.ordinal()]);

        outConfig.set("disable-item-pickup", booleans[Booleans.DISABLE_ITEM_PICKUP.ordinal()]);
        outConfig.set("disable-item-drop", booleans[Booleans.DISABLE_ITEM_DROP.ordinal()]);
        outConfig.set("clear-inv", booleans[Booleans.CLEAR_INV.ordinal()]);
        outConfig.set("block-swap-items", booleans[Booleans.BLOCK_SWAP_ITEMS.ordinal()]);

        outConfig.set("gui-size", integers[Integers.GUI_SIZE.ordinal()]);

        try {
            outConfig.save(Constants.LOBBY_FILE_PATH);
        } catch (IOException exception) {
            exception.printStackTrace();
        }

    }

    @Override
    public void executeCritical() { }

}
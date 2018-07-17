package com.enjin.enjincoin.spigot_framework.listeners.notifications;

import com.enjin.enjincoin.sdk.client.service.notifications.vo.NotificationEvent;
import com.enjin.enjincoin.spigot_framework.player.MinecraftPlayer;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.enjin.enjincoin.sdk.client.enums.NotificationType;
import com.enjin.enjincoin.sdk.client.service.identities.vo.Identity;
import com.enjin.enjincoin.sdk.client.service.identities.vo.IdentityField;
import com.enjin.enjincoin.sdk.client.service.identities.vo.data.IdentitiesData;
import com.enjin.enjincoin.sdk.client.service.notifications.NotificationListener;
import com.enjin.enjincoin.sdk.client.service.tokens.vo.Token;
import com.enjin.enjincoin.spigot_framework.BasePlugin;
import com.enjin.enjincoin.spigot_framework.inventory.WalletInventory;
import com.enjin.enjincoin.spigot_framework.util.UuidUtils;
import com.enjin.enjincoin.spigot_framework.util.MessageUtils;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * <p>A listener for handling Enjin Coin SDK events.</p>
 */
// TODO: Refactor around MinecraftPlayer
public class GenericNotificationListener implements NotificationListener {

    /**
     * <p>The spigot plugin.</p>
     */
    private BasePlugin main;

    /**
     * <p>Listener constructor.</p>
     *
     * @param main the Spigot plugin
     */
    public GenericNotificationListener(BasePlugin main) {
        this.main = main;
    }

    @Override
    public void notificationReceived(NotificationEvent event) {
        this.main.getBootstrap().debug(String.format("Received %s event with data: %s", event.getNotificationType().getEventType(), event.getSourceData()));
        if (event.getNotificationType() == NotificationType.TX_EXECUTED) {
            this.main.getBootstrap().debug(String.format("Parsing data for %s event", event.getNotificationType().getEventType()));
            JsonParser parser = new JsonParser();
            String eventType = event.getNotificationType().getEventType();
            JsonObject data = parser.parse(event.getSourceData()).getAsJsonObject().get("data").getAsJsonObject();

            // txr_ => transaction request
            // tx_ => transaction
            if (eventType.equalsIgnoreCase("txr_pending")) {
                this.main.getBootstrap().debug("Transaction is pending");
            } else if (eventType.equalsIgnoreCase("tx_executed")) {
                this.main.getBootstrap().debug("Transaction is executed");
                if (data.get("event") != null && data.get("event").getAsString().equalsIgnoreCase("Transfer")) {
                    // handle transfer event.
                    String fromEthereumAddress = data.get("param1").getAsString();
                    String toEthereumAddress = data.get("param2").getAsString();
                    String tokenId = data.get("token").getAsJsonObject().get("token_id").getAsString();
                    String amount = data.get("param3").getAsString();

                    MinecraftPlayer fromPlayer = null;
                    MinecraftPlayer toPlayer = null;
                    int found = 0;
                    for(Map.Entry<UUID, MinecraftPlayer> entry : this.main.getBootstrap().getPlayerManager().getPlayers().entrySet()) {
                        if (entry.getValue().getIdentity().getEthereumAddress().equals(fromEthereumAddress) && found < 2) {
                            entry.getValue().reloadUser();
                            TextComponent text = TextComponent.of("You have successfully sent ").color(TextColor.GOLD)
                                    .append(TextComponent.of(amount).color(TextColor.GREEN))
                                    .append(TextComponent.of(" " + data.get("token").getAsJsonObject().get("name").getAsString()).color(TextColor.DARK_PURPLE));
                            MessageUtils.sendMessage(entry.getValue().getBukkitPlayer(), text);
                            found++;
                        }

                        if (entry.getValue().getIdentity().getEthereumAddress().equals(toEthereumAddress) && found < 2) {
                            entry.getValue().reloadUser();
                            TextComponent text = TextComponent.of("You have received ").color(TextColor.GOLD)
                                    .append(TextComponent.of(amount).color(TextColor.GREEN))
                                    .append(TextComponent.of(" " + data.get("token").getAsJsonObject().get("name").getAsString()).color(TextColor.DARK_PURPLE));
                            MessageUtils.sendMessage(entry.getValue().getBukkitPlayer(), text);
                            found++;
                        }
                    }
                }

            } else if (eventType.equalsIgnoreCase("txr_canceled_user")) {
                this.main.getBootstrap().debug("Transaction was canceled");

            } else {
                this.main.getBootstrap().debug("Transaction was last in state: " + eventType);
            }
                // Handle melt event.
//                String ethereumAddress = data.get("param1").getAsString();
//                double amount = Double.valueOf(data.get("param2").getAsString());
//                String tokenId = data.get("token").getAsJsonObject().get("token_id").getAsString();
//                int appId = data.get("token").getAsJsonObject().get("app_id").getAsInt();
//
//                this.main.getBootstrap().debug(String.format("%s of token %s was melted by %s", amount, tokenId, ethereumAddress));
//
//                JsonObject config = this.main.getBootstrap().getConfig();
//                if (config.get("appId").getAsInt() == appId) {
//                    this.main.getBootstrap().debug(String.format("Updating balance of player linked to %s", ethereumAddress));
//                    Identity identity = getIdentity(ethereumAddress);
//
//                    if (identity != null)
//                        addTokenValue(identity, tokenId, -amount);
//                }
//            } else if (data.get("event").getAsString().equalsIgnoreCase("transfer")) {
//                // Handle transfer event.
//                String fromEthereumAddress = data.get("param1").getAsString();
//                String toEthereumAddress = data.get("param2").getAsString();
//                double amount = Double.valueOf(data.get("param3").getAsString());
//                String tokenId = data.get("token").getAsJsonObject().get("token_id").getAsString();
//                int appId = data.get("token").getAsJsonObject().get("app_id").getAsInt();
//
//                this.main.getBootstrap().debug(String.format("%s received %s of %s tokens from %s", toEthereumAddress, amount, tokenId, fromEthereumAddress));
//
//                JsonObject config = this.main.getBootstrap().getConfig();
//                if (config.get("appId").getAsInt() == appId) {
//                    this.main.getBootstrap().debug(String.format("Updating balance of player linked to %s", toEthereumAddress));
//                    Identity toIdentity = getIdentity(toEthereumAddress);
//                    Identity fromIdentity = getIdentity(fromEthereumAddress);
//
//                    if (toIdentity != null)
//                        addTokenValue(toIdentity, tokenId, amount);
//                    if (fromIdentity != null)
//                        addTokenValue(fromIdentity, tokenId, -amount);
//                }
//            }
        }
    }

    /**
     * <p>Returns an {@link Identity} of an online player associated with the
     * provided Ethereum address.</p>
     *
     * @param address the Ethereum address
     *
     * @return the identity associated with address or null if no matching identity is found
     *
     * @since 1.0
     */
    public Identity getIdentity(String address) {
//        return this.main.getBootstrap().getIdentities().values().stream()
//                .filter(i -> i != null && i.getEthereumAddress().equalsIgnoreCase(address))
//                .findFirst()
//                .orElse(null);
        return null;
    }

    /**
     * <p>Returns an {@link Token} associated with an {@link Identity}
     * of an online player that matches the provided token ID.</p>
     *
     * @param identity the identity
     * @param tokenId the token ID
     *
     * @return a {@link Token} if present or null if not present
     *
     * @since 1.0
     */
    public Token getTokenEntry(Identity identity, String tokenId) {
        Token entry = null;
        for (Token e : identity.getTokens()) {
            if (e.getTokenId().equals(tokenId)) {
                entry = e;
                break;
            }
        }
        return entry;
    }

    /**
     * <p>Add a value to a {@link Token} for the provided token ID
     * and identity.</p>
     *
     * @param identity the identity
     * @param tokenId the token ID
     * @param amount the amount
     *
     * @since 1.0
     */
    public void addTokenValue(Identity identity, String tokenId, double amount) {
//        Token entry = getTokenEntry(identity, tokenId);
//        if (entry != null)
//            entry.setBalance(entry.getBalance() + amount);
//        else {
//            List<Token> entries = new ArrayList<Token>(identity.getTokens());
//            Token token = new Token();
//            token.setTokenId(tokenId);
//            token.setBalance(amount);
//            entries.add(token);
//            identity.setTokens(entries);
//        }
//
//        updateInventory(identity, tokenId, amount);
    }

    /**
     * <p>Updates the inventory associated with an identity where a
     * menu item represents a token with the provided ID to the
     * specified amount.</p>
     *
     * @param identity the identity
     * @param tokenId the token ID
     * @param amount the amount
     */
    public void updateInventory(Identity identity, String tokenId, double amount) {
        JsonObject config = main.getBootstrap().getConfig();

        String displayName = null;
        if (config.has("tokens")) {
            JsonObject tokens = config.getAsJsonObject("tokens");
            if (tokens.has(tokenId)) {
                JsonObject token = tokens.getAsJsonObject(tokenId);
                if (token.has("displayName")) {
                    displayName = token.get("displayName").getAsString();
                } else {
                    Token spec = main.getBootstrap().getTokens().get(tokenId);
                    if (spec != null) {
                        if (spec.getName() != null) {
                            displayName = spec.getName();
                        } else {
                            displayName = "Token #" + tokenId;
                        }
                    }
                }
            }
        }

        if (displayName != null) {
            UUID uuid = null;
            for (IdentityField field : identity.getFields()) {
                if (field.getKey().equalsIgnoreCase("uuid")) {
                    uuid = UuidUtils.stringToUuid(field.getFieldValue());
                    break;
                }
            }

            Player player = null;
            if (uuid != null) {
                player = Bukkit.getPlayer(uuid);
            }

            if (player != null) {
                InventoryView view = player.getOpenInventory();
                if (view != null && ChatColor.stripColor(view.getTitle()).equalsIgnoreCase("Enjin Wallet")) {
                    ItemStack stack = null;
                    ItemMeta meta = null;
                    int i;
                    for (i = 0; i < 6 * 9; i++) {
                        stack = view.getItem(i++);
                        if (stack != null) {
                            meta = stack.getItemMeta();
                            if (ChatColor.stripColor(meta.getDisplayName()).equalsIgnoreCase(displayName))
                                break;
                        }
                        stack = null;
                        meta = null;
                    }

                    if (stack == null) {
                        if (config.has("tokens")) {
                            JsonObject tokens = config.getAsJsonObject("tokens");
                            if (tokens.has(tokenId)) {
                                JsonObject tokenDisplay = config.getAsJsonObject(tokenId);
                                Token token = main.getBootstrap().getTokens().get(tokenId);
                                if (token != null) {
                                    Material material = null;
                                    if (tokenDisplay.has("material"))
                                        material = Material.getMaterial(tokenDisplay.get("material").getAsString());
                                    if (material == null)
                                        material = Material.APPLE;

                                    stack = new ItemStack(material);
                                    meta = stack.getItemMeta();

                                    if (tokenDisplay.has("displayName")) {
                                        meta.setDisplayName(ChatColor.DARK_PURPLE + tokenDisplay.get("displayName").getAsString());
                                    } else {
                                        if (token.getName() != null)
                                            meta.setDisplayName(ChatColor.DARK_PURPLE + token.getName());
                                        else
                                            meta.setDisplayName(ChatColor.DARK_PURPLE + "Token #" + token.getTokenId());
                                    }

                                    List<String> lore = new ArrayList<>();
                                    if (token.getDecimals() == 0) {
                                        int balance = Double.valueOf(amount).intValue();
                                        lore.add(ChatColor.GRAY + "Balance: " + ChatColor.GOLD + balance);
                                    } else {
                                        lore.add(ChatColor.GRAY + "Balance: " + ChatColor.GOLD + WalletInventory.DECIMAL_FORMAT.format(amount));
                                    }

                                    if (tokenDisplay.has("lore")) {
                                        JsonElement element = tokenDisplay.get("lore");
                                        if (element.isJsonArray()) {
                                            JsonArray array = element.getAsJsonArray();
                                            for (JsonElement line : array) {
                                                lore.add(ChatColor.DARK_GRAY + line.getAsString());
                                            }
                                        } else {
                                            lore.add(ChatColor.DARK_GRAY + element.getAsString());
                                        }
                                    }

                                    meta.setLore(lore);
                                    stack.setItemMeta(meta);
                                    view.setItem(i - 1, stack);
                                }
                            }
                        }
                    } else {
                        List<String> lore = meta.getLore();
                        String value = ChatColor.stripColor(lore.get(0)).replace("Balance: ", "");
                        if (value.contains(".")) {
                            Double val = Double.valueOf(value) + amount;
                            lore.set(0, ChatColor.GRAY + "Balance: " + ChatColor.GOLD + WalletInventory.DECIMAL_FORMAT.format(val));
                        } else {
                            Integer val = Double.valueOf(value).intValue() + Double.valueOf(amount).intValue();
                            lore.set(0, ChatColor.GRAY + "Balance: " + ChatColor.GOLD + val);
                        }
                        meta.setLore(lore);
                        stack.setItemMeta(meta);
                        view.setItem(i - 1, stack);
                    }

                    player.updateInventory();
                }
            }
        }
    }

}

package ru.armagidon.achievementignore;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public final class AchievementIgnore extends JavaPlugin implements Listener {

    private final Set<String> ignore = new HashSet<>();

    private FileConfiguration storage;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
        getDataFolder().mkdirs();
        File file = new File(getDataFolder(), "data.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        storage = YamlConfiguration.loadConfiguration(file);
        deserialize();
        PluginCommand command = getCommand("ignoreachievements");
        if (command != null) {
            command.setTabCompleter((sender, command1, alias, args) -> new ArrayList<>());
            command.setExecutor(this);
        }

        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(this, ListenerPriority.HIGHEST, PacketType.Play.Server.CHAT) {
                    @Override
                    public void onPacketSending(PacketEvent event) {
                        if (!event.getPacketType().equals(PacketType.Play.Server.CHAT)) return;
                        WrappedChatComponent c = event.getPacket().getChatComponents().read(0);
                        if (c == null) return;
                        String json = c.getJson();
                        if (json == null) return;
                        JsonObject element = new JsonParser().parse(json).getAsJsonObject();
                        JsonElement trEl = element.get("translate");
                        if (trEl == null)
                            return;
                        String tr = trEl.getAsString();
                        if (tr != null && (tr.startsWith("chat.type.advancement") || tr.contains("has made the advancement") || tr.contains("has reached the goal"))) {
                            JsonArray arr = element.getAsJsonArray("with");
                            JsonObject o = arr.get(0).getAsJsonObject();
                            String inse = o.get("insertion").getAsString();
                            if (!inse.equals(event.getPlayer().getName())) {
                                event.setCancelled(true);
                            }
//                    if(!recipients.contains(event.getPlayer())){
//                        event.setCancelled(true);
//                    }
                        }
            }
        });

    }

    private void deserialize() {

        if (storage != null) {
            ignore.addAll(storage.getStringList("players"));
        }

    }

    @Override
    public void onDisable() {
        storage.set("players", new ArrayList<>(ignore));
        try {
            storage.save(new File(getDataFolder(), "data.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        ProtocolLibrary.getProtocolManager().removePacketListeners(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        if (ignore.contains(player.getName())) {
            ignore.remove(player.getName());
            String msg = getConfig().getString("unignore", "unignore");
            sender.sendMessage(color.apply(msg));
        } else {
            String msg = getConfig().getString("ignore", "ignore");
            sender.sendMessage(color.apply(msg));
            ignore.add(player.getName());
        }
        return true;
    }

    private Function<String, String> color = (s) -> ChatColor.translateAlternateColorCodes('&', s);

}

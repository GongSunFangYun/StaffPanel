package cn.gsfy;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerChatEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.Player;
import cn.nukkit.utils.TextFormat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class StaffEventHandler implements Listener {

    private final StaffMain plugin;

    public StaffEventHandler(StaffMain plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName().toLowerCase();

        if (plugin.getBannedConfig().exists(playerName)) {
            Map<String, Object> banData = plugin.getBannedConfig().get(playerName, new HashMap<>());
            String endTimeStr = (String) banData.getOrDefault("endTime", "Unknown");

            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date endTime = sdf.parse(endTimeStr);
                if (endTime.getTime() <= System.currentTimeMillis()) {
                    plugin.getBannedConfig().remove(playerName.toLowerCase());
                    plugin.getBannedConfig().save();
                    return;
                }
            } catch (Exception e) {
                plugin.getLogger().error("Error parsing ban end time for " + playerName, e);
            }

            String reason = (String) banData.getOrDefault("reason", "No reason specified");
            player.kick(TextFormat.RED + "\nYOU HAVE BEEN BANNED FROM THIS SERVER!"
                    + TextFormat.RED + "\nReason: "
                    + TextFormat.WHITE + reason
                    + TextFormat.RED + "\nUntil: "
                    + TextFormat.WHITE + endTimeStr);
        }
    }

    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName().toLowerCase();

        if (plugin.getMutedConfig().exists(playerName)) {
            Map<String, Object> muteData = plugin.getMutedConfig().get(playerName, new HashMap<>());
            String endTimeStr = (String) muteData.getOrDefault("endTime", "Unknown");

            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date endTime = sdf.parse(endTimeStr);
                if (endTime.getTime() <= System.currentTimeMillis()) {
                    plugin.getMutedConfig().remove(playerName.toLowerCase());
                    plugin.getMutedConfig().save();
                    return; // Mute expired, allow chat
                }
            } catch (Exception e) {
                plugin.getLogger().error("Error parsing mute end time for " + playerName, e);
            }

            String reason = (String) muteData.getOrDefault("reason", "No reason specified");
            event.setCancelled(true);
            player.sendMessage("------------------------------"
                    + TextFormat.RED
                    + "\nYOU HAVE BEEN MUTED ON THIS SERVER!" + TextFormat.RED + "\nReason: "
                    + TextFormat.WHITE + reason + TextFormat.RED
                    + "\nMute expires: " + TextFormat.WHITE + endTimeStr
                    + TextFormat.RESET + "\n------------------------------");
        }
    }
}
package cn.gsfy;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import cn.nukkit.potion.Effect;
import cn.nukkit.utils.TextFormat;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@SuppressWarnings("SameReturnValue")
public class StaffCommandParser implements CommandExecutor {

    private final StaffMain plugin;

    public StaffCommandParser(StaffMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!command.getName().equalsIgnoreCase("staff")) {
            return false;
        }

        if (args.length == 0) {
            if (sender instanceof Player player) {
                if (player.hasPermission("staff.admin")) {
                    plugin.getFormManager().openAdminPanelForm(player);
                } else if (player.hasPermission("staff.use")) {
                    plugin.getFormManager().openPlayerSelectionForm(player);
                } else {
                    player.sendMessage(TextFormat.RED + "You don't have permission to use this command!");
                }
            } else {
                sender.sendMessage("§6|| §bStaff§3Panel §9v1.3.3");
                sender.sendMessage("§6|| §aUse /staff help to get help.");
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (args.length == 1 && sender instanceof Player) {
            String inputName = args[0];
            String matchedName = findBestPlayerMatchIncludingAll(inputName);
            plugin.getFormManager().openPlayerActionForm((Player) sender, matchedName);
            return true;
        }

        try {
            return switch (subCommand) {
                case "ban" -> handleBan(sender, args);
                case "unban" -> handleUnban(sender, args);
                case "mute" -> handleMute(sender, args);
                case "unmute" -> handleUnmute(sender, args);
                case "kill" -> handleKill(sender, args);
                case "kick" -> handleKick(sender, args);
                case "warn" -> handleWarn(sender, args);
                case "tp" -> handleTp(sender, args);
                case "weaken" -> handleWeaken(sender, args);
                case "log" -> handleLog(sender, args);
                case "check" -> handleCheck(sender, args);
                case "help" -> {
                    sendUsage(sender);
                    yield true;
                }
                default -> {
                    sender.sendMessage(TextFormat.RED + "Unknown command. Use /staff help for help.");
                    yield true;
                }
            };
        } catch (Exception e) {
            sender.sendMessage(TextFormat.RED + "An error occurred: " + e.getMessage());
            plugin.getLogger().error("Error executing staff command", e);
            return true;
        }
    }

    public void logAction(String action, CommandSender executor, String target) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String logEntry = String.format("[%s] [%s] %s --> %s",
                sdf.format(new Date()),
                action,
                executor.getName(),
                target);

        plugin.getLogs().add(logEntry);

        try (FileWriter writer = new FileWriter(plugin.getLogFile(), true)) {
            writer.write(logEntry + "\n");
        } catch (IOException e) {
            plugin.getLogger().error("Could not write to log file", e);
        }
    }

    public void handleBanForm(CommandSender sender, String playerName, String reason, String duration) {
        if (!sender.hasPermission("staff.ban")) {
            sender.sendMessage(TextFormat.RED + "You don't have permission to ban players.");
            return;
        }

        if (duration == null || duration.trim().isEmpty()) {
            duration = "10y";
        }

        long durationMillis = parseDuration(duration);
        if (durationMillis <= 0) {
            sender.sendMessage(TextFormat.RED + "Invalid duration format. Use m (minutes), h (hours), d (days), or y (years)");
            return;
        }

        long endTime = System.currentTimeMillis() + durationMillis;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String endTimeStr = sdf.format(new Date(endTime));

        Map<String, Object> banData = new LinkedHashMap<>();
        banData.put("name", playerName);
        banData.put("reason", reason);
        banData.put("duration", duration);
        banData.put("executor", sender.getName());
        banData.put("endTime", endTimeStr);

        plugin.getBannedConfig().set(playerName.toLowerCase(), banData);
        plugin.getBannedConfig().save();

        Player target = sender.getServer().getPlayerExact(playerName);
        if (target != null) {
            target.kick(TextFormat.RED + "\nYOU HAVE BEEN BANNED FROM THIS SERVER!"
                    + TextFormat.RED + "\nReason: "
                    + TextFormat.WHITE + reason
                    + TextFormat.RED + "\nUntil: "
                    + TextFormat.WHITE + endTimeStr);
        }

        sender.sendMessage(TextFormat.GREEN
                + "Successfully banned " + TextFormat.GOLD
                + playerName + TextFormat.GREEN
                + " for " + TextFormat.YELLOW
                + duration + TextFormat.GREEN
                + "\nReason: " + TextFormat.WHITE
                + reason);

        logAction("BAN", sender, playerName);
    }

    public void handleMuteForm(CommandSender sender, String playerName, String reason, String duration) {
        if (!sender.hasPermission("staff.mute")) {
            sender.sendMessage(TextFormat.RED + "You don't have permission to mute players.");
            return;
        }

        // 如果duration为空或null，使用默认值"1h"
        if (duration == null || duration.trim().isEmpty()) {
            duration = "1h";
        }

        long durationMillis = parseDuration(duration);
        if (durationMillis <= 0) {
            sender.sendMessage(TextFormat.RED + "Invalid duration format. Use m (minutes), h (hours), d (days), or y (years)");
            return;
        }

        long endTime = System.currentTimeMillis() + durationMillis;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String endTimeStr = sdf.format(new Date(endTime));

        Map<String, Object> muteData = new LinkedHashMap<>();
        muteData.put("name", playerName);
        muteData.put("reason", reason);
        muteData.put("duration", duration);
        muteData.put("executor", sender.getName());
        muteData.put("endTime", endTimeStr);

        plugin.getMutedConfig().set(playerName.toLowerCase(), muteData);
        plugin.getMutedConfig().save();

        Player target = sender.getServer().getPlayerExact(playerName);
        if (target != null) {
            target.sendMessage("------------------------------"
                    + TextFormat.RED
                    + "\nYOU HAVE BEEN MUTED ON THIS SERVER!" + TextFormat.RED + "\nReason: "
                    + TextFormat.WHITE + reason + TextFormat.RED
                    + "\nMute expires: " + TextFormat.WHITE + endTimeStr
                    + TextFormat.RESET + "\n------------------------------");
        }

        sender.sendMessage(TextFormat.GREEN + "Successfully muted "
                + TextFormat.GOLD + playerName
                + TextFormat.GREEN + " for "
                + TextFormat.YELLOW + duration
                + TextFormat.GREEN
                + "\nReason: "
                + TextFormat.WHITE + reason);

        logAction("MUTE", sender, playerName);
    }

    public void handleKickForm(CommandSender sender, String playerName, String reason) {
        if (!sender.hasPermission("staff.kick")) {
            sender.sendMessage(TextFormat.RED + "You don't have permission to kick players.");
            return;
        }

        Player target = sender.getServer().getPlayerExact(playerName);
        if (target == null) {
            sender.sendMessage(TextFormat.RED + "Player " + TextFormat.GOLD + playerName + TextFormat.RED + " not found.");
            return;
        }

        target.kick(TextFormat.RED + "\nYOU HAVE BEEN KICKED FROM THIS SERVER!" + TextFormat.RED + "\nReason: " + TextFormat.WHITE + reason);
        sender.sendMessage(TextFormat.GREEN + "Successfully kicked " + TextFormat.GOLD
                + playerName + TextFormat.GREEN
                + "\nReason: "
                + TextFormat.WHITE + reason);
        logAction("KICK", sender, playerName);
    }

    public void handleKillForm(CommandSender sender, String playerName) {
        if (!sender.hasPermission("staff.kill")) {
            sender.sendMessage(TextFormat.RED + "You don't have permission to kill players.");
            return;
        }

        Player target = sender.getServer().getPlayerExact(playerName);
        if (target == null) {
            sender.sendMessage(TextFormat.RED + "Player " + TextFormat.GOLD + playerName + TextFormat.RED + " not found.");
            return;
        }

        target.setHealth(0);
        sender.sendMessage(TextFormat.GREEN + "Successfully killed " + TextFormat.GOLD + playerName);
        logAction("KILL", sender, playerName);
    }

    public void handleWarnForm(CommandSender sender, String playerName, String message) {
        if (!sender.hasPermission("staff.warn")) {
            sender.sendMessage(TextFormat.RED + "You don't have permission to warn players.");
            return;
        }

        Player target = sender.getServer().getPlayerExact(playerName);
        if (target == null) {
            sender.sendMessage(TextFormat.RED + "Player " + TextFormat.GOLD + playerName + TextFormat.RED + " not found.");
            return;
        }

        target.sendTitle(TextFormat.RED + "Warning!", message, 10, 70, 20);
        sender.sendMessage(TextFormat.GREEN + "Successfully warned "
                + TextFormat.GOLD + playerName
                + TextFormat.GREEN + " warned with message: "
                + TextFormat.WHITE + message
        );
        logAction("WARN", sender, playerName);
    }

    public void handleTpForm(Player staff, String playerName) {
        if (!staff.hasPermission("staff.tp")) {
            staff.sendMessage(TextFormat.RED + "You don't have permission to teleport to players.");
            return;
        }

        Player target = staff.getServer().getPlayerExact(playerName);
        if (target == null) {
            staff.sendMessage(TextFormat.RED + "Player " + TextFormat.GOLD + playerName + TextFormat.RED + " not found.");
            return;
        }

        staff.teleport(target);
        staff.setGamemode(3); // Spectator mode
        staff.sendMessage(TextFormat.GREEN + "Teleported to " + TextFormat.GOLD + playerName + TextFormat.GREEN + " and entered spectator mode");
        logAction("TELEPORT", staff, playerName);
    }

    public void handleWeakenForm(CommandSender sender, String playerName) {
        if (!sender.hasPermission("staff.weaken")) {
            sender.sendMessage(TextFormat.RED + "You don't have permission to weaken players.");
            return;
        }

        Player target = sender.getServer().getPlayerExact(playerName);
        if (target == null) {
            sender.sendMessage(TextFormat.RED + "Player " + TextFormat.GOLD + playerName + TextFormat.RED + " not found.");
            return;
        }

        target.addEffect(Effect.getEffect(Effect.WEAKNESS).setAmplifier(255).setDuration(Integer.MAX_VALUE));
        target.addEffect(Effect.getEffect(Effect.BLINDNESS).setAmplifier(255).setDuration(Integer.MAX_VALUE));
        target.addEffect(Effect.getEffect(Effect.SLOWNESS).setAmplifier(2).setDuration(Integer.MAX_VALUE));

        sender.sendMessage(TextFormat.GREEN + "Successfully weakened " + TextFormat.GOLD + playerName);
        logAction("WEAKEN", sender, playerName);
    }

    private boolean handleBan(CommandSender sender, String[] args) {
        if (!sender.hasPermission("staff.ban")) {
            sender.sendMessage(TextFormat.RED + "You don't have permission to ban players.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(TextFormat.RED + "Usage: /staff ban <player> <reason> [duration]");
            return true;
        }

        String playerName = args[1];

        String matchedName = findBestPlayerMatchIncludingBanned(playerName);
        if (matchedName != null) {
            playerName = matchedName;
        }

        if (!playerName.matches("^[a-zA-Z0-9]+$")) {
            sender.sendMessage(TextFormat.RED + "Invalid player name. Only letters and numbers are allowed.");
            return true;
        }

        String reason = args[2];
        String duration = args.length > 3 ? args[3] : "10y";

        long durationMillis = parseDuration(duration);
        if (durationMillis <= 0) {
            sender.sendMessage(TextFormat.RED + "Invalid duration format. Use m (minutes), h (hours), d (days), or y (years)");
            return true;
        }

        long endTime = System.currentTimeMillis() + durationMillis;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String endTimeStr = sdf.format(new Date(endTime));

        Map<String, Object> banData = new LinkedHashMap<>();
        banData.put("name", playerName);
        banData.put("reason", reason);
        banData.put("duration", duration);
        banData.put("executor", sender.getName());
        banData.put("endTime", endTimeStr);

        plugin.getBannedConfig().set(playerName.toLowerCase(), banData);
        plugin.getBannedConfig().save();

        // 如果玩家在线，则踢出
        Player target = sender.getServer().getPlayerExact(playerName);
        if (target != null) {
            target.kick(TextFormat.RED + "\nYOU HAVE BEEN BANNED FROM THIS SERVER!"
                    + TextFormat.RED + "\nReason: "
                    + TextFormat.WHITE + reason
                    + TextFormat.RED + "\nUntil: "
                    + TextFormat.WHITE + endTimeStr);
        }

        sender.sendMessage(TextFormat.GREEN
                + "Successfully banned " + TextFormat.GOLD
                + playerName + TextFormat.GREEN
                + " for " + TextFormat.YELLOW
                + duration + TextFormat.GREEN
                + "\nReason: " + TextFormat.WHITE
                + reason);

        logAction("BAN", sender, playerName);
        return true;
    }

    private boolean handleUnban(CommandSender sender, String[] args) {
        if (!sender.hasPermission("staff.unban")) {
            sender.sendMessage(TextFormat.RED + "You don't have permission to unban players.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(TextFormat.RED + "Usage: /staff unban <player>");
            return true;
        }

        String playerName = args[1];

        String matchedName = findBestPlayerMatchIncludingBanned(playerName);
        if (matchedName != null) {
            playerName = matchedName;
        }

        if (!plugin.getBannedConfig().exists(playerName.toLowerCase())) {
            sender.sendMessage(TextFormat.GOLD + playerName + TextFormat.RED + " is not banned.");
            return true;
        }

        plugin.getBannedConfig().remove(playerName.toLowerCase());
        plugin.getBannedConfig().save();

        sender.sendMessage(TextFormat.GREEN + "Successfully unbanned " + TextFormat.GOLD + playerName);
        logAction("UNBAN", sender, playerName);
        return true;
    }

    private boolean handleMute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("staff.mute")) {
            sender.sendMessage(TextFormat.RED + "You don't have permission to mute players.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(TextFormat.RED + "Usage: /staff mute <player> <reason> [duration]");
            return true;
        }

        String playerName = findBestPlayerMatch(args[1]);
        if (playerName == null) {
            sender.sendMessage(TextFormat.RED + "Player " + TextFormat.GOLD + args[1] + TextFormat.RED + " not found.");
            return true;
        }

        String reason = args[2];
        String duration = args.length > 3 ? args[3] : "1h";

        long durationMillis = parseDuration(duration);
        if (durationMillis <= 0) {
            sender.sendMessage(TextFormat.RED + "Invalid duration format. Use m (minutes), h (hours), d (days), or y (years)");
            return true;
        }

        long endTime = System.currentTimeMillis() + durationMillis;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String endTimeStr = sdf.format(new Date(endTime));

        Map<String, Object> muteData = new LinkedHashMap<>();
        muteData.put("name", playerName);
        muteData.put("reason", reason);
        muteData.put("duration", duration);
        muteData.put("executor", sender.getName());
        muteData.put("endTime", endTimeStr);

        plugin.getMutedConfig().set(playerName.toLowerCase(), muteData);
        plugin.getMutedConfig().save();

        Player target = sender.getServer().getPlayerExact(playerName);
        if (target != null) {
            target.sendMessage("------------------------------"
                    + TextFormat.RED
                    + "\nYOU HAVE BEEN MUTED ON THIS SERVER!" + TextFormat.RED + "\nReason: "
                    + TextFormat.WHITE + reason + TextFormat.RED
                    + "\nMute expires: " + TextFormat.WHITE + endTimeStr
                    + TextFormat.RESET + "\n------------------------------");
        }

        sender.sendMessage(TextFormat.GREEN + "Successfully muted "
                + TextFormat.GOLD + playerName
                + TextFormat.GREEN + " for "
                + TextFormat.YELLOW + duration
                + TextFormat.GREEN
                + "\nReason: "
                + TextFormat.WHITE + reason);

        logAction("MUTE", sender, playerName);
        return true;
    }

    private boolean handleUnmute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("staff.unmute")) {
            sender.sendMessage(TextFormat.RED + "You don't have permission to unmute players.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(TextFormat.RED + "Usage: /staff unmute <player>");
            return true;
        }

        String playerName = args[1];

        String matchedName = findBestPlayerMatchIncludingMuted(playerName);
        if (matchedName != null) {
            playerName = matchedName;
        }

        if (!plugin.getMutedConfig().exists(playerName.toLowerCase())) {
            sender.sendMessage(TextFormat.GOLD + playerName + TextFormat.RED + " is not muted.");
            return true;
        }

        plugin.getMutedConfig().remove(playerName.toLowerCase());
        plugin.getMutedConfig().save();

        Player target = sender.getServer().getPlayerExact(playerName);
        if (target != null) {
            target.sendMessage(TextFormat.GREEN + "You have been unmuted!");
        }

        sender.sendMessage(TextFormat.GREEN + "Successfully unmuted " + TextFormat.GOLD + playerName);
        logAction("UNMUTE", sender, playerName);
        return true;
    }

    private boolean handleCheck(CommandSender sender, String[] args) {
        if (!sender.hasPermission("staff.check")) {
            sender.sendMessage(TextFormat.RED + "You don't have permission to check player records.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(TextFormat.RED + "Usage: /staff check <player>");
            return true;
        }

        String playerName = args[1].toLowerCase();
        boolean hasBan = plugin.getBannedConfig().exists(playerName);
        boolean hasMute = plugin.getMutedConfig().exists(playerName);

        if (!hasBan && !hasMute) {
            sender.sendMessage(TextFormat.RED + "Player " + TextFormat.GOLD + args[1] + TextFormat.RED + " has no ban or mute records.");
            return true;
        }

        sender.sendMessage(TextFormat.YELLOW + "===== Query Results =====");
        sender.sendMessage(TextFormat.GOLD + "TargetPlayer " + TextFormat.WHITE + "-> " + TextFormat.GREEN + args[1]);

        if (hasBan) {
            Map<String, Object> banData = plugin.getBannedConfig().get(playerName, new HashMap<>());
            String banReason = (String) banData.getOrDefault("reason", "No reason specified");
            String banSource = (String) banData.getOrDefault("executor", "Unknown");
            String banEndTime = (String) banData.getOrDefault("endTime", "Permanent");

            sender.sendMessage(TextFormat.GOLD + "IsBanned " + TextFormat.WHITE + "-> " + TextFormat.RED + "True");
            sender.sendMessage(TextFormat.GOLD + "BanReason " + TextFormat.WHITE + "-> " + TextFormat.GREEN + banReason);
            sender.sendMessage(TextFormat.GOLD + "BanSource " + TextFormat.WHITE + "-> " + TextFormat.GREEN + banSource);
            sender.sendMessage(TextFormat.GOLD + "Until " + TextFormat.WHITE + "-> " + TextFormat.GREEN + banEndTime);
        } else {
            sender.sendMessage(TextFormat.GOLD + "IsBanned " + TextFormat.WHITE + "-> " + TextFormat.LIGHT_PURPLE + "False");
        }

        if (hasMute) {
            Map<String, Object> muteData = plugin.getMutedConfig().get(playerName, new HashMap<>());
            String muteReason = (String) muteData.getOrDefault("reason", "No reason specified");
            String muteSource = (String) muteData.getOrDefault("executor", "Unknown");
            String muteEndTime = (String) muteData.getOrDefault("endTime", "Permanent");

            sender.sendMessage(TextFormat.GOLD + "IsMuted " + TextFormat.WHITE + "-> " + TextFormat.RED + "True");
            sender.sendMessage(TextFormat.GOLD + "MuteReason " + TextFormat.WHITE + "-> " + TextFormat.GREEN + muteReason);
            sender.sendMessage(TextFormat.GOLD + "MuteSource " + TextFormat.WHITE + "-> " + TextFormat.GREEN + muteSource);
            sender.sendMessage(TextFormat.GOLD + "Until " + TextFormat.WHITE + "-> " + TextFormat.GREEN + muteEndTime);
        } else {
            sender.sendMessage(TextFormat.GOLD + "IsMuted " + TextFormat.WHITE + "-> " + TextFormat.LIGHT_PURPLE + "False");
        }

        return true;
    }

    private boolean handleKill(CommandSender sender, String[] args) {
        if (!sender.hasPermission("staff.kill")) {
            sender.sendMessage(TextFormat.RED + "You don't have permission to kill players.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(TextFormat.RED + "Usage: /staff kill <player>");
            return true;
        }

        String playerName = findBestPlayerMatch(args[1]);
        if (playerName == null) {
            sender.sendMessage(TextFormat.RED + "Player " + TextFormat.GOLD + args[1] + TextFormat.RED + " not found.");
            return true;
        }

        Player target = sender.getServer().getPlayerExact(playerName);
        if (target == null) {
            sender.sendMessage(TextFormat.RED + "Player " + TextFormat.GOLD + playerName + TextFormat.RED + " not found.");
            return true;
        }

        target.setHealth(0);
        sender.sendMessage(TextFormat.GREEN + "Successfully killed " + TextFormat.GOLD + playerName);
        logAction("KILL", sender, playerName);
        return true;
    }

    private boolean handleKick(CommandSender sender, String[] args) {
        if (!sender.hasPermission("staff.kick")) {
            sender.sendMessage(TextFormat.RED + "You don't have permission to kick players.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(TextFormat.RED + "Usage: /staff kick <player> <reason>");
            return true;
        }

        String playerName = findBestPlayerMatch(args[1]);
        if (playerName == null) {
            sender.sendMessage(TextFormat.RED + "Player " + TextFormat.GOLD + args[1] + TextFormat.RED + " not found.");
            return true;
        }

        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            reasonBuilder.append(args[i]).append(" ");
        }
        String reason = reasonBuilder.toString().trim();

        Player target = sender.getServer().getPlayerExact(playerName);
        if (target == null) {
            sender.sendMessage(TextFormat.RED + "Player " + TextFormat.GOLD + playerName + TextFormat.RED + " not found.");
            return true;
        }

        target.kick(TextFormat.RED + "\nYOU HAVE BEEN KICKED FROM THIS SERVER!" + TextFormat.RED + "\nReason: " + TextFormat.WHITE + reason);
        sender.sendMessage(TextFormat.GREEN + "Successfully kicked " + TextFormat.GOLD
                + playerName + TextFormat.GREEN
                + "\nReason: "
                + TextFormat.WHITE + reason);
        logAction("KICK", sender, playerName);
        return true;
    }

    private boolean handleWarn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("staff.warn")) {
            sender.sendMessage(TextFormat.RED + "You don't have permission to warn players.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(TextFormat.RED + "Usage: /staff warn <player> <message>");
            return true;
        }

        String playerName = findBestPlayerMatch(args[1]);
        if (playerName == null) {
            sender.sendMessage(TextFormat.RED + "Player " + TextFormat.GOLD + args[1] + TextFormat.RED + " not found.");
            return true;
        }

        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            messageBuilder.append(args[i]).append(" ");
        }
        String message = messageBuilder.toString().trim();

        Player target = sender.getServer().getPlayerExact(playerName);
        if (target == null) {
            sender.sendMessage(TextFormat.RED + "Player " + TextFormat.GOLD + playerName + TextFormat.RED + " not found.");
            return true;
        }

        target.sendTitle(TextFormat.RED + "警告！", message, 10, 70, 20);
        sender.sendMessage(TextFormat.GREEN + "Successfully warned "
                + TextFormat.GOLD + playerName
                + TextFormat.GREEN + " warned with message: "
                + TextFormat.WHITE + message
        );
        logAction("WARN", sender, playerName);
        return true;
    }

    private boolean handleTp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("staff.tp")) {
            sender.sendMessage(TextFormat.RED + "You don't have permission to teleport to players.");
            return true;
        }

        if (!(sender instanceof Player staff)) {
            sender.sendMessage(TextFormat.RED + "This command can only be used by players.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(TextFormat.RED + "Usage: /staff tp <player>");
            return true;
        }

        String playerName = findBestPlayerMatch(args[1]);
        if (playerName == null) {
            sender.sendMessage(TextFormat.RED + "Player " + TextFormat.GOLD + args[1] + TextFormat.RED + " not found.");
            return true;
        }

        Player target = sender.getServer().getPlayerExact(playerName);
        if (target == null) {
            sender.sendMessage(TextFormat.RED + "Player " + TextFormat.GOLD + playerName + TextFormat.RED + " not found.");
            return true;
        }

        staff.teleport(target);
        staff.setGamemode(3); // Spectator mode
        staff.sendMessage(TextFormat.GREEN + "Teleported to " + TextFormat.GOLD + playerName + TextFormat.GREEN + " and entered spectator mode");
        logAction("TELEPORT", sender, playerName);
        return true;
    }

    private boolean handleWeaken(CommandSender sender, String[] args) {
        if (!sender.hasPermission("staff.weaken")) {
            sender.sendMessage(TextFormat.RED + "You don't have permission to weaken players.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(TextFormat.RED + "Usage: /staff weaken <player>");
            return true;
        }

        String playerName = findBestPlayerMatch(args[1]);
        if (playerName == null) {
            sender.sendMessage(TextFormat.RED + "Player " + TextFormat.GOLD + args[1] + TextFormat.RED + " not found.");
            return true;
        }

        Player target = sender.getServer().getPlayerExact(playerName);
        if (target == null) {
            sender.sendMessage(TextFormat.RED + "Player " + TextFormat.GOLD + playerName + TextFormat.RED + " not found.");
            return true;
        }

        target.addEffect(Effect.getEffect(Effect.WEAKNESS).setAmplifier(255).setDuration(Integer.MAX_VALUE));
        target.addEffect(Effect.getEffect(Effect.BLINDNESS).setAmplifier(255).setDuration(Integer.MAX_VALUE));
        target.addEffect(Effect.getEffect(Effect.SLOWNESS).setAmplifier(2).setDuration(Integer.MAX_VALUE));

        sender.sendMessage(TextFormat.GREEN + "Successfully weakened " + TextFormat.GOLD + playerName);
        logAction("WEAKEN", sender, playerName);
        return true;
    }

    private boolean handleLog(CommandSender sender, String[] args) {
        if (!sender.hasPermission("staff.log")) {
            sender.sendMessage(TextFormat.RED + "You don't have permission to view logs.");
            return true;
        }

        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(TextFormat.RED + "Invalid page number.");
                return true;
            }
        }

        int totalPages = (int) Math.ceil((double) plugin.getLogs().size() / plugin.getLogsPerPage());
        if (page < 1 || page > totalPages) {
            sender.sendMessage(TextFormat.RED + "Page number out of range (1-" + totalPages + ")");
            return true;
        }

        int start = (page - 1) * plugin.getLogsPerPage();
        int end = Math.min(start + plugin.getLogsPerPage(), plugin.getLogs().size());

        sender.sendMessage(TextFormat.YELLOW + "=== Staff Logs (Page " + page + "/" + totalPages + ") ===");
        for (int i = start; i < end; i++) {
            sender.sendMessage(TextFormat.WHITE + plugin.getLogs().get(i));
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(TextFormat.BLUE + "===== Staff Command Help =====" + TextFormat.RESET);
        sender.sendMessage(TextFormat.WHITE + "[" + TextFormat.RED + "Admin+" + TextFormat.WHITE + "] " + TextFormat.GOLD + "/staff " + TextFormat.BLUE + "unban " + TextFormat.GREEN + "<player>" + TextFormat.WHITE + " - Unban a player");
        sender.sendMessage(TextFormat.WHITE + "[" + TextFormat.RED + "Admin+" + TextFormat.WHITE + "] " + TextFormat.GOLD + "/staff " + TextFormat.BLUE + "unmute " + TextFormat.GREEN + "<player>" + TextFormat.WHITE + " - Unmute a player");
        sender.sendMessage(TextFormat.WHITE + "[" + TextFormat.RED + "Admin+" + TextFormat.WHITE + "] " + TextFormat.GOLD + "/staff " + TextFormat.BLUE + "log " + TextFormat.GREEN + "[page]" + TextFormat.WHITE + " - View action logs");
        sender.sendMessage(TextFormat.WHITE + "[" + TextFormat.RED + "Admin+" + TextFormat.WHITE + "] " + TextFormat.GOLD + "/staff " + TextFormat.BLUE + "check " + TextFormat.GREEN + "<player>" + TextFormat.WHITE + " - Check ban information");
        sender.sendMessage(TextFormat.WHITE + "[" + TextFormat.LIGHT_PURPLE + "Staff" + TextFormat.WHITE + "] " + TextFormat.GOLD + "/staff" + TextFormat.WHITE + " - Open staff window.");
        sender.sendMessage(TextFormat.WHITE + "[" + TextFormat.LIGHT_PURPLE + "Staff" + TextFormat.WHITE + "] " + TextFormat.GOLD + "/staff " + TextFormat.BLUE + "ban " + TextFormat.GREEN + "<player> <reason> [duration]" + TextFormat.WHITE + " - Ban a player");
        sender.sendMessage(TextFormat.WHITE + "[" + TextFormat.LIGHT_PURPLE + "Staff" + TextFormat.WHITE + "] " + TextFormat.GOLD + "/staff " + TextFormat.BLUE + "mute " + TextFormat.GREEN + "<player> <reason> [duration]" + TextFormat.WHITE + " - Mute a player");
        sender.sendMessage(TextFormat.WHITE + "[" + TextFormat.LIGHT_PURPLE + "Staff" + TextFormat.WHITE + "] " + TextFormat.GOLD + "/staff " + TextFormat.BLUE + "kill " + TextFormat.GREEN + "<player>" + TextFormat.WHITE + " - Kill a player");
        sender.sendMessage(TextFormat.WHITE + "[" + TextFormat.LIGHT_PURPLE + "Staff" + TextFormat.WHITE + "] " + TextFormat.GOLD + "/staff " + TextFormat.BLUE + "kick " + TextFormat.GREEN + "<player> <reason>" + TextFormat.WHITE + " - Kick a player");
        sender.sendMessage(TextFormat.WHITE + "[" + TextFormat.LIGHT_PURPLE + "Staff" + TextFormat.WHITE + "] " + TextFormat.GOLD + "/staff " + TextFormat.BLUE + "warn " + TextFormat.GREEN + "<player> <message>" + TextFormat.WHITE + " - Warn a player");
        sender.sendMessage(TextFormat.WHITE + "[" + TextFormat.LIGHT_PURPLE + "Staff" + TextFormat.WHITE + "] " + TextFormat.GOLD + "/staff " + TextFormat.BLUE + "tp " + TextFormat.GREEN + "<player>" + TextFormat.WHITE + " - Teleport to a player (spectator mode)");
        sender.sendMessage(TextFormat.WHITE + "[" + TextFormat.LIGHT_PURPLE + "Staff" + TextFormat.WHITE + "] " + TextFormat.GOLD + "/staff " + TextFormat.BLUE + "weaken " + TextFormat.GREEN + "<player>" + TextFormat.WHITE + " - Weaken a player");
        sender.sendMessage(TextFormat.WHITE + "[" + TextFormat.LIGHT_PURPLE + "Staff" + TextFormat.WHITE + "] " + TextFormat.GOLD + "/staff " + TextFormat.BLUE + "help" + TextFormat.WHITE + " - Show this help");
        sender.sendMessage(TextFormat.RED + "Warning: There are a lot of sub-directives that are harmful to the player, so please use them as appropriate if you are not sure of the facts!" + TextFormat.RESET);
    }

    private long parseDuration(String durationStr) {
        if (durationStr == null || durationStr.isEmpty()) {
            return 0;
        }

        char unit = durationStr.charAt(durationStr.length() - 1);
        String numberStr = durationStr.substring(0, durationStr.length() - 1);

        try {
            long number = Long.parseLong(numberStr);

            return switch (unit) {
                case 'm' -> number * 60 * 1000;
                case 'h' -> number * 60 * 60 * 1000;
                case 'd' -> number * 24 * 60 * 60 * 1000;
                case 'y' -> number * 365L * 24 * 60 * 60 * 1000;
                default -> 0;
            };
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String findBestPlayerMatch(String partialName) {
        List<String> onlinePlayers = plugin.getServer().getOnlinePlayers().values().stream()
                .map(Player::getName)
                .toList();

        for (String name : onlinePlayers) {
            if (name.equalsIgnoreCase(partialName)) {
                return name;
            }
        }

        for (String name : onlinePlayers) {
            if (name.toLowerCase().startsWith(partialName.toLowerCase())) {
                return name;
            }
        }

        for (String name : onlinePlayers) {
            if (name.toLowerCase().contains(partialName.toLowerCase())) {
                return name;
            }
        }

        return null;
    }

    private String findBestPlayerMatchIncludingBanned(String partialName) {
        for (Player player : plugin.getServer().getOnlinePlayers().values()) {
            if (player.getName().equalsIgnoreCase(partialName)) {
                return player.getName();
            }
        }

        for (String bannedName : plugin.getBannedConfig().getKeys(false)) {
            if (bannedName.equalsIgnoreCase(partialName)) {
                return bannedName;
            }
        }

        for (Player player : plugin.getServer().getOnlinePlayers().values()) {
            if (player.getName().toLowerCase().startsWith(partialName.toLowerCase())) {
                return player.getName();
            }
        }

        for (String bannedName : plugin.getBannedConfig().getKeys(false)) {
            if (bannedName.toLowerCase().startsWith(partialName.toLowerCase())) {
                return bannedName;
            }
        }

        for (Player player : plugin.getServer().getOnlinePlayers().values()) {
            if (player.getName().toLowerCase().contains(partialName.toLowerCase())) {
                return player.getName();
            }
        }

        for (String bannedName : plugin.getBannedConfig().getKeys(false)) {
            if (bannedName.toLowerCase().contains(partialName.toLowerCase())) {
                return bannedName;
            }
        }

        return null;
    }

    private String findBestPlayerMatchIncludingMuted(String partialName) {
        String onlineMatch = findBestPlayerMatch(partialName);
        if (onlineMatch != null) {
            return onlineMatch;
        }

        for (String mutedName : plugin.getMutedConfig().getKeys(false)) {
            if (mutedName.equalsIgnoreCase(partialName)) {
                return mutedName;
            }
            if (mutedName.toLowerCase().startsWith(partialName.toLowerCase())) {
                return mutedName;
            }
            if (mutedName.toLowerCase().contains(partialName.toLowerCase())) {
                return mutedName;
            }
        }

        return null;
    }

    private String findBestPlayerMatchIncludingAll(String partialName) {
        for (Player player : plugin.getServer().getOnlinePlayers().values()) {
            if (player.getName().equalsIgnoreCase(partialName)) {
                return player.getName();
            }
        }

        for (String bannedName : plugin.getBannedConfig().getKeys(false)) {
            if (bannedName.equalsIgnoreCase(partialName)) {
                return bannedName;
            }
        }

        for (String mutedName : plugin.getMutedConfig().getKeys(false)) {
            if (mutedName.equalsIgnoreCase(partialName)) {
                return mutedName;
            }
        }

        for (Player player : plugin.getServer().getOnlinePlayers().values()) {
            if (player.getName().toLowerCase().startsWith(partialName.toLowerCase())) {
                return player.getName();
            }
        }

        for (String bannedName : plugin.getBannedConfig().getKeys(false)) {
            if (bannedName.toLowerCase().startsWith(partialName.toLowerCase())) {
                return bannedName;
            }
        }

        for (String mutedName : plugin.getMutedConfig().getKeys(false)) {
            if (mutedName.toLowerCase().startsWith(partialName.toLowerCase())) {
                return mutedName;
            }
        }

        for (Player player : plugin.getServer().getOnlinePlayers().values()) {
            if (player.getName().toLowerCase().contains(partialName.toLowerCase())) {
                return player.getName();
            }
        }

        for (String bannedName : plugin.getBannedConfig().getKeys(false)) {
            if (bannedName.toLowerCase().contains(partialName.toLowerCase())) {
                return bannedName;
            }
        }

        for (String mutedName : plugin.getMutedConfig().getKeys(false)) {
            if (mutedName.toLowerCase().contains(partialName.toLowerCase())) {
                return mutedName;
            }
        }
        return partialName;
    }

}
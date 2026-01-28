package cn.gsfy;

import cn.nukkit.command.PluginCommand;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"SameReturnValue", "ResultOfMethodCallIgnored"})
public class StaffMain extends PluginBase {

    private Config bannedConfig;
    private Config mutedConfig;
    private File logFile;
    private final List<String> logs = new ArrayList<>();
    private static final int LOGS_PER_PAGE = 20;

    private StaffCommandParser commandParser;
    private StaffForm formManager;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        File bannedFile = new File(getDataFolder(), "banned.json");
        File mutedFile = new File(getDataFolder(), "muted.json");
        this.bannedConfig = new Config(bannedFile, Config.JSON);
        this.mutedConfig = new Config(mutedFile, Config.JSON);

        this.logFile = new File(getDataFolder(), "staff_log.log");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (Exception e) {
                getLogger().error("Could not create log file", e);
            }
        }
        loadLogs();

        this.formManager = new StaffForm(this);
        StaffEventHandler eventHandler = new StaffEventHandler(this);

        this.commandParser = new StaffCommandParser(this);
        getServer().getPluginManager().registerEvents(eventHandler, this);

        registerCommands();

        getServer().getScheduler().scheduleDelayedRepeatingTask(this, this::checkExpiredRecords, 20 * 60, 20 * 60);

        getLogger().info("");
        getLogger().info(TextFormat.GOLD + "||" + TextFormat.GREEN + " StaffPanel Plugin");
        getLogger().info(TextFormat.GOLD + "||" + TextFormat.GREEN + " Author: " + TextFormat.YELLOW + "GongSunFangYun");
        getLogger().info(TextFormat.GOLD + "||" + TextFormat.GREEN + " Version: " + TextFormat.BLUE + "1.3.3");
        getLogger().info("");
        getLogger().info(TextFormat.GREEN + "Plugin successfully enabled!");
    }

    private void registerCommands() {
        try {
            PluginCommand<StaffMain> staffCommand = new PluginCommand<>("staff", this);
            staffCommand.setDescription("Managing players is a supreme art (nonsense)");
            staffCommand.setExecutor(this.commandParser);
            getServer().getCommandMap().register("staff", staffCommand);
        } catch (Exception e) {
            getLogger().error("Failed to register staff command in registerCommands method", e);
        }
    }

    @Override
    public void onDisable() {
        bannedConfig.save();
        mutedConfig.save();
        getLogger().info(TextFormat.RED + "Plugin successfully disabled!");
    }

    public Config getBannedConfig() { return bannedConfig; }
    public Config getMutedConfig() { return mutedConfig; }
    public File getLogFile() { return logFile; }
    public List<String> getLogs() { return logs; }
    public int getLogsPerPage() { return LOGS_PER_PAGE; }
    public StaffForm getFormManager() { return formManager; }

    public StaffCommandParser getCommandParser() { return commandParser; }

    private void loadLogs() {
        try (java.util.Scanner scanner = new java.util.Scanner(logFile)) {
            logs.clear();
            while (scanner.hasNextLine()) {
                logs.add(scanner.nextLine());
            }
        } catch (Exception e) {
            getLogger().error("Could not load logs", e);
        }
    }

    private void checkExpiredRecords() {
        long currentTime = System.currentTimeMillis();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (String playerName : bannedConfig.getKeys(false)) {
            java.util.Map<String, Object> banData = bannedConfig.get(playerName, new java.util.HashMap<>());
            String endTimeStr = (String) banData.get("endTime");
            try {
                java.util.Date endTime = sdf.parse(endTimeStr);
                if (endTime.getTime() <= currentTime) {
                    bannedConfig.remove(playerName.toLowerCase());
                    getLogger().info("Automatically unbanned " + TextFormat.GOLD + playerName + TextFormat.WHITE +" (ban expired)");
                }
            } catch (Exception e) {
                getLogger().error("Error checking ban expiration for " + playerName, e);
            }
        }

        for (String playerName : mutedConfig.getKeys(false)) {
            java.util.Map<String, Object> muteData = mutedConfig.get(playerName, new java.util.HashMap<>());
            String endTimeStr = (String) muteData.get("endTime");
            try {
                java.util.Date endTime = sdf.parse(endTimeStr);
                if (endTime.getTime() <= currentTime) {
                    mutedConfig.remove(playerName.toLowerCase());
                    getLogger().info("Automatically unmuted " + playerName + " (mute expired)");

                    // Notify player if online
                    cn.nukkit.Player target = getServer().getPlayerExact(playerName);
                    if (target != null) {
                        target.sendMessage(TextFormat.GREEN + "Your mute has expired!");
                    }
                }
            } catch (Exception e) {
                getLogger().error("Error checking mute expiration for " + playerName, e);
            }
        }

        bannedConfig.save();
        mutedConfig.save();
    }
}
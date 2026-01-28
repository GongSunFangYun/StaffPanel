package cn.gsfy;

import cn.nukkit.Player;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementButtonImageData;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.handler.FormResponseHandler;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.utils.TextFormat;

import java.util.*;

public class StaffForm {

    private final StaffMain plugin;

    public StaffForm(StaffMain plugin) {
        this.plugin = plugin;
    }

    public void openAdminPanelForm(Player staff) {
        FormWindowSimple form = new FormWindowSimple("Admin Panel", "Select an action.");

        form.addButton(new ElementButton("Player Management",
                new ElementButtonImageData("path", "textures/ui/icon_steve.png")));

        form.addButton(new ElementButton("Unban",
                new ElementButtonImageData("path", "textures/ui/icon_fall.png")));

        form.addButton(new ElementButton("Unmute",
                new ElementButtonImageData("path", "textures/ui/mute_off.png")));

        form.addButton(new ElementButton("Check Logs",
                new ElementButtonImageData("path", "textures/items/book_normal.png")));

        form.addButton(new ElementButton("Check Records",
                new ElementButtonImageData("path", "textures/items/paper.png")));

        form.addHandler(FormResponseHandler.withoutPlayer(ignored -> {
            if (form.wasClosed()) return;

            String action = form.getResponse().getClickedButton().getText();

            switch (action) {
                case "Player Management":
                    openPlayerSelectionForm(staff);
                    break;
                case "Unban":
                    openUnbanForm(staff);
                    break;
                case "Unmute":
                    openUnmuteForm(staff);
                    break;
                case "Check Logs":
                    openLogsForm(staff, 1);
                    break;
                case "Check Records":
                    openCheckForm(staff);
                    break;
            }
        }));

        staff.showFormWindow(form);
    }

    public void openPlayerSelectionForm(Player staff) {
        FormWindowCustom form = new FormWindowCustom("Select Player");
        form.addElement(new ElementInput("Player Name", "Enter player name"));

        form.addHandler(FormResponseHandler.withoutPlayer(ignored -> {
            if (form.wasClosed()) return;

            String inputName = form.getResponse().getInputResponse(0);

            if (inputName == null || inputName.trim().isEmpty()) {
                staff.sendMessage(TextFormat.RED + "No player name entered.");
                return;
            }
            String matchedName = findBestPlayerMatchIncludingAll(inputName);
            openPlayerActionForm(staff, matchedName);
        }));

        staff.showFormWindow(form);
    }

    public void openPlayerActionForm(Player staff, String targetName) {
        Player target = plugin.getServer().getPlayerExact(targetName);
        boolean isOnline = target != null && target.isOnline();
        boolean isAdmin = staff.hasPermission("staff.admin");

        FormWindowSimple form = new FormWindowSimple("Manage Player: " + targetName,
                isOnline ? "Select an action" : "Player is offline, limited actions available");

        form.addButton(new ElementButton("Ban",
                new ElementButtonImageData("path", "textures/blocks/barrier.png")));

        form.addButton(new ElementButton("Mute",
                new ElementButtonImageData("path", "textures/ui/mute_on.png")));

        if (isAdmin && !isOnline) {
            form.addButton(new ElementButton("Check Records",
                    new ElementButtonImageData("path", "textures/items/paper.png")));
        }

        if (isOnline) {
            form.addButton(new ElementButton("Kick",
                    new ElementButtonImageData("path", "textures/items/arrow.png")));

            form.addButton(new ElementButton("Warn",
                    new ElementButtonImageData("path", "textures/items/blaze_powder.png")));

            form.addButton(new ElementButton("Kill",
                    new ElementButtonImageData("path", "textures/items/diamond_axe.png")));

            form.addButton(new ElementButton("Teleport",
                    new ElementButtonImageData("path", "textures/items/crossbow_arrow.png")));

            form.addButton(new ElementButton("Weaken",
                    new ElementButtonImageData("path", "textures/ui/weakness_effect.png")));
        }

        form.addHandler(FormResponseHandler.withoutPlayer(ignored -> {
            if (form.wasClosed()) return;

            String action = form.getResponse().getClickedButton().getText();

            switch (action) {
                case "Ban":
                case "Mute":
                case "Kick":
                case "Warn":
                    openActionForm(staff, targetName, action);
                    break;
                case "Check Records":
                    openCheckForm(staff, targetName);
                    break;
                case "Kill":
                case "Teleport":
                case "Weaken":
                    handleOnlineAction(staff, targetName, action);
                    break;
            }
        }));

        staff.showFormWindow(form);
    }

    private void handleOnlineAction(Player staff, String targetName, String action) {
        Player target = plugin.getServer().getPlayerExact(targetName);
        if (target == null || !target.isOnline()) {
            staff.sendMessage(TextFormat.RED + "Player " + targetName + " is no longer online!");
            return;
        }

        switch (action) {
            case "Kill":
                plugin.getCommandParser().handleKillForm(staff, targetName);
                break;
            case "Teleport":
                plugin.getCommandParser().handleTpForm(staff, targetName);
                break;
            case "Weaken":
                plugin.getCommandParser().handleWeakenForm(staff, targetName);
                break;
        }
    }

    private void openActionForm(Player staff, String targetName, String action) {
        FormWindowCustom form = new FormWindowCustom(action + " Player: " + targetName);

        form.addElement(new ElementInput("Reason", "Default Reason"));

        if (action.equals("Ban") || action.equals("Mute")) {
            form.addElement(new ElementInput("Duration (e.g. 1h, 30m, 7d)", "1h"));
        }

        form.addHandler(FormResponseHandler.withoutPlayer(ignored -> {
            if (form.wasClosed()) return;

            String reason = form.getResponse().getInputResponse(0);
            String duration = (action.equals("Ban") || action.equals("Mute")) ?
                    form.getResponse().getInputResponse(1) : null;

            switch (action) {
                case "Ban":
                    plugin.getCommandParser().handleBanForm(staff, targetName, reason, duration);
                    break;
                case "Mute":
                    plugin.getCommandParser().handleMuteForm(staff, targetName, reason, duration);
                    break;
                case "Kick":
                    plugin.getCommandParser().handleKickForm(staff, targetName, reason);
                    break;
                case "Warn":
                    plugin.getCommandParser().handleWarnForm(staff, targetName, reason);
                    break;
            }
        }));

        staff.showFormWindow(form);
    }

    public void openCheckForm(Player staff, String targetName) {
        FormWindowSimple resultForm = new FormWindowSimple("Check Result: " + targetName, "");

        boolean hasBan = plugin.getBannedConfig().exists(targetName.toLowerCase());
        boolean hasMute = plugin.getMutedConfig().exists(targetName.toLowerCase());

        if (!hasBan && !hasMute) {
            resultForm.setContent("No records found for this player.\nWould you want to play something interesting?");
        } else {
            StringBuilder content = new StringBuilder();

            if (hasBan) {
                Map<String, Object> banData = plugin.getBannedConfig().get(targetName.toLowerCase(), new HashMap<>());
                content.append(TextFormat.YELLOW)
                        .append("BAN RECORD:\n")
                        .append(TextFormat.GOLD)
                        .append("BanReason ")
                        .append(TextFormat.WHITE)
                        .append("-> ")
                        .append(TextFormat.GREEN)
                        .append(banData.getOrDefault("reason", "None"))
                        .append("\n")
                        .append(TextFormat.GOLD)
                        .append("BanSource ")
                        .append(TextFormat.WHITE)
                        .append("-> ")
                        .append(TextFormat.GREEN)
                        .append(banData.getOrDefault("executor", "Unknown"))
                        .append("\n")
                        .append(TextFormat.GOLD)
                        .append("Until ")
                        .append(TextFormat.WHITE)
                        .append("-> ")
                        .append(TextFormat.GREEN)
                        .append(banData.getOrDefault("endTime", "Permanent"))
                        .append("\n\n");
            }

            if (hasMute) {
                Map<String, Object> muteData = plugin.getMutedConfig().get(targetName.toLowerCase(), new HashMap<>());
                content.append(TextFormat.YELLOW)
                        .append("MUTE RECORD:\n")
                        .append(TextFormat.GOLD)
                        .append("MuteReason ")
                        .append(TextFormat.WHITE)
                        .append("-> ")
                        .append(TextFormat.GREEN)
                        .append(muteData.getOrDefault("reason", "None"))
                        .append("\n")
                        .append(TextFormat.GOLD)
                        .append("MuteSource: ")
                        .append(TextFormat.WHITE)
                        .append("-> ")
                        .append(TextFormat.GREEN)
                        .append(muteData.getOrDefault("executor", "Unknown"))
                        .append("\n")
                        .append(TextFormat.GOLD)
                        .append("Until ")
                        .append(TextFormat.WHITE)
                        .append("-> ")
                        .append(TextFormat.GREEN)
                        .append(muteData.getOrDefault("endTime", "Permanent"))
                        .append("\n");
            }

            resultForm.setContent(content.toString());
        }

        staff.showFormWindow(resultForm);
    }

    public void openCheckForm(Player staff) {
        FormWindowCustom form = new FormWindowCustom("Check Player Records");
        form.addElement(new ElementInput("Player Name", "Enter player name to check"));

        form.addHandler(FormResponseHandler.withoutPlayer(ignored -> {
            if (form.wasClosed()) return;

            String playerName = form.getResponse().getInputResponse(0);
            if (playerName == null || playerName.trim().isEmpty()) {
                staff.sendMessage(TextFormat.RED + "No player name entered.");
                return;
            }
            String matchedName = findBestPlayerMatchIncludingAll(playerName);
            openCheckForm(staff, matchedName);
        }));

        staff.showFormWindow(form);
    }

    public void openUnbanForm(Player staff) {
        FormWindowCustom form = new FormWindowCustom("Unban Player");
        form.addElement(new ElementInput("Player Name", "Enter banned player name"));

        form.addHandler(FormResponseHandler.withoutPlayer(ignored -> {
            if (form.wasClosed()) return;

            String playerName = form.getResponse().getInputResponse(0);
            if (playerName == null || playerName.trim().isEmpty()) {
                staff.sendMessage(TextFormat.RED + "No player name entered.");
                return;
            }

            String matchedName = findBestPlayerMatchIncludingBanned(playerName);
            if (matchedName == null) {
                staff.sendMessage(TextFormat.RED + "No banned player found matching: " + playerName);
                return;
            }

            if (!plugin.getBannedConfig().exists(matchedName.toLowerCase())) {
                staff.sendMessage(TextFormat.RED + "Player " + matchedName + " is not banned.");
                return;
            }

            plugin.getBannedConfig().remove(matchedName.toLowerCase());
            plugin.getBannedConfig().save();
            staff.sendMessage(TextFormat.GREEN + "Successfully unbanned " + TextFormat.GOLD + matchedName);
            plugin.getCommandParser().logAction("UNBAN", staff, matchedName);
        }));

        staff.showFormWindow(form);
    }

    public void openUnmuteForm(Player staff) {
        FormWindowCustom form = new FormWindowCustom("Unmute Player");
        form.addElement(new ElementInput("Player Name", "Enter muted player name"));

        form.addHandler(FormResponseHandler.withoutPlayer(ignored -> {
            if (form.wasClosed()) return;

            String playerName = form.getResponse().getInputResponse(0);
            if (playerName == null || playerName.trim().isEmpty()) {
                staff.sendMessage(TextFormat.RED + "No player name entered.");
                return;
            }

            String matchedName = findBestPlayerMatchIncludingMuted(playerName);
            if (matchedName == null) {
                staff.sendMessage(TextFormat.RED + "No muted player found matching: " + playerName);
                return;
            }

            if (!plugin.getMutedConfig().exists(matchedName.toLowerCase())) {
                staff.sendMessage(TextFormat.RED + "Player " + matchedName + " is not muted.");
                return;
            }

            plugin.getMutedConfig().remove(matchedName.toLowerCase());
            plugin.getMutedConfig().save();

            Player target = plugin.getServer().getPlayerExact(matchedName);
            if (target != null) {
                target.sendMessage(TextFormat.GREEN + "You have been unmuted by " + staff.getName());
            }

            staff.sendMessage(TextFormat.GREEN + "Successfully unmuted " + TextFormat.GOLD + matchedName);
            plugin.getCommandParser().logAction("UNMUTE", staff, matchedName);
        }));

        staff.showFormWindow(form);
    }

    public void openLogsForm(Player staff, int page) {
        FormWindowSimple form = new FormWindowSimple("Staff Logs", "");

        int totalPages = (int) Math.ceil((double) plugin.getLogs().size() / plugin.getLogsPerPage());
        int start = (page - 1) * plugin.getLogsPerPage();
        int end = Math.min(start + plugin.getLogsPerPage(), plugin.getLogs().size());

        for (int i = start; i < end; i++) {
            form.addButton(new ElementButton(plugin.getLogs().get(i)));
        }

        if (page > 1) {
            form.addButton(new ElementButton("Previous Page"));
        }
        if (page < totalPages) {
            form.addButton(new ElementButton("Next Page"));
        }

        form.addHandler(FormResponseHandler.withoutPlayer(ignored -> {
            if (form.wasClosed()) return;

            String clicked = form.getResponse().getClickedButton().getText();
            if (clicked.equals("Previous Page")) {
                openLogsForm(staff, page - 1);
            } else if (clicked.equals("Next Page")) {
                openLogsForm(staff, page + 1);
            }
        }));

        staff.showFormWindow(form);
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
}
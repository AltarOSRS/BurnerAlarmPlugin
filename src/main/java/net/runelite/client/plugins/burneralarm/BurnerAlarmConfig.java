package net.runelite.client.plugins.burneralarm;

import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup("burneralarm")
public interface BurnerAlarmConfig extends Config {

    // --- Burner Alarm Section ---
    @ConfigSection(name = "Burner Alarm", description = "Settings for the two-stage incense burner alarm.", position = 0, closedByDefault = false)
    String burnerAlarmSection = "burnerAlarmSection";

    @ConfigItem(keyName = "sendNotification", name = "Pre-warning Notification", description = "Configure the notification that fires shortly before burners can go out.", position = 1, section = burnerAlarmSection)
    default Notification burnerPreWarningNotification() {
        return new Notification()
                .withEnabled(true)
                .withInitialized(true)
                .withOverride(true)
                .withTray(true)
                .withTrayIconType(TrayIcon.MessageType.NONE)
                .withRequestFocus(RequestFocusType.OFF)
                .withSound(NotificationSound.NATIVE)
                .withSoundName(null)
                .withVolume(50)
                .withTimeout(10000)
                .withGameMessage(false)
                .withFlash(FlashNotification.FLASH_TWO_SECONDS)
                .withFlashColor(new Color(185, 65, 0, 70))
                .withSendWhenFocused(true);
    }

    @ConfigItem(keyName = "burnerPreWarningColor", name = "Message Color", description = "Color of the burner pre-warning message in chat.", position = 2, section = burnerAlarmSection)
    default Color burnerPreWarningColor() {
        return new Color(185, 65, 0);
    }

    @ConfigItem(keyName = "burnerPreWarningGameMessage", name = "Chat Message", description = "Toggle custom chat message for burner pre-warning.", position = 2, section = burnerAlarmSection)
    default boolean burnerPreWarningGameMessage() {
        return true;
    }

    @ConfigItem(keyName = "playAlertSound", name = "Play Final Alarm", description = "Toggle the main audible alarm that plays the moment burners can extinguish.", position = 3, section = burnerAlarmSection)
    default boolean playFinalAlarm() {
        return true;
    }

    @Range(min = -40, max = 6)
    @ConfigItem(keyName = "soundVolume", name = "Final Alarm Volume (dB)", description = "Adjust the volume of the 'Final Alarm' sound (-40 to +6).", position = 4, section = burnerAlarmSection)
    default int finalAlarmVolume() {
        return -30;
    }

    @Range(min = 0, max = 50)
    @Units(Units.TICKS)
    @ConfigItem(keyName = "leadTime", name = "Pre-warning Lead Time", description = "How many ticks before the final alarm to send the pre-warning notification.", position = 5, section = burnerAlarmSection)
    default int burnerLeadTime() {
        return 25;
    }

    // --- Unlit Burner Highlight Section ---
    @ConfigSection(name = "Unlit Burner Highlight", description = "Settings for highlighting unlit incense burners.", position = 7, closedByDefault = false)
    String unlitBurnerHighlightSection = "unlitBurnerHighlightSection";

    @ConfigItem(keyName = "enableUnlitBurnerOutline", name = "Draw Outline", description = "Draws a smooth outline around the burner model.", position = 1, section = unlitBurnerHighlightSection)
    default boolean enableUnlitBurnerOutline() {
        return true;
    }

    @ConfigItem(keyName = "unlitBurnerHighlightColor", name = "Highlight Color", description = "Color of the unlit burner highlight.", position = 2, section = unlitBurnerHighlightSection)
    default Color unlitBurnerHighlightColor() {
        return Color.RED;
    }

    @Range(min = 1, max = 5)
    @ConfigItem(keyName = "unlitBurnerBorderWidth", name = "Border Width", description = "Width of the highlight border for unlit burners.", position = 3, section = unlitBurnerHighlightSection)
    default int unlitBurnerBorderWidth() {
        return 2;
    }

    @Range(min = 0, max = 4)
    @ConfigItem(keyName = "unlitBurnerOutlineFeather", name = "Outline Feather", description = "How much to feather the outline.", position = 4, section = unlitBurnerHighlightSection)
    default int unlitBurnerOutlineFeather() {
        return 0;
    }


    // --- Tip Jar Section ---
    @ConfigSection(name = "Tip Jar Notifications", description = "Settings for tip jar notifications and chat recoloring.", position = 10, closedByDefault = false)
    String tipJarSection = "tipJarSection";

    // Tier 3
    @ConfigItem(keyName = "tipJarTier3Notification", name = "Tier 3 Notification", description = "Notification for the lowest tier of tips.", position = 11, section = tipJarSection)
    default Notification tipJarTier3Notification() {
        return new Notification()
                .withEnabled(true)
                .withInitialized(true)
                .withOverride(true)
                .withTray(true)
                .withTrayIconType(TrayIcon.MessageType.NONE)
                .withRequestFocus(RequestFocusType.OFF)
                .withSound(NotificationSound.NATIVE)
                .withSoundName(null)
                .withVolume(50)
                .withTimeout(10000)
                .withGameMessage(false)
                .withFlash(FlashNotification.FLASH_TWO_SECONDS)
                .withFlashColor(new Color(0, 115, 0, 70))
                .withSendWhenFocused(true);
    }

    @Units(" gp")
    @ConfigItem(keyName = "tipJarTier3Threshold", name = "Tier 3 Threshold", description = "Minimum tip amount to trigger the Tier 3 notification.", position = 12, section = tipJarSection)
    default int tipJarTier3Threshold() {
        return 100_000;
    }

    @ConfigItem(keyName = "tipJarTier3Color", name = "Tier 3 Color", description = "Color of the Tier 3 tip message in chat.", position = 13, section = tipJarSection)
    default Color tipJarTier3Color() {
        return new Color(0, 115, 0);
    }

    @ConfigItem(keyName = "tipJarRecolorChatMessage", name = "Recolor Tip Jar Chat", description = "Recolor the in-game tip jar message.", position = 13, section = tipJarSection)
    default boolean tipJarRecolorChatMessage() {
        return true;
    }

    // Tier 2
    @ConfigItem(keyName = "tipJarTier2Notification", name = "Tier 2 Notification", description = "Notification for the middle tier of tips.", position = 14, section = tipJarSection)
    default Notification tipJarTier2Notification() {
        return new Notification()
                .withEnabled(true)
                .withInitialized(true)
                .withOverride(true)
                .withTray(true)
                .withTrayIconType(TrayIcon.MessageType.NONE)
                .withRequestFocus(RequestFocusType.OFF)
                .withSound(NotificationSound.NATIVE)
                .withSoundName(null)
                .withVolume(50)
                .withTimeout(10000)
                .withGameMessage(false)
                .withFlash(FlashNotification.FLASH_TWO_SECONDS)
                .withFlashColor(new Color(100, 100, 255, 70))
                .withSendWhenFocused(true);
    }

    @Units(" gp")
    @ConfigItem(keyName = "tipJarTier2Threshold", name = "Tier 2 Threshold", description = "Minimum tip amount to trigger the Tier 2 notification.", position = 15, section = tipJarSection)
    default int tipJarTier2Threshold() {
        return 1_000_000;
    }

    @ConfigItem(keyName = "tipJarTier2Color", name = "Tier 2 Color", description = "Color of the Tier 2 tip message in chat.", position = 16, section = tipJarSection)
    default Color tipJarTier2Color() {
        return new Color(100, 100, 255);
    }

    // Tier 1
    @ConfigItem(keyName = "tipJarTier1Notification", name = "Tier 1 Notification", description = "Notification for the highest tier of tips.", position = 17, section = tipJarSection)
    default Notification tipJarTier1Notification() {
        return new Notification()
                .withEnabled(true)
                .withInitialized(true)
                .withOverride(true)
                .withTray(true)
                .withTrayIconType(TrayIcon.MessageType.NONE)
                .withRequestFocus(RequestFocusType.OFF)
                .withSound(NotificationSound.NATIVE)
                .withSoundName(null)
                .withVolume(50)
                .withTimeout(10000)
                .withGameMessage(false)
                .withFlash(FlashNotification.FLASH_TWO_SECONDS)
                .withFlashColor(new Color(220, 0, 220, 70))
                .withSendWhenFocused(true);
    }

    @Units(" gp")
    @ConfigItem(keyName = "tipJarTier1Threshold", name = "Tier 1 Threshold", description = "Minimum tip amount to trigger the Tier 1 notification.", position = 18, section = tipJarSection)
    default int tipJarTier1Threshold() {
        return 10_000_000;
    }

    @ConfigItem(keyName = "tipJarTier1Color", name = "Tier 1 Color", description = "Color of the Tier 1 tip message in chat.", position = 19, section = tipJarSection)
    default Color tipJarTier1Color() {
        return new Color(220, 0, 220);
    }

    // --- Player Level-Up Notifications Section ---
    @ConfigSection(name = "Player Level-Up Notifications", description = "Settings for notifications when other players level up in your POH.", position = 20, closedByDefault = false)
    String levelUpSection = "levelUpSection";

    @ConfigItem(keyName = "levelUpNotification", name = "Generic Level-Up Notification", description = "Configure the notification for a generic level-up by another player.", position = 21, section = levelUpSection)
    default Notification levelUpNotification() {
        return new Notification()
                .withEnabled(false)
                .withInitialized(true)
                .withOverride(true)
                .withTray(false)
                .withTrayIconType(TrayIcon.MessageType.NONE)
                .withRequestFocus(RequestFocusType.OFF)
                .withSound(NotificationSound.OFF)
                .withSoundName(null)
                .withVolume(50)
                .withTimeout(10000)
                .withGameMessage(false)
                .withFlash(FlashNotification.DISABLED)
                .withFlashColor(new Color(0, 145, 140, 70))
                .withSendWhenFocused(false);
    }

    @ConfigItem(keyName = "levelUpColor", name = "Message Color", description = "Color of the Generic Level-Up message in chat.", position = 22, section = levelUpSection)
    default Color levelUpColor() {
        return new Color(0, 145, 140);
    }

    @ConfigItem(keyName = "levelUpGameMessage", name = "Chat Message", description = "Toggle custom chat message for generic level-ups.", position = 22, section = levelUpSection)
    default boolean levelUpGameMessage() {
        return false;
    }

    @ConfigItem(keyName = "level99Notification", name = "Level 99 Notification", description = "Configure the notification for a level 99 achievement by another player (non-combat).", position = 23, section = levelUpSection)
    default Notification level99Notification() {
        return new Notification()
                .withEnabled(true)
                .withInitialized(true)
                .withOverride(true)
                .withTray(true)
                .withTrayIconType(TrayIcon.MessageType.NONE)
                .withRequestFocus(RequestFocusType.OFF)
                .withSound(NotificationSound.NATIVE)
                .withSoundName(null)
                .withVolume(50)
                .withTimeout(10000)
                .withGameMessage(false)
                .withFlash(FlashNotification.FLASH_TWO_SECONDS)
                .withFlashColor(new Color(90, 0, 180, 70))
                .withSendWhenFocused(true);
    }

    @ConfigItem(keyName = "level99Color", name = "Message Color", description = "Color of the Level 99 achievement message in chat (non-combat).", position = 24, section = levelUpSection)
    default Color level99Color() {
        return new Color(90, 0, 180);
    }

    @ConfigItem(keyName = "level99GameMessage", name = "Chat Message", description = "Toggle custom chat message for level 99 achievements (non-combat).", position = 24, section = levelUpSection)
    default boolean level99GameMessage() {
        return true;
    }

    // --- Combat Level 126 Notification ---
    @ConfigItem(keyName = "level126CombatNotification", name = "Combat Level 126 Notification", description = "Configure the notification for a Combat Level 126 achievement by another player.", position = 25, section = levelUpSection)
    default Notification level126CombatNotification() {
        return new Notification()
                .withEnabled(true)
                .withInitialized(true)
                .withOverride(true)
                .withTray(true)
                .withTrayIconType(TrayIcon.MessageType.NONE)
                .withRequestFocus(RequestFocusType.OFF)
                .withSound(NotificationSound.NATIVE)
                .withSoundName(null)
                .withVolume(75)
                .withTimeout(10000)
                .withGameMessage(false)
                .withFlash(FlashNotification.FLASH_TWO_SECONDS)
                .withFlashColor(new Color(120, 0, 135, 70))
                .withSendWhenFocused(true);
    }

    @ConfigItem(keyName = "level126CombatGameMessage", name = "Chat Message", description = "Toggle custom chat message for Combat Level 126 achievements.", position = 26, section = levelUpSection)
    default boolean level126CombatGameMessage() {
        return true;
    }

    @ConfigItem(keyName = "level126CombatColor", name = "Message Color", description = "Color of the Combat Level 126 achievement message in chat.", position = 27, section = levelUpSection)
    default Color level126CombatColor() {
        return new Color(120, 0, 135);
    }


    // --- Marrentill Tracker Section ---
    @ConfigSection(name = "Marrentill Tracker", description = "Settings for tracking unnoted Clean Marrentills.", position = 40, closedByDefault = false)
    String marrentillTrackerSection = "marrentillTrackerSection";

    @ConfigItem(keyName = "marrentillNotification", name = "Out of Marrentills Notification", description = "Configure notification when you run out of unnoted Clean Marrentills.", position = 41, section = marrentillTrackerSection)
    default Notification marrentillNotification() {
        return new Notification()
                .withEnabled(true)
                .withInitialized(true)
                .withOverride(true)
                .withTray(true)
                .withTrayIconType(TrayIcon.MessageType.NONE)
                .withRequestFocus(RequestFocusType.OFF)
                .withSound(NotificationSound.NATIVE)
                .withSoundName(null)
                .withVolume(50)
                .withTimeout(5000)
                .withGameMessage(false)
                .withFlash(FlashNotification.FLASH_TWO_SECONDS)
                .withFlashColor(new Color(0, 105, 70, 70))
                .withSendWhenFocused(true);
    }

    @ConfigItem(keyName = "marrentillGameMessageColor", name = "Chat Message Color", description = "Color of the 'Out of Marrentills' chat message.", position = 42, section = marrentillTrackerSection)
    default Color marrentillGameMessageColor() {
        return new Color(0, 105, 70);
    }

    @ConfigItem(keyName = "marrentillGameMessage", name = "Chat Message", description = "Toggle custom chat message for low marrentill stock.", position = 43, section = marrentillTrackerSection)
    default boolean marrentillGameMessage() {
        return true;
    }

    // --- POH Guest Tracker Section ---
    @ConfigSection(name = "POH Guest Tracker", description = "Settings for the overlay displaying the number of guests in your POH.", position = 30, closedByDefault = false)
    String guestTrackerSection = "guestTrackerSection";

    @ConfigItem(keyName = "pohGuestTrackerEnabled", name = "Enable Guest Tracker", description = "Toggle the display of the POH guest counter overlay.", position = 31, section = guestTrackerSection)
    default boolean pohGuestTrackerEnabled() {
        return true;
    }
}
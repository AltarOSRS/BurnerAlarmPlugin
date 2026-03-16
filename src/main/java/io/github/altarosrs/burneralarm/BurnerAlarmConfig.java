/*
 * Copyright (c) 2025, Altar <https://github.com/AltarOSRS>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.github.altarosrs.burneralarm;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Notification;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup("burneralarm")
public interface BurnerAlarmConfig extends Config
{
	// --- Enums ---

	enum HighlightStyle
	{
		HULL("Hull"),
		OUTLINE("Outline"),
		CLICKBOX("Clickbox"),
		TILE("Tile");

		private final String name;

		HighlightStyle(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	enum LevelUpChatMode
	{
		OFF("Off"),
		MILESTONES_ONLY("99 & 126 only"),
		ALL("All level-ups");

		private final String name;

		LevelUpChatMode(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	enum LevelUpHighlightMode
	{
		OFF("Off"),
		COMBAT_126_ONLY("Combat 126 only"),
		MILESTONES_ONLY("99 & 126 only"),
		ALL("All level-ups");

		private final String name;

		LevelUpHighlightMode(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	enum TipHighlightMode
	{
		OFF("Off"),
		TIER_1_ONLY("Tier 1 only"),
		TIER_2_AND_ABOVE("Tier 2+"),
		ALL_TIERS("All tiers");

		private final String name;

		TipHighlightMode(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	// --- Section Definitions ---

	@ConfigSection(
		name = "Advanced POH Detection",
		description = "Settings for reliable house ownership detection.",
		position = 1,
		closedByDefault = true
	)
	String advancedPohDetectionSection = "advancedPohDetectionSection";

	@ConfigSection(
		name = "POH Guests",
		description = "Settings for the overlay displaying the number of guests in your POH.",
		position = 10,
		closedByDefault = true
	)
	String guestTrackerSection = "guestTrackerSection";

	@ConfigSection(
		name = "Burner Alarm",
		description = "Settings for the two-stage incense burner alarm.",
		position = 20,
		closedByDefault = true
	)
	String burnerAlarmSection = "burnerAlarmSection";

	@ConfigSection(
		name = "Burner Highlights",
		description = "Settings for highlighting incense burners.",
		position = 25,
		closedByDefault = true
	)
	String burnerHighlightSection = "burnerHighlightSection";

	@ConfigSection(
		name = "Advertisement Warning",
		description = "Notifications and timers for your house advertisement status.",
		position = 30,
		closedByDefault = true
	)
	String advertisementWarningSection = "advertisementWarningSection";

	@ConfigSection(
		name = "Marrentill Tracker",
		description = "Settings for tracking unnoted Clean Marrentills.",
		position = 40,
		closedByDefault = true
	)
	String marrentillTrackerSection = "marrentillTrackerSection";

	@ConfigSection(
		name = "Player Highlights",
		description = "Settings for highlighting players in your POH.",
		position = 50,
		closedByDefault = true
	)
	String playerHighlightSection = "playerHighlightSection";

	@ConfigSection(
		name = "Tip Tracker",
		description = "Settings for the Tip Tracker panel.",
		position = 60,
		closedByDefault = true
	)
	String tipTrackerSection = "tipTrackerSection";

	@ConfigSection(
		name = "Player Level-Ups",
		description = "Settings for notifications when other players level up in your POH.",
		position = 80,
		closedByDefault = true
	)
	String levelUpSection = "levelUpSection";

	// ===== Advanced POH Detection =====

	@ConfigItem(
		keyName = "advancedPohDetectionEnabled",
		name = "Enable Advanced Detection",
		description = "Enables a more reliable method to detect if you are in your own house."
			+ " Requires a scan. If off, all features will work on any POH.",
		position = 1,
		section = advancedPohDetectionSection
	)
	default boolean advancedPohDetectionEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "startHouseScan",
		name = "Scan My House (Toggle)",
		description = "Click this toggle once to scan your current house to create a unique"
			+ " fingerprint. Will automatically reset to unchecked.",
		position = 2,
		section = advancedPohDetectionSection
	)
	default boolean startHouseScan()
	{
		return false;
	}

	@ConfigItem(
		keyName = "pohFingerprint",
		name = "House Fingerprint Data",
		description = "Saved fingerprint data for your house.",
		hidden = true
	)
	default String pohFingerprint()
	{
		return "";
	}

	@ConfigItem(
		keyName = "pohStatusOverlayEnabled",
		name = "Show POH Status Overlay",
		description = "Shows an overlay indicating if you are in your own house or another"
			+ " player's house.",
		position = 3,
		section = advancedPohDetectionSection
	)
	default boolean pohStatusOverlayEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "allowFeaturesInGuestPOH",
		name = "Run Features in Guest's House",
		description = "Allows all plugin features to function while you are in another"
			+ " player's house.",
		position = 4,
		section = advancedPohDetectionSection
	)
	default boolean allowFeaturesInGuestPOH()
	{
		return false;
	}

	// ===== POH Guests =====

	@ConfigItem(
		keyName = "pohGuestTrackerEnabled",
		name = "Enable Guest Tracker",
		description = "Toggle the display of the POH guest counter overlay.",
		position = 1,
		section = guestTrackerSection
	)
	default boolean pohGuestTrackerEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hideExpelGuests",
		name = "Hide 'Expel Guests' button",
		description = "Hides the 'Expel Guests' button to prevent accidental clicks.",
		position = 2,
		section = guestTrackerSection
	)
	default boolean hideExpelGuests()
	{
		return true;
	}

	// ===== Burner Alarm =====

	@ConfigItem(
		keyName = "sendNotification",
		name = "Pre-warning Notification",
		description = "Configure the notification that fires shortly before burners can go out.",
		position = 1,
		section = burnerAlarmSection
	)
	default Notification burnerPreWarningNotification()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "burnerPreWarningGameMessage",
		name = "Chat Message",
		description = "Toggle custom chat message for burner pre-warning.",
		position = 2,
		section = burnerAlarmSection
	)
	default boolean burnerPreWarningGameMessage()
	{
		return true;
	}

	@ConfigItem(
		keyName = "burnerPreWarningColor",
		name = "Color",
		description = "Color of the burner pre-warning message in chat.",
		position = 3,
		section = burnerAlarmSection
	)
	default Color burnerPreWarningColor()
	{
		return new Color(185, 65, 0);
	}

	@ConfigItem(
		keyName = "playAlertSound",
		name = "Play Final Alarm",
		description = "Toggle the main audible alarm that plays the moment burners can"
			+ " extinguish.",
		position = 4,
		section = burnerAlarmSection
	)
	default boolean playFinalAlarm()
	{
		return true;
	}

	@Range(min = -40, max = 6)
	@ConfigItem(
		keyName = "soundVolume",
		name = "Final Alarm Volume (dB)",
		description = "Adjust the volume of the 'Final Alarm' sound (-40 to +6).",
		position = 5,
		section = burnerAlarmSection
	)
	default int finalAlarmVolume()
	{
		return -30;
	}

	@Range(min = 0, max = 50)
	@Units(Units.TICKS)
	@ConfigItem(
		keyName = "leadTime",
		name = "Pre-warning Lead Time",
		description = "How many ticks before the final alarm to send the pre-warning"
			+ " notification.",
		position = 6,
		section = burnerAlarmSection
	)
	default int burnerLeadTime()
	{
		return 25;
	}

	@ConfigItem(
		keyName = "burnerAlarmOverlay",
		name = "Show Alarm Overlay",
		description = "Shows an overlay on screen during the pre-warning and final alarm.",
		position = 7,
		section = burnerAlarmSection
	)
	default boolean burnerAlarmOverlay()
	{
		return true;
	}

	// ===== Burner Highlights =====

	@ConfigItem(
		keyName = "enableUnlitBurnerOutline",
		name = "Highlight Unlit Burners",
		description = "Enables highlighting for unlit burners.",
		position = 1,
		section = burnerHighlightSection
	)
	default boolean highlightUnlitBurners()
	{
		return true;
	}

	@ConfigItem(
		keyName = "unlitBurnerHighlightStyle",
		name = "Unlit Burner Style",
		description = "The style of highlight to use for unlit burners.",
		position = 3,
		section = burnerHighlightSection
	)
	default HighlightStyle unlitBurnerHighlightStyle()
	{
		return HighlightStyle.OUTLINE;
	}

	@ConfigItem(
		keyName = "highlightLitBurners",
		name = "Highlight Lit Burners",
		description = "Enables highlighting for lit burners.",
		position = 4,
		section = burnerHighlightSection
	)
	default boolean highlightLitBurners()
	{
		return false;
	}

	@ConfigItem(
		keyName = "litBurnerHighlightStyle",
		name = "Lit Burner Style",
		description = "The style of highlight to use for lit and random burnout burners.",
		position = 6,
		section = burnerHighlightSection
	)
	default HighlightStyle litBurnerHighlightStyle()
	{
		return HighlightStyle.OUTLINE;
	}


	// ===== Advertisement Warning =====

	@ConfigItem(
		keyName = "enableAdWarning",
		name = "Enable Advertisement Warnings",
		description = "Shows a timer and sends notifications for POH advertisement removal.",
		position = 1,
		section = advertisementWarningSection
	)
	default boolean enableAdWarning()
	{
		return true;
	}

	@ConfigItem(
		keyName = "adWarningNotification",
		name = "Ad Warning Notification",
		description = "Notification for advertisement removal countdown warnings.",
		position = 2,
		section = advertisementWarningSection
	)
	default Notification adWarningNotification()
	{
		return Notification.ON;
	}

	// ===== Marrentill Tracker =====

	@ConfigItem(
		keyName = "marrentillNotification",
		name = "Out of Marrentills Notification",
		description = "Configure notification when you run out of unnoted Clean Marrentills.",
		position = 1,
		section = marrentillTrackerSection
	)
	default Notification marrentillNotification()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "marrentillGameMessage",
		name = "Chat Message",
		description = "Toggle custom chat message for low marrentill stock.",
		position = 2,
		section = marrentillTrackerSection
	)
	default boolean marrentillGameMessage()
	{
		return true;
	}

	@ConfigItem(
		keyName = "marrentillGameMessageColor",
		name = "Color",
		description = "Color of the 'Out of Marrentills' chat message.",
		position = 3,
		section = marrentillTrackerSection
	)
	default Color marrentillGameMessageColor()
	{
		return new Color(0, 105, 70);
	}

	@ConfigItem(
		keyName = "marrentillOverlay",
		name = "Show Low Stock Overlay",
		description = "Shows an overlay on screen when you are low on unnoted Clean"
			+ " Marrentills.",
		position = 4,
		section = marrentillTrackerSection
	)
	default boolean marrentillOverlay()
	{
		return true;
	}

	// ===== Player Highlights (merged) =====

	@ConfigItem(
		keyName = "drawPlayerNames",
		name = "Draw Player Names",
		description = "Draws the name of highlighted players above their heads.",
		position = 1,
		section = playerHighlightSection
	)
	default boolean drawPlayerNames()
	{
		return true;
	}

	@ConfigItem(
		keyName = "drawPlayerTiles",
		name = "Draw Player Tiles",
		description = "Draws a tile outline under highlighted players.",
		position = 2,
		section = playerHighlightSection
	)
	default boolean drawPlayerTiles()
	{
		return false;
	}

	@ConfigItem(
		keyName = "excludeFriendsAndClan",
		name = "Exclude friends/clan from highlight",
		description = "If enabled, this plugin will not highlight friends, clan, or friends"
			+ " chat members to avoid conflicts with other plugins.",
		position = 3,
		section = playerHighlightSection
	)
	default boolean excludeFriendsAndClan()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightTopTippers",
		name = "Highlight Top 100 Tippers",
		description = "Permanently highlight players in the top 100 of your 'All Time' tip"
			+ " leaderboard.",
		position = 4,
		section = playerHighlightSection
	)
	default boolean highlightTopTippers()
	{
		return true;
	}

	@Range(min = 1, max = 60)
	@Units(Units.MINUTES)
	@ConfigItem(
		keyName = "highlightCooldown",
		name = "Highlight Cooldown",
		description = "The duration in minutes before a temporary highlight expires.",
		position = 5,
		section = playerHighlightSection
	)
	default int highlightCooldown()
	{
		return 5;
	}

	@ConfigItem(
		keyName = "tipHighlightMode",
		name = "Highlight Tip Jar Tips",
		description = "Which tip jar tip tiers trigger a temporary player highlight.",
		position = 6,
		section = playerHighlightSection
	)
	default TipHighlightMode tipHighlightMode()
	{
		return TipHighlightMode.ALL_TIERS;
	}

	@ConfigItem(
		keyName = "highlightTradeTips",
		name = "Highlight Trade Tips",
		description = "Temporarily highlights the player who tipped you via trade.",
		position = 7,
		section = playerHighlightSection
	)
	default boolean highlightTradeTips()
	{
		return true;
	}

	@ConfigItem(
		keyName = "levelUpHighlightMode",
		name = "Highlight Level-Ups",
		description = "Which level-up events trigger a temporary player highlight.",
		position = 8,
		section = playerHighlightSection
	)
	default LevelUpHighlightMode levelUpHighlightMode()
	{
		return LevelUpHighlightMode.MILESTONES_ONLY;
	}

	// ===== Tip Tracker =====

	@ConfigItem(
		keyName = "enableTipTracker",
		name = "Enable Tip Tracker",
		description = "Toggles the Tip Tracker side panel.",
		position = 1,
		section = tipTrackerSection
	)
	default boolean enableTipTracker()
	{
		return true;
	}

	@ConfigItem(
		keyName = "tradeTipColor",
		name = "Trade Tip Color",
		description = "Color for trade tips.",
		position = 4,
		section = tipTrackerSection
	)
	default Color tradeTipColor()
	{
		return new Color(255, 255, 0);
	}

	@Units(" gp")
	@ConfigItem(
		keyName = "tipJarTier3Threshold",
		name = "Tier 3 Threshold",
		description = "Minimum tip amount for Tier 3.",
		position = 9,
		section = tipTrackerSection
	)
	default int tipJarTier3Threshold()
	{
		return 100_000;
	}

	@ConfigItem(
		keyName = "tipJarTier3Color",
		name = "Tier 3 Color",
		description = "Color of Tier 3 tips.",
		position = 10,
		section = tipTrackerSection
	)
	default Color tipJarTier3Color()
	{
		return new Color(0, 115, 0);
	}

	@ConfigItem(
		keyName = "tipJarTier3Notification",
		name = "Tip Jar Tier 3 Notification",
		description = "Notification for the lowest tier of tips.",
		position = 11,
		section = tipTrackerSection
	)
	default Notification tipJarTier3Notification()
	{
		return Notification.ON;
	}

	@Units(" gp")
	@ConfigItem(
		keyName = "tipJarTier2Threshold",
		name = "Tier 2 Threshold",
		description = "Minimum tip amount for Tier 2.",
		position = 12,
		section = tipTrackerSection
	)
	default int tipJarTier2Threshold()
	{
		return 1_000_000;
	}

	@ConfigItem(
		keyName = "tipJarTier2Color",
		name = "Tier 2 Color",
		description = "Color of Tier 2 tips.",
		position = 13,
		section = tipTrackerSection
	)
	default Color tipJarTier2Color()
	{
		return new Color(100, 100, 255);
	}

	@ConfigItem(
		keyName = "tipJarTier2Notification",
		name = "Tip Jar Tier 2 Notification",
		description = "Notification for the middle tier of tips.",
		position = 14,
		section = tipTrackerSection
	)
	default Notification tipJarTier2Notification()
	{
		return Notification.ON;
	}

	@Units(" gp")
	@ConfigItem(
		keyName = "tipJarTier1Threshold",
		name = "Tier 1 Threshold",
		description = "Minimum tip amount for Tier 1.",
		position = 15,
		section = tipTrackerSection
	)
	default int tipJarTier1Threshold()
	{
		return 10_000_000;
	}

	@ConfigItem(
		keyName = "tipJarTier1Color",
		name = "Tier 1 Color",
		description = "Color of Tier 1 tips.",
		position = 16,
		section = tipTrackerSection
	)
	default Color tipJarTier1Color()
	{
		return new Color(220, 0, 220);
	}

	@ConfigItem(
		keyName = "tipJarTier1Notification",
		name = "Tip Jar Tier 1 Notification",
		description = "Notification for the highest tier of tips.",
		position = 17,
		section = tipTrackerSection
	)
	default Notification tipJarTier1Notification()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "resetCurrentCharacterTips",
		name = "Reset Current Character",
		description = "Deletes all tip data for the currently logged-in character.",
		warning = "This will delete all tip data for the current character."
			+ " This action cannot be undone.",
		position = 18,
		section = tipTrackerSection
	)
	default boolean resetCurrentCharacterTips()
	{
		return false;
	}

	@ConfigItem(
		keyName = "tipTrackerData",
		name = "",
		description = "",
		hidden = true
	)
	default String tipTrackerData()
	{
		return "";
	}

	// ===== Player Level-Ups =====

	@ConfigItem(
		keyName = "levelUpChatMode",
		name = "Chat Messages",
		description = "Which level-up events show a colored chat message.",
		position = 1,
		section = levelUpSection
	)
	default LevelUpChatMode levelUpChatMode()
	{
		return LevelUpChatMode.MILESTONES_ONLY;
	}

	@ConfigItem(
		keyName = "levelUpNotification",
		name = "Generic Level-Up Notification",
		description = "Configure the notification for a generic level-up by another player.",
		position = 2,
		section = levelUpSection
	)
	default Notification levelUpNotification()
	{
		return Notification.OFF;
	}

	@ConfigItem(
		keyName = "levelUpColor",
		name = "Generic Level-Up Color",
		description = "Color of the Generic Level-Up message in chat.",
		position = 3,
		section = levelUpSection
	)
	default Color levelUpColor()
	{
		return new Color(0, 145, 140);
	}

	@ConfigItem(
		keyName = "level99Notification",
		name = "Level 99 Notification",
		description = "Configure the notification for a level 99 achievement by another"
			+ " player.",
		position = 4,
		section = levelUpSection
	)
	default Notification level99Notification()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "level99Color",
		name = "Level 99 Color",
		description = "Color of the Level 99 achievement message in chat.",
		position = 5,
		section = levelUpSection
	)
	default Color level99Color()
	{
		return new Color(90, 0, 180);
	}

	@ConfigItem(
		keyName = "level126CombatNotification",
		name = "Combat Level 126 Notification",
		description = "Configure the notification for a Combat Level 126 achievement by"
			+ " another player.",
		position = 6,
		section = levelUpSection
	)
	default Notification level126CombatNotification()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "level126CombatColor",
		name = "Combat Level 126 Color",
		description = "Color of the Combat Level 126 achievement message in chat.",
		position = 7,
		section = levelUpSection
	)
	default Color level126CombatColor()
	{
		return new Color(120, 0, 135);
	}
}

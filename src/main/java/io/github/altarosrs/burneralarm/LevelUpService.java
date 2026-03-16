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
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.PlayerDespawned;
import net.runelite.api.events.PlayerSpawned;
import net.runelite.client.Notifier;
import net.runelite.client.config.Notification;
import net.runelite.client.util.ColorUtil;

@Slf4j
@Singleton
public class LevelUpService
{
	private final Client client;
	private final BurnerAlarmConfig config;
	private final Notifier notifier;
	private final HouseDetectionService houseDetectionService;
	private final PlayerHighlightService playerHighlightService;

	private final Map<String, Integer> playerCombatLevels = new HashMap<>();
	private final Map<String, Boolean> playerSuppressNext99GraphicFor126 = new HashMap<>();

	@Inject
	public LevelUpService(Client client, BurnerAlarmConfig config, Notifier notifier,
		HouseDetectionService houseDetectionService, PlayerHighlightService playerHighlightService)
	{
		this.client = client;
		this.config = config;
		this.notifier = notifier;
		this.houseDetectionService = houseDetectionService;
		this.playerHighlightService = playerHighlightService;
	}

	public void reset()
	{
		playerCombatLevels.clear();
		playerSuppressNext99GraphicFor126.clear();
	}

	public void initCombatLevels()
	{
		playerCombatLevels.clear();
		playerSuppressNext99GraphicFor126.clear();
		if (houseDetectionService.isInPOH())
		{
			for (Player player : client.getTopLevelWorldView().players())
			{
				if (player != null && !player.equals(client.getLocalPlayer()))
				{
					playerCombatLevels.put(player.getName(), player.getCombatLevel());
				}
			}
		}
	}

	public void onPlayerSpawned(PlayerSpawned event)
	{
		final Player player = event.getPlayer();
		if (player != null && !player.equals(client.getLocalPlayer()) && houseDetectionService.isInPOH())
		{
			playerCombatLevels.put(player.getName(), player.getCombatLevel());
		}
	}

	public void onPlayerDespawned(PlayerDespawned event)
	{
		if (!houseDetectionService.isInPOH())
		{
			return;
		}
		final Player player = event.getPlayer();
		if (player != null)
		{
			playerCombatLevels.remove(player.getName());
			playerSuppressNext99GraphicFor126.remove(player.getName());
		}
	}

	public void onGraphicChanged(GraphicChanged event)
	{
		if (!(event.getActor() instanceof Player))
		{
			return;
		}
		final Player player = (Player) event.getActor();
		if (player.equals(client.getLocalPlayer()))
		{
			return;
		}
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		if (!houseDetectionService.isInPOH())
		{
			return;
		}

		final boolean isGenericLevelUpGraphic = player.hasSpotAnim(BurnerAlarmConstants.GENERIC_LEVEL_UP_GRAPHIC_ID);
		final boolean isLevel99Graphic = player.hasSpotAnim(BurnerAlarmConstants.LEVEL_99_GRAPHIC_ID);
		log.debug("GraphicChanged for {}. Has 99 graphic: {}. Has Generic graphic: {}.",
			player.getName(), isLevel99Graphic, isGenericLevelUpGraphic);
		if (!isGenericLevelUpGraphic && !isLevel99Graphic)
		{
			return;
		}

		Integer previousCombatLevel = playerCombatLevels.get(player.getName());
		int currentCombatLevel = player.getCombatLevel();
		log.debug("Level-up Graphic for {}. Previous Combat Level: {}. Current Combat Level: {}.",
			player.getName(), previousCombatLevel, currentCombatLevel);

		// Combat Level 126 Detection
		if (previousCombatLevel != null && previousCombatLevel == 125 && currentCombatLevel == 126)
		{
			handleCombat126(player);
			playerSuppressNext99GraphicFor126.put(player.getName(), true);
		}

		// Handle Generic Level Up Graphic
		if (isGenericLevelUpGraphic
			&& houseDetectionService.shouldRunFeature(config.allowFeaturesInGuestPOH()))
		{
			handleGenericLevelUp(player);
		}
		// Level 99 Skill Detection
		else if (isLevel99Graphic
			&& houseDetectionService.shouldRunFeature(config.allowFeaturesInGuestPOH()))
		{
			boolean shouldSuppressThis99Graphic =
				playerSuppressNext99GraphicFor126.getOrDefault(player.getName(), false);

			if (shouldSuppressThis99Graphic)
			{
				log.debug("Level 99 graphic for {} suppressed (expected 126 combat animation).",
					player.getName());
				playerSuppressNext99GraphicFor126.put(player.getName(), false);
			}
			else
			{
				handleLevel99(player);
			}
		}

		// Update combat level tracking
		if (previousCombatLevel == null || previousCombatLevel != currentCombatLevel)
		{
			playerCombatLevels.put(player.getName(), currentCombatLevel);
			log.debug("Updated {}'s combat level to {}.", player.getName(), currentCombatLevel);
		}
	}

	private void handleCombat126(Player player)
	{
		handleLevelEvent(player,
			player.getName() + " has achieved Combat Level 126!",
			config.level126CombatNotification(),
			config.level126CombatColor(),
			config.levelUpChatMode() != BurnerAlarmConfig.LevelUpChatMode.OFF,
			config.levelUpHighlightMode() != BurnerAlarmConfig.LevelUpHighlightMode.OFF);
	}

	private void handleGenericLevelUp(Player player)
	{
		handleLevelEvent(player,
			player.getName() + " has leveled up!",
			config.levelUpNotification(),
			config.levelUpColor(),
			config.levelUpChatMode() == BurnerAlarmConfig.LevelUpChatMode.ALL,
			config.levelUpHighlightMode() == BurnerAlarmConfig.LevelUpHighlightMode.ALL);
	}

	private void handleLevel99(Player player)
	{
		BurnerAlarmConfig.LevelUpHighlightMode hlMode = config.levelUpHighlightMode();
		handleLevelEvent(player,
			player.getName() + " has achieved level 99!",
			config.level99Notification(),
			config.level99Color(),
			config.levelUpChatMode() != BurnerAlarmConfig.LevelUpChatMode.OFF,
			hlMode == BurnerAlarmConfig.LevelUpHighlightMode.MILESTONES_ONLY
				|| hlMode == BurnerAlarmConfig.LevelUpHighlightMode.ALL);
	}

	private void handleLevelEvent(Player player, String message,
		Notification notifConfig, Color color, boolean shouldChat, boolean shouldHighlight)
	{
		log.debug("Detected: {}", message);
		if (notifConfig.isEnabled())
		{
			notifier.notify(notifConfig, BurnerAlarmConstants.PLUGIN_PREFIX + message);
		}
		if (shouldChat && color != null)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
				ColorUtil.wrapWithColorTag(
					BurnerAlarmConstants.PLUGIN_PREFIX + message, color),
				null);
		}
		if (shouldHighlight)
		{
			playerHighlightService.addTemporaryHighlight(player.getName(), color);
		}
	}
}

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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameObject;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.client.Notifier;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.Notification;
import net.runelite.client.game.ItemManager;
import net.runelite.client.hiscore.HiscoreEndpoint;
import net.runelite.client.hiscore.HiscoreManager;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ColorUtil;

import java.util.concurrent.ScheduledExecutorService;

@Slf4j
@Singleton
public class BurnerService
{
	public enum BurnerOverlayState
	{
		OFF,
		PRE_WARNING,
		CAN_EXTINGUISH,
		FINAL_ALARM
	}

	@Getter
	public static class BurnerState
	{
		final int startTick;
		int certainDurationTicks = 0;
		boolean preNotificationSent = false;
		boolean finalAlarmSent = false;

		BurnerState(int startTick)
		{
			this.startTick = startTick;
		}
	}

	private final Client client;
	private final ClientThread clientThread;
	private final BurnerAlarmConfig config;
	private final Notifier notifier;
	private final AudioPlayer audioPlayer;
	private final InfoBoxManager infoBoxManager;
	private final ItemManager itemManager;
	private final ScheduledExecutorService executor;
	private final HiscoreManager hiscoreManager;
	private final HouseDetectionService houseDetectionService;

	@Getter
	private final Map<Tile, BurnerState> litBurners = new HashMap<>();
	@Getter
	private final Map<Tile, GameObject> unlitBurners = new HashMap<>();
	@Getter
	private final Map<Tile, GameObject> litBurnerObjects = new HashMap<>();
	private final Map<Tile, Integer> pendingFiremakingLevels = new HashMap<>();

	private int lastPreWarningTick = 0;
	private int lastFinalAlarmTick = 0;
	@Getter
	private BurnerOverlayState burnerOverlayState = BurnerOverlayState.OFF;
	private boolean burnersHaveBeenLitThisSession = false;

	private int lightingGracePeriodEndTick = 0;
	private static final int GRACE_PERIOD_TICKS = 8;
	private int lastTimerTicksRemaining = 0;

	private BurnerTimerInfobox burnerTimerInfobox = null;
	private BurnerAlarmPlugin plugin;

	@Inject
	public BurnerService(Client client, ClientThread clientThread,
		BurnerAlarmConfig config, Notifier notifier, AudioPlayer audioPlayer,
		InfoBoxManager infoBoxManager, ItemManager itemManager,
		ScheduledExecutorService executor, HiscoreManager hiscoreManager,
		HouseDetectionService houseDetectionService)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.config = config;
		this.notifier = notifier;
		this.audioPlayer = audioPlayer;
		this.infoBoxManager = infoBoxManager;
		this.itemManager = itemManager;
		this.executor = executor;
		this.hiscoreManager = hiscoreManager;
		this.houseDetectionService = houseDetectionService;
	}

	public void setPlugin(BurnerAlarmPlugin plugin)
	{
		this.plugin = plugin;
	}

	public boolean isLightingGracePeriodActive()
	{
		return client.getTickCount() < lightingGracePeriodEndTick;
	}

	public void reset()
	{
		removeTimerInfobox();
		litBurners.clear();
		litBurnerObjects.clear();
		unlitBurners.clear();
		pendingFiremakingLevels.clear();
		burnerOverlayState = BurnerOverlayState.OFF;
		burnersHaveBeenLitThisSession = false;
		lightingGracePeriodEndTick = 0;
	}

	public void shutDown()
	{
		removeTimerInfobox();
	}

	public void onGameTick()
	{
		if (houseDetectionService.shouldRunFeature(config.allowFeaturesInGuestPOH()))
		{
			processAlarms();
		}
		updateBurnerTimer();
		updateBurnerOverlayState();
	}

	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		final GameObject gameObject = event.getGameObject();
		final Tile tile = event.getTile();

		if (BurnerAlarmConstants.LIT_BURNER_IDS.contains(gameObject.getId()))
		{
			if (litBurners.isEmpty() || !isLightingGracePeriodActive())
			{
				lightingGracePeriodEndTick = client.getTickCount() + GRACE_PERIOD_TICKS;
			}

			burnersHaveBeenLitThisSession = true;
			BurnerState state = new BurnerState(client.getTickCount());
			litBurners.put(tile, state);
			litBurnerObjects.put(tile, gameObject);
			unlitBurners.remove(tile);

			Integer firemakingLevel = pendingFiremakingLevels.remove(tile);
			if (firemakingLevel != null)
			{
				state.certainDurationTicks = 200 + firemakingLevel;
				log.debug("Burner lit on tile {}. Using pre-fetched level {} for a duration of {} ticks.",
					tile.getWorldLocation(), firemakingLevel, state.certainDurationTicks);
			}
			else
			{
				log.debug("Burner lit on tile {}. Waiting for remote player hiscore data...",
					tile.getWorldLocation());
			}
		}
		else if (BurnerAlarmConstants.UNLIT_BURNER_IDS.contains(gameObject.getId()))
		{
			unlitBurners.put(tile, gameObject);
			litBurnerObjects.remove(tile);
		}
		updateBurnerOverlayState();
	}

	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		final Tile tile = event.getTile();
		litBurners.remove(tile);
		litBurnerObjects.remove(tile);
		unlitBurners.remove(tile);
		updateBurnerOverlayState();
	}

	public void onAnimationChanged(AnimationChanged event)
	{
		final Actor actor = event.getActor();
		if (!(actor instanceof Player)
			|| actor.getAnimation() != BurnerAlarmConstants.HUMAN_LIGHT_TORCH_ANIMATION_ID)
		{
			return;
		}

		final LocalPoint loc = actor.getLocalLocation();
		Set<Tile> allBurnerTiles = new HashSet<>(litBurners.keySet());
		allBurnerTiles.addAll(unlitBurners.keySet());
		allBurnerTiles.stream()
			.min(Comparator.comparingInt(tile -> loc.distanceTo(tile.getLocalLocation())))
			.ifPresent(tile ->
			{
				if (actor == client.getLocalPlayer())
				{
					int level = client.getRealSkillLevel(Skill.FIREMAKING);
					log.debug("Local player lighting burner, Firemaking level: {}", level);
					pendingFiremakingLevels.put(tile, level);
				}
				else if (actor.getName() != null)
				{
					log.debug("Remote player {} lighting burner, looking up Firemaking level.",
						actor.getName());
					lookupPlayer(actor.getName(), tile);
				}
			});
	}

	private void processAlarms()
	{
		if (litBurners.isEmpty())
		{
			return;
		}

		final int currentTick = client.getTickCount();
		boolean triggerPreWarningThisTick = false;
		boolean triggerFinalAlarmThisTick = false;

		for (Map.Entry<Tile, BurnerState> entry : litBurners.entrySet())
		{
			BurnerState state = entry.getValue();
			if (state.certainDurationTicks <= 0)
			{
				continue;
			}

			final int preNotificationTriggerTicks = state.certainDurationTicks - config.burnerLeadTime();
			final int ticksSinceLit = currentTick - state.startTick;

			if (!state.preNotificationSent && ticksSinceLit >= preNotificationTriggerTicks)
			{
				state.preNotificationSent = true;
				triggerPreWarningThisTick = true;
			}
			if (!state.finalAlarmSent && ticksSinceLit >= state.certainDurationTicks)
			{
				state.finalAlarmSent = true;
				triggerFinalAlarmThisTick = true;
			}
		}

		if (triggerPreWarningThisTick)
		{
			if (currentTick >= lastPreWarningTick + BurnerAlarmConstants.NOTIFICATION_COOLDOWN_TICKS)
			{
				Notification preWarningNotification = config.burnerPreWarningNotification();
				String notificationMessage = "A burner will enter its random burnout phase soon!";
				if (preWarningNotification.isEnabled())
				{
					notifier.notify(preWarningNotification,
						BurnerAlarmConstants.PLUGIN_PREFIX + notificationMessage);
				}
				if (config.burnerPreWarningGameMessage())
				{
					client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
						ColorUtil.wrapWithColorTag(
							BurnerAlarmConstants.PLUGIN_PREFIX + notificationMessage,
							config.burnerPreWarningColor()),
						null);
				}
				lastPreWarningTick = currentTick;
			}
		}
		if (triggerFinalAlarmThisTick && config.playFinalAlarm())
		{
			if (currentTick >= lastFinalAlarmTick + BurnerAlarmConstants.NOTIFICATION_COOLDOWN_TICKS)
			{
				playFinalAlarmSound();
				lastFinalAlarmTick = currentTick;
			}
		}
	}

	private void updateBurnerTimer()
	{
		if (litBurners.isEmpty())
		{
			removeTimerInfobox();
			return;
		}

		int currentTick = client.getTickCount();
		int leadTime = config.burnerLeadTime();

		int newTicksRemaining = litBurners.values().stream()
			.filter(burnerState -> burnerState.getCertainDurationTicks() > 0)
			.mapToInt(burnerState ->
				(burnerState.getCertainDurationTicks() - leadTime) - (currentTick - burnerState.getStartTick()))
			.min()
			.orElse(-1);

		if (newTicksRemaining <= 0)
		{
			removeTimerInfobox();
			return;
		}

		if (burnerTimerInfobox == null || newTicksRemaining != lastTimerTicksRemaining - 1)
		{
			removeTimerInfobox();
			final BufferedImage burnerIcon = itemManager.getImage(8063);
			Duration duration = Duration.ofMillis((long) newTicksRemaining * 600);
			burnerTimerInfobox = new BurnerTimerInfobox(duration, burnerIcon, plugin);
			infoBoxManager.addInfoBox(burnerTimerInfobox);
		}

		lastTimerTicksRemaining = newTicksRemaining;
	}

	private void removeTimerInfobox()
	{
		if (burnerTimerInfobox != null)
		{
			infoBoxManager.removeInfoBox(burnerTimerInfobox);
			burnerTimerInfobox = null;
		}
	}

	void updateBurnerOverlayState()
	{
		if (!burnersHaveBeenLitThisSession || !config.burnerAlarmOverlay()
			|| !houseDetectionService.shouldRunFeature(config.allowFeaturesInGuestPOH()))
		{
			burnerOverlayState = BurnerOverlayState.OFF;
			return;
		}

		if (!unlitBurners.isEmpty())
		{
			burnerOverlayState = BurnerOverlayState.FINAL_ALARM;
			return;
		}

		boolean isPreWarning = false;
		for (BurnerState burnerState : litBurners.values())
		{
			if (burnerState.finalAlarmSent)
			{
				burnerOverlayState = BurnerOverlayState.CAN_EXTINGUISH;
				return;
			}
			if (burnerState.preNotificationSent)
			{
				isPreWarning = true;
			}
		}

		if (isPreWarning)
		{
			burnerOverlayState = BurnerOverlayState.PRE_WARNING;
			return;
		}

		burnerOverlayState = BurnerOverlayState.OFF;
	}

	private void lookupPlayer(String playerName, Tile tile)
	{
		executor.execute(() ->
		{
			try
			{
				final HiscoreResult playerStats = hiscoreManager.lookup(playerName, HiscoreEndpoint.NORMAL);
				if (playerStats == null)
				{
					log.debug("Hiscore lookup for {} failed. Burner timer will use local player's level as fallback.",
						playerName);
					return;
				}
				final net.runelite.client.hiscore.Skill fm = playerStats.getSkill(HiscoreSkill.FIREMAKING);
				final int level = fm.getLevel();
				log.debug("Hiscore lookup for {} successful, Firemaking level: {}", playerName, level);

				clientThread.invokeLater(() ->
				{
					BurnerState state = litBurners.get(tile);
					if (state != null)
					{
						int correctDuration = 200 + Math.max(level, 1);
						log.debug("Setting burner duration on tile {} with correct level {}. Duration: {}",
							tile.getWorldLocation(), level, correctDuration);
						state.certainDurationTicks = correctDuration;
					}
				});
			}
			catch (IOException e)
			{
				log.warn("Error fetching Hiscore data for {}: {}", playerName, e.getMessage());
			}
		});
	}

	private void playFinalAlarmSound()
	{
		try
		{
			audioPlayer.play(BurnerAlarmPlugin.class, BurnerAlarmConstants.FINAL_ALARM_SOUND_FILE,
				config.finalAlarmVolume());
		}
		catch (Exception e)
		{
			log.warn("Failed to play POH Assistant final alarm sound", e);
		}
	}
}

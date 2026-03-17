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

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JOptionPane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;

@Slf4j
@Singleton
public class HouseDetectionService
{
	private final Client client;
	private final ClientThread clientThread;
	private final BurnerAlarmConfig config;
	private final ConfigManager configManager;
	private final Gson gson;

	private HouseFingerprint savedFingerprint = null;
	@Getter
	private boolean isMyPOH = false;
	private boolean wasInPOH = false;

	@Inject
	public HouseDetectionService(Client client, ClientThread clientThread,
		BurnerAlarmConfig config, ConfigManager configManager, Gson gson)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.config = config;
		this.configManager = configManager;
		this.gson = gson;
	}

	public void reset()
	{
		savedFingerprint = null;
		isMyPOH = false;
		wasInPOH = false;
	}

	public void loadFingerprint()
	{
		if (client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null)
		{
			log.debug("Not logged in, cannot load fingerprint.");
			return;
		}
		final String fingerprintKey = "pohFingerprint_" + client.getLocalPlayer().getName();
		final String json = configManager.getConfiguration("burneralarm", fingerprintKey);

		if (json != null && !json.isEmpty())
		{
			try
			{
				savedFingerprint = gson.fromJson(json, HouseFingerprint.class);
				log.debug("Loaded house fingerprint for {}: {} unique objects.",
					client.getLocalPlayer().getName(),
					savedFingerprint.getObjectCounts().size());
			}
			catch (Exception e)
			{
				log.warn("Failed to load house fingerprint, resetting data for {}.",
					client.getLocalPlayer().getName(), e);
				configManager.unsetConfiguration("burneralarm", fingerprintKey);
			}
		}
		else
		{
			savedFingerprint = null;
			log.debug("No saved fingerprint found for {}.", client.getLocalPlayer().getName());
		}
	}

	public boolean isInPOH()
	{
		return isAnyPOHFeaturePresent();
	}

	public boolean shouldRunFeature(boolean allowInGuestPOH)
	{
		if (!isInPOH())
		{
			return false;
		}
		return isMyPOH || allowInGuestPOH;
	}

	/**
	 * Called each game tick to detect POH entry/exit transitions and update ownership state.
	 */
	public void onGameTick()
	{
		boolean currentlyInPOH = isInPOH();
		if (currentlyInPOH && !wasInPOH)
		{
			log.debug("POH entry detected via GameTick. Performing one-time fingerprint check.");
			if (config.advancedPohDetectionEnabled())
			{
				checkIfInMyPOH();
			}
			else
			{
				isMyPOH = currentlyInPOH;
			}
		}
		else if (!currentlyInPOH && wasInPOH)
		{
			isMyPOH = false;
			log.debug("POH exit detected via GameTick. Resetting house ownership status.");
		}
		wasInPOH = currentlyInPOH;
	}

	public void onAdvancedDetectionEnabled()
	{
		loadFingerprint();
		checkIfInMyPOH();
	}

	public void onAdvancedDetectionDisabled()
	{
		savedFingerprint = null;
		isMyPOH = false;
		wasInPOH = false;
	}

	public void scanHouse()
	{
		log.debug("Starting house scan process (triggered by config toggle).");
		if (client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
				BurnerAlarmConstants.PLUGIN_PREFIX + "You must be logged in to scan your house.", null);
			log.debug("Scan failed: Not logged in.");
			return;
		}
		if (!isInPOH())
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
				BurnerAlarmConstants.PLUGIN_PREFIX + "You must be inside your Player-Owned House to scan.", null);
			log.debug("Scan failed: Not detected in any POH (no key POH objects found).");
			return;
		}

		int result = JOptionPane.showConfirmDialog(
			null,
			"<html><p><b>Before scanning, ensure you are standing on the EXACT TILE you land on"
				+ " when entering the POH portal.</b></p>"
				+ "<p>Scanning saves a unique 'fingerprint' of objects around you to differentiate"
				+ " your house from others.</p></html>",
			"Confirm House Scan",
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE
		);
		if (result != JOptionPane.OK_OPTION)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
				BurnerAlarmConstants.PLUGIN_PREFIX + "House scan cancelled.", null);
			log.debug("Scan cancelled by user confirmation dialog.");
			return;
		}

		clientThread.invokeLater(() ->
		{
			LocalPoint playerLocalLocation = client.getLocalPlayer().getLocalLocation();
			if (playerLocalLocation == null)
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
					BurnerAlarmConstants.PLUGIN_PREFIX + "Could not determine player location. Scan failed.", null);
				log.debug("Scan failed: LocalPoint is null for local player.");
				return;
			}

			Map<Integer, Integer> objectIdCounts = scanObjectsInRadius(playerLocalLocation);

			if (objectIdCounts.isEmpty())
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
					BurnerAlarmConstants.PLUGIN_PREFIX + "Could not find any static objects to form a fingerprint. Scan failed.", null);
				log.debug("Scan failed: No static objects found in the loaded scene radius.");
				return;
			}

			HouseFingerprint fingerprint = new HouseFingerprint(objectIdCounts);
			final String fingerprintKey = "pohFingerprint_" + client.getLocalPlayer().getName();
			configManager.setConfiguration("burneralarm", fingerprintKey, gson.toJson(fingerprint));
			this.savedFingerprint = fingerprint;
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
				BurnerAlarmConstants.PLUGIN_PREFIX + "House fingerprint saved successfully! Advanced detection is now active for this house.", null);
			log.debug("Scan successful. Fingerprint saved with: {} unique objects.", objectIdCounts.size());

			checkIfInMyPOH();
		});
	}

	private void checkIfInMyPOH()
	{
		if (!config.advancedPohDetectionEnabled() || savedFingerprint == null)
		{
			isMyPOH = false;
			return;
		}

		if (!isInPOH())
		{
			isMyPOH = false;
			return;
		}

		LocalPoint currentPlayerLocalLocation = client.getLocalPlayer().getLocalLocation();
		if (currentPlayerLocalLocation == null)
		{
			isMyPOH = false;
			log.debug("POH check failed: Player local location is null.");
			return;
		}

		Map<Integer, Integer> currentObjectIdCounts = scanObjectsInRadius(currentPlayerLocalLocation);

		if (!currentObjectIdCounts.equals(savedFingerprint.getObjectCounts()))
		{
			log.debug("POH check failed: Object count map mismatch. Current objects: {}, Saved objects: {}",
				currentObjectIdCounts.size(), savedFingerprint.getObjectCounts().size());

			if (log.isDebugEnabled())
			{
				savedFingerprint.getObjectCounts().forEach((id, count) ->
				{
					if (currentObjectIdCounts.getOrDefault(id, 0) < count)
					{
						log.debug("Fingerprint mismatch: Missing object ID {} (Current: {}, Saved: {})",
							id, currentObjectIdCounts.getOrDefault(id, 0), count);
					}
				});
				currentObjectIdCounts.forEach((id, count) ->
				{
					if (savedFingerprint.getObjectCounts().getOrDefault(id, 0) < count)
					{
						log.debug("Fingerprint mismatch: Extra object ID {} (Current: {}, Saved: {})",
							id, count, savedFingerprint.getObjectCounts().getOrDefault(id, 0));
					}
				});
			}

			isMyPOH = false;
			return;
		}

		isMyPOH = true;
		log.debug("POH check successful: House matches saved fingerprint.");
	}

	private Map<Integer, Integer> scanObjectsInRadius(LocalPoint center)
	{
		Map<Integer, Integer> objectIdCounts = new HashMap<>();
		net.runelite.api.Scene scene = client.getTopLevelWorldView().getScene();
		int maxDistance = BurnerAlarmConstants.POH_DETECTION_RADIUS * 128;

		for (int x = 0; x < BurnerAlarmConstants.SCENE_SIZE; x++)
		{
			for (int y = 0; y < BurnerAlarmConstants.SCENE_SIZE; y++)
			{
				for (int plane = 0; plane < BurnerAlarmConstants.MAX_PLANE; plane++)
				{
					Tile tile = scene.getTiles()[plane][x][y];
					if (tile == null)
					{
						continue;
					}
					if (tile.getLocalLocation().distanceTo(center) > maxDistance)
					{
						continue;
					}

					if (tile.getWallObject() != null)
					{
						objectIdCounts.merge(tile.getWallObject().getId(), 1, Integer::sum);
					}
					if (tile.getDecorativeObject() != null)
					{
						objectIdCounts.merge(tile.getDecorativeObject().getId(), 1, Integer::sum);
					}
					if (tile.getGroundObject() != null)
					{
						objectIdCounts.merge(tile.getGroundObject().getId(), 1, Integer::sum);
					}
				}
			}
		}
		return objectIdCounts;
	}

	private boolean isAnyPOHFeaturePresent()
	{
		final Set<Integer> detectionIds = BurnerAlarmConstants.POH_DETECTION_OBJECT_IDS;
		for (int x = 0; x < BurnerAlarmConstants.SCENE_SIZE; x++)
		{
			for (int y = 0; y < BurnerAlarmConstants.SCENE_SIZE; y++)
			{
				for (int plane = 0; plane < BurnerAlarmConstants.MAX_PLANE; plane++)
				{
					Tile tile = client.getTopLevelWorldView().getScene().getTiles()[plane][x][y];
					if (tile == null)
					{
						continue;
					}

					if (tile.getGameObjects() != null)
					{
						for (GameObject gameObject : tile.getGameObjects())
						{
							if (gameObject != null && detectionIds.contains(gameObject.getId()))
							{
								return true;
							}
						}
					}

					if (tile.getWallObject() != null)
					{
						if (detectionIds.contains(tile.getWallObject().getId()))
						{
							return true;
						}
					}
				}
			}
		}
		return false;
	}
}

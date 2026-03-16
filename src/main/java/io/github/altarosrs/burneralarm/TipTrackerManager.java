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
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.SwingUtilities;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;

@Slf4j
public class TipTrackerManager
{
	private static final String CONFIG_GROUP = "burneralarm";
	private static final String TIPS_KEY_PREFIX = "tips_";
	private static final String LEGACY_KEY = "tipTrackerData";
	private static final String ALLTIME_LEADERBOARD_KEY = "leaderboard_alltime";
	private static final String ALLTIME_TOTAL_KEY = "alltime_total";
	private static final Type TIP_LIST_TYPE = new TypeToken<List<Tip>>() {}.getType();
	private static final Type LEGACY_TIP_MAP_TYPE = new TypeToken<Map<String, List<Tip>>>() {}.getType();
	private static final Type LEADERBOARD_MAP_TYPE = new TypeToken<Map<String, long[]>>() {}.getType();
	private static final int SAVE_INTERVAL_TICKS = 50;
	private static final int SESSION_TIMEOUT_MINUTES = 5;
	private static final int TIP_RETENTION_MONTHS = 6;

	private final List<Tip> tips = new ArrayList<>();
	private final Set<String> dirtyMonths = new HashSet<>();
	// Persistent aggregated leaderboard: playerName -> [totalAmount, tipCount]
	// Survives the 6-month tip pruning so all-time totals are truly all-time
	private final Map<String, long[]> allTimeLeaderboard = new HashMap<>();
	private long allTimeTotal = 0;
	private boolean aggregateDirty = false;
	private int saveTickCounter = 0;
	private String profileKey;

	@Setter
	private TipTrackerPanel panel;
	@Setter
	private PlayerHighlightService playerHighlightService;

	private final Client client;
	private final ConfigManager configManager;

	@Getter
	private final BurnerAlarmConfig config;

	private final Gson gson;

	private boolean loginFlag = false;

	@Getter
	private Instant sessionStartTime;

	private Instant lastLogoutTime;

	public TipTrackerManager(Client client, ConfigManager configManager, BurnerAlarmConfig config, Gson gson)
	{
		this.client = client;
		this.configManager = configManager;
		this.config = config;
		this.gson = gson;
	}

	public void init()
	{
		profileKey = configManager.getRSProfileKey();
		if (profileKey != null)
		{
			loadTips();
		}
		sessionStartTime = Instant.now();
	}

	public void shutdown()
	{
		flushDirtyMonths();
		flushAggregate();
		tips.clear();
		dirtyMonths.clear();
		allTimeLeaderboard.clear();
		allTimeTotal = 0;
		aggregateDirty = false;
		profileKey = null;
		panel = null;
	}

	public void clearAllTips()
	{
		if (profileKey == null)
		{
			return;
		}

		for (String key : configManager.getRSProfileConfigurationKeys(CONFIG_GROUP, profileKey, TIPS_KEY_PREFIX))
		{
			configManager.unsetConfiguration(CONFIG_GROUP, profileKey, key);
		}

		configManager.unsetConfiguration(CONFIG_GROUP, profileKey, ALLTIME_LEADERBOARD_KEY);
		configManager.unsetConfiguration(CONFIG_GROUP, profileKey, ALLTIME_TOTAL_KEY);

		tips.clear();
		dirtyMonths.clear();
		allTimeLeaderboard.clear();
		allTimeTotal = 0;
		aggregateDirty = false;
		updatePanelAndHighlights();
	}

	private void loadTips()
	{
		tips.clear();
		dirtyMonths.clear();
		allTimeLeaderboard.clear();
		allTimeTotal = 0;
		aggregateDirty = false;

		migrateLegacyData();

		if (profileKey == null)
		{
			return;
		}

		for (String key : configManager.getRSProfileConfigurationKeys(CONFIG_GROUP, profileKey, TIPS_KEY_PREFIX))
		{
			String json = configManager.getConfiguration(CONFIG_GROUP, profileKey, key);
			if (json == null || json.isEmpty())
			{
				continue;
			}

			try
			{
				List<Tip> monthTips = gson.fromJson(json, TIP_LIST_TYPE);
				if (monthTips != null)
				{
					tips.addAll(monthTips);
				}
			}
			catch (JsonSyntaxException e)
			{
				log.warn("Removing tip data with malformed json for key {}: {}", key, e.getMessage());
				configManager.unsetConfiguration(CONFIG_GROUP, profileKey, key);
			}
		}

		tips.sort((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));

		loadAggregate();
		pruneOldTips();
	}

	private void loadAggregate()
	{
		String leaderboardJson = configManager.getConfiguration(CONFIG_GROUP, profileKey, ALLTIME_LEADERBOARD_KEY);
		String totalJson = configManager.getConfiguration(CONFIG_GROUP, profileKey, ALLTIME_TOTAL_KEY);

		if (leaderboardJson != null && !leaderboardJson.isEmpty())
		{
			try
			{
				Map<String, long[]> loaded = gson.fromJson(leaderboardJson, LEADERBOARD_MAP_TYPE);
				if (loaded != null)
				{
					allTimeLeaderboard.putAll(loaded);
				}
			}
			catch (JsonSyntaxException e)
			{
				log.warn("Failed to load all-time leaderboard, rebuilding: {}", e.getMessage());
			}
		}

		if (totalJson != null && !totalJson.isEmpty())
		{
			try
			{
				allTimeTotal = Long.parseLong(totalJson);
			}
			catch (NumberFormatException e)
			{
				log.warn("Failed to load all-time total, rebuilding: {}", e.getMessage());
			}
		}

		// If no aggregate exists yet, build it from current tips (one-time migration)
		if (allTimeLeaderboard.isEmpty() && !tips.isEmpty())
		{
			rebuildAggregateFromTips();
		}
	}

	private void rebuildAggregateFromTips()
	{
		allTimeLeaderboard.clear();
		allTimeTotal = 0;

		for (Tip tip : tips)
		{
			allTimeTotal += tip.getAmount();

			if (!tip.getPlayerName().equalsIgnoreCase("Manual"))
			{
				allTimeLeaderboard.merge(tip.getPlayerName(),
					new long[]{tip.getAmount(), 1},
					(existing, added) ->
					{
						existing[0] += added[0];
						existing[1] += added[1];
						return existing;
					});
			}
		}

		aggregateDirty = true;
		log.debug("Built all-time aggregate from {} tips, {} leaderboard entries",
			tips.size(), allTimeLeaderboard.size());
	}

	private void migrateLegacyData()
	{
		String legacyJson = configManager.getConfiguration(CONFIG_GROUP, LEGACY_KEY);
		if (legacyJson == null || legacyJson.isEmpty())
		{
			return;
		}

		log.info("Migrating legacy tip tracker data to monthly keys");

		try
		{
			Map<String, List<Tip>> legacyTips = gson.fromJson(legacyJson, LEGACY_TIP_MAP_TYPE);
			if (legacyTips == null)
			{
				return;
			}

			String playerName = null;
			if (client.getLocalPlayer() != null)
			{
				playerName = client.getLocalPlayer().getName();
			}

			if (playerName != null && legacyTips.containsKey(playerName))
			{
				List<Tip> characterTips = legacyTips.get(playerName);
				if (characterTips != null)
				{
					tips.addAll(characterTips);
					markAllMonthsDirty();
					flushDirtyMonths();
				}
			}

			configManager.unsetConfiguration(CONFIG_GROUP, LEGACY_KEY);
			log.info("Legacy tip data migration complete");
		}
		catch (Exception e)
		{
			log.warn("Failed to migrate legacy tip data: {}", e.getMessage());
		}
	}

	private void flushDirtyMonths()
	{
		if (profileKey == null || dirtyMonths.isEmpty())
		{
			return;
		}

		Map<String, List<Tip>> tipsByMonth = groupTipsByMonth();

		for (String month : dirtyMonths)
		{
			String key = TIPS_KEY_PREFIX + month;
			List<Tip> monthTips = tipsByMonth.get(month);

			if (monthTips == null || monthTips.isEmpty())
			{
				configManager.unsetConfiguration(CONFIG_GROUP, profileKey, key);
			}
			else
			{
				String json = gson.toJson(monthTips);
				configManager.setConfiguration(CONFIG_GROUP, profileKey, key, json);
			}
		}

		dirtyMonths.clear();
	}

	private void flushAggregate()
	{
		if (profileKey == null || !aggregateDirty)
		{
			return;
		}

		configManager.setConfiguration(CONFIG_GROUP, profileKey, ALLTIME_LEADERBOARD_KEY,
			gson.toJson(allTimeLeaderboard));
		configManager.setConfiguration(CONFIG_GROUP, profileKey, ALLTIME_TOTAL_KEY,
			String.valueOf(allTimeTotal));

		aggregateDirty = false;
	}

	private Map<String, List<Tip>> groupTipsByMonth()
	{
		Map<String, List<Tip>> tipsByMonth = new HashMap<>();
		for (Tip tip : tips)
		{
			String monthKey = getMonthKey(tip.getTimestamp());
			tipsByMonth.computeIfAbsent(monthKey, k -> new ArrayList<>()).add(tip);
		}
		return tipsByMonth;
	}

	private String getMonthKey(Instant timestamp)
	{
		YearMonth ym = YearMonth.from(timestamp.atZone(ZoneId.systemDefault()));
		return ym.toString();
	}

	private void markAllMonthsDirty()
	{
		for (Tip tip : tips)
		{
			dirtyMonths.add(getMonthKey(tip.getTimestamp()));
		}
	}

	private void pruneOldTips()
	{
		if (profileKey == null)
		{
			return;
		}

		YearMonth cutoff = YearMonth.now().minusMonths(TIP_RETENTION_MONTHS);
		Instant cutoffInstant = cutoff.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

		Set<String> monthsToDelete = new HashSet<>();

		tips.removeIf(tip ->
		{
			if (tip.getTimestamp().isBefore(cutoffInstant))
			{
				monthsToDelete.add(getMonthKey(tip.getTimestamp()));
				return true;
			}
			return false;
		});

		for (String month : monthsToDelete)
		{
			String key = TIPS_KEY_PREFIX + month;
			configManager.unsetConfiguration(CONFIG_GROUP, profileKey, key);
			log.debug("Pruned old tip data for month: {}", month);
		}
	}

	public void onGameStateChanged(GameStateChanged event)
	{
		GameState gameState = event.getGameState();
		if (gameState == GameState.LOGIN_SCREEN || gameState == GameState.CONNECTION_LOST)
		{
			lastLogoutTime = Instant.now();
		}
		else if (gameState == GameState.LOGGED_IN)
		{
			String newProfileKey = configManager.getRSProfileKey();
			if (newProfileKey != null && !newProfileKey.equals(profileKey))
			{
				if (profileKey != null)
				{
					flushDirtyMonths();
					flushAggregate();
				}
				profileKey = newProfileKey;
				loadTips();
			}

			if (lastLogoutTime != null)
			{
				long minutesSinceLogout = ChronoUnit.MINUTES.between(lastLogoutTime, Instant.now());
				if (minutesSinceLogout >= SESSION_TIMEOUT_MINUTES)
				{
					sessionStartTime = Instant.now();
				}
			}
			loginFlag = true;
		}
	}

	public void onGameTick()
	{
		if (loginFlag)
		{
			loginFlag = false;
			updatePanelAndHighlights();
		}

		saveTickCounter++;
		if (saveTickCounter >= SAVE_INTERVAL_TICKS)
		{
			if (!dirtyMonths.isEmpty())
			{
				flushDirtyMonths();
			}
			if (aggregateDirty)
			{
				flushAggregate();
			}
			saveTickCounter = 0;
		}
	}

	public List<LeaderboardEntry> getLeaderboard(Instant timeFilter)
	{
		Map<String, LeaderboardEntry> leaderboardMap = new HashMap<>();

		for (Tip tip : tips)
		{
			if (!tip.getTimestamp().isAfter(timeFilter))
			{
				continue;
			}
			if (tip.getPlayerName().equalsIgnoreCase("Manual"))
			{
				continue;
			}
			leaderboardMap.compute(tip.getPlayerName(), (name, entry) ->
			{
				if (entry == null)
				{
					return new LeaderboardEntry(name, tip.getAmount(), 1);
				}
				return new LeaderboardEntry(name,
					entry.getTotalAmount() + tip.getAmount(),
					entry.getTipCount() + 1);
			});
		}

		List<LeaderboardEntry> leaderboard = new ArrayList<>(leaderboardMap.values());
		leaderboard.sort((e1, e2) -> Long.compare(e2.getTotalAmount(), e1.getTotalAmount()));
		return leaderboard;
	}

	public List<LeaderboardEntry> getAllTimeLeaderboard()
	{
		List<LeaderboardEntry> leaderboard = new ArrayList<>();
		for (Map.Entry<String, long[]> entry : allTimeLeaderboard.entrySet())
		{
			long[] data = entry.getValue();
			leaderboard.add(new LeaderboardEntry(entry.getKey(), data[0], (int) data[1]));
		}
		leaderboard.sort((e1, e2) -> Long.compare(e2.getTotalAmount(), e1.getTotalAmount()));
		return leaderboard;
	}

	public Color getColorForAmount(long amount)
	{
		if (amount >= config.tipJarTier1Threshold())
		{
			return config.tipJarTier1Color();
		}
		if (amount >= config.tipJarTier2Threshold())
		{
			return config.tipJarTier2Color();
		}
		if (amount >= config.tipJarTier3Threshold())
		{
			return config.tipJarTier3Color();
		}
		return Color.WHITE;
	}

	public long getSessionTotal()
	{
		if (sessionStartTime == null)
		{
			return 0;
		}
		return tips.stream()
			.filter(tip -> tip.getTimestamp().isAfter(sessionStartTime))
			.mapToLong(Tip::getAmount)
			.sum();
	}

	public long getWeeklyTotal()
	{
		Instant oneWeekAgo = Instant.now().minus(7, ChronoUnit.DAYS);
		return tips.stream()
			.filter(tip -> tip.getTimestamp().isAfter(oneWeekAgo))
			.mapToLong(Tip::getAmount)
			.sum();
	}

	public long getMonthlyTotal()
	{
		Instant oneMonthAgo = Instant.now().minus(30, ChronoUnit.DAYS);
		return tips.stream()
			.filter(tip -> tip.getTimestamp().isAfter(oneMonthAgo))
			.mapToLong(Tip::getAmount)
			.sum();
	}

	public long getAllTimeTotal()
	{
		return allTimeTotal;
	}

	public List<Tip> getTips()
	{
		return Collections.unmodifiableList(tips);
	}

	public void addTip(Tip tip)
	{
		tips.add(tip);
		dirtyMonths.add(getMonthKey(tip.getTimestamp()));

		allTimeTotal += tip.getAmount();
		if (!tip.getPlayerName().equalsIgnoreCase("Manual"))
		{
			allTimeLeaderboard.merge(tip.getPlayerName(),
				new long[]{tip.getAmount(), 1},
				(existing, added) ->
				{
					existing[0] += added[0];
					existing[1] += added[1];
					return existing;
				});
		}
		aggregateDirty = true;

		updatePanelAndHighlights();
	}

	public void deleteTip(Tip tip)
	{
		tips.remove(tip);
		dirtyMonths.add(getMonthKey(tip.getTimestamp()));

		allTimeTotal -= tip.getAmount();
		if (!tip.getPlayerName().equalsIgnoreCase("Manual"))
		{
			long[] entry = allTimeLeaderboard.get(tip.getPlayerName());
			if (entry != null)
			{
				entry[0] -= tip.getAmount();
				entry[1]--;
				if (entry[1] <= 0)
				{
					allTimeLeaderboard.remove(tip.getPlayerName());
				}
			}
		}
		aggregateDirty = true;

		updatePanelAndHighlights();
	}

	public void updateTipAmount(Tip tip, long newAmount)
	{
		long oldAmount = tip.getAmount();
		long delta = newAmount - oldAmount;

		tip.setAmount(newAmount);
		dirtyMonths.add(getMonthKey(tip.getTimestamp()));

		allTimeTotal += delta;
		if (!tip.getPlayerName().equalsIgnoreCase("Manual"))
		{
			long[] entry = allTimeLeaderboard.get(tip.getPlayerName());
			if (entry != null)
			{
				entry[0] += delta;
			}
		}
		aggregateDirty = true;

		updatePanelAndHighlights();
	}

	private void updatePanelAndHighlights()
	{
		if (panel != null)
		{
			SwingUtilities.invokeLater(() -> panel.rebuild());
		}
		if (playerHighlightService != null)
		{
			playerHighlightService.updatePermanentHighlights();
		}
	}
}

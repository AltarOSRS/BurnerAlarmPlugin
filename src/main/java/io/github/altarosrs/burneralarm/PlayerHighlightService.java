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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.client.util.Text;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PlayerHighlightService
{
	private final Client client;
	private final BurnerAlarmConfig config;

	private final Map<String, HighlightInfo> temporaryHighlights = new HashMap<>();
	private final Map<String, Color> permanentHighlights = new HashMap<>();
	private TipTrackerManager tipTrackerManager;

	public void setTipTrackerManager(TipTrackerManager tipTrackerManager)
	{
		this.tipTrackerManager = tipTrackerManager;
		updatePermanentHighlights();
	}

	private int getHighlightTierValue(Color color, boolean isPermanent)
	{
		if (color == null)
		{
			return 0;
		}

		int permanentBonus = isPermanent ? 100 : 0;

		if (color.equals(config.tipJarTier1Color()))
		{
			return 3 + permanentBonus;
		}
		if (color.equals(config.tipJarTier2Color()) || color.equals(config.tradeTipColor()))
		{
			return 2 + permanentBonus;
		}
		if (color.equals(config.tipJarTier3Color()))
		{
			return 1 + permanentBonus;
		}
		if (color.equals(config.levelUpColor())
			|| color.equals(config.level99Color())
			|| color.equals(config.level126CombatColor()))
		{
			return 999;
		}

		return 0;
	}

	public void addTemporaryHighlight(String playerName, Color color)
	{
		Instant expireTime = Instant.now().plus(config.highlightCooldown(), ChronoUnit.MINUTES);
		temporaryHighlights.put(Text.sanitize(playerName), new HighlightInfo(color, expireTime));
	}

	public void removeExpiredHighlights()
	{
		temporaryHighlights.entrySet().removeIf(
			entry -> entry.getValue().getExpireTime().isBefore(Instant.now()));
	}

	public void updatePermanentHighlights()
	{
		permanentHighlights.clear();

		if (tipTrackerManager == null || !config.highlightTopTippers())
		{
			return;
		}

		List<LeaderboardEntry> leaderboard = tipTrackerManager.getAllTimeLeaderboard();
		int limit = Math.min(leaderboard.size(), 100);

		for (int i = 0; i < limit; i++)
		{
			LeaderboardEntry entry = leaderboard.get(i);
			Color color = tipTrackerManager.getColorForAmount(entry.getTotalAmount());
			if (color != Color.WHITE)
			{
				permanentHighlights.put(Text.sanitize(entry.getPlayerName()), color);
			}
		}
	}

	public Color getHighlightColor(Player player)
	{
		if (player == null || player.getName() == null)
		{
			return null;
		}

		String sanitizedName = Text.sanitize(player.getName());

		HighlightInfo tempHighlight = temporaryHighlights.get(sanitizedName);
		Color tempColor = (tempHighlight != null) ? tempHighlight.getColor() : null;

		Color permColor = null;
		if (config.highlightTopTippers())
		{
			permColor = permanentHighlights.get(sanitizedName);
		}

		if (tempColor == null && permColor == null)
		{
			return null;
		}
		if (tempColor == null)
		{
			return permColor;
		}
		if (permColor == null)
		{
			return tempColor;
		}

		int tempTier = getHighlightTierValue(tempColor, false);
		int permTier = getHighlightTierValue(permColor, true);
		return (tempTier >= permTier) ? tempColor : permColor;
	}

	public Collection<Player> getPlayersToHighlight()
	{
		Set<String> namesToHighlight = new HashSet<>(temporaryHighlights.keySet());
		if (config.highlightTopTippers())
		{
			namesToHighlight.addAll(permanentHighlights.keySet());
		}

		Set<Player> players = new HashSet<>();
		for (Player player : client.getTopLevelWorldView().players())
		{
			if (player != null && player.getName() != null)
			{
				String sanitizedName = Text.sanitize(player.getName());
				if (namesToHighlight.contains(sanitizedName))
				{
					players.add(player);
				}
			}
		}
		return players;
	}
}

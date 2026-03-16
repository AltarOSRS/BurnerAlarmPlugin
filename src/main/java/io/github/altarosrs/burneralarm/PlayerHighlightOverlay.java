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
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Collection;
import javax.inject.Inject;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.Text;

public class PlayerHighlightOverlay extends Overlay
{
	private final BurnerAlarmConfig config;
	private final PlayerHighlightService playerHighlightService;
	private final HouseDetectionService houseDetectionService;

	@Inject
	public PlayerHighlightOverlay(BurnerAlarmConfig config,
		PlayerHighlightService playerHighlightService,
		HouseDetectionService houseDetectionService)
	{
		this.config = config;
		this.playerHighlightService = playerHighlightService;
		this.houseDetectionService = houseDetectionService;
		setPosition(OverlayPosition.DYNAMIC);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!houseDetectionService.shouldRunFeature(config.allowFeaturesInGuestPOH()))
		{
			return null;
		}

		Collection<Player> playersToHighlight = playerHighlightService.getPlayersToHighlight();
		for (Player player : playersToHighlight)
		{
			if (config.excludeFriendsAndClan()
				&& (player.isFriend() || player.isFriendsChatMember() || player.isClanMember()))
			{
				continue;
			}

			Color highlightColor = playerHighlightService.getHighlightColor(player);
			if (highlightColor == null)
			{
				continue;
			}

			if (config.drawPlayerTiles())
			{
				OverlayUtil.renderPolygon(graphics, player.getCanvasTilePoly(), highlightColor);
			}

			if (config.drawPlayerNames())
			{
				String name = player.getName();
				if (name == null)
				{
					continue;
				}

				name = Text.sanitize(name);
				Point textLocation = player.getCanvasTextLocation(
					graphics, name, player.getLogicalHeight() + 40);
				if (textLocation != null)
				{
					OverlayUtil.renderTextLocation(graphics, textLocation, name, highlightColor);
				}
			}
		}
		return null;
	}
}

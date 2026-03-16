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

import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.util.Map;

public class BurnerAlarmOverlay extends Overlay
{
	private final Client client;
	private final BurnerService burnerService;
	private final BurnerAlarmConfig config;
	private final ModelOutlineRenderer modelOutlineRenderer;

	@Inject
	public BurnerAlarmOverlay(Client client, BurnerService burnerService,
		BurnerAlarmConfig config, ModelOutlineRenderer modelOutlineRenderer)
	{
		this.client = client;
		this.burnerService = burnerService;
		this.config = config;
		this.modelOutlineRenderer = modelOutlineRenderer;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		boolean flashCycleOn = client.getGameCycle() % 40 >= 20;

		if (config.highlightUnlitBurners())
		{
			if (flashCycleOn)
			{
				for (GameObject burner : burnerService.getUnlitBurners().values())
				{
					renderHighlight(graphics, burner,
						BurnerAlarmConstants.UNLIT_BURNER_COLOR,
						config.unlitBurnerHighlightStyle());
				}
			}
		}

		if (config.highlightLitBurners())
		{
			for (Map.Entry<Tile, GameObject> entry : burnerService.getLitBurnerObjects().entrySet())
			{
				Tile burnerTile = entry.getKey();
				GameObject burnerObject = entry.getValue();
				BurnerService.BurnerState state = burnerService.getLitBurners().get(burnerTile);

				boolean isRandomBurnout = state != null && state.finalAlarmSent;
				boolean isPreWarning = state != null && state.preNotificationSent;
				Color colorToUse;
				boolean shouldFlash;

				if (isRandomBurnout)
				{
					colorToUse = BurnerAlarmConstants.RANDOM_BURNOUT_COLOR;
					shouldFlash = true;
				}
				else if (isPreWarning)
				{
					colorToUse = BurnerAlarmConstants.LIT_BURNER_COLOR;
					shouldFlash = true;
				}
				else
				{
					colorToUse = BurnerAlarmConstants.LIT_BURNER_COLOR;
					shouldFlash = false;
				}

				if (!shouldFlash || flashCycleOn)
				{
					renderHighlight(graphics, burnerObject,
						colorToUse,
						config.litBurnerHighlightStyle());
				}
			}
		}

		return null;
	}

	private void renderHighlight(Graphics2D graphics, TileObject gameObject,
		Color color, BurnerAlarmConfig.HighlightStyle style)
	{
		if (gameObject == null
			|| gameObject.getPlane() != client.getTopLevelWorldView().getPlane())
		{
			return;
		}

		switch (style)
		{
			case HULL:
				final Shape hull;
				Shape hull2 = null;

				if (gameObject instanceof GameObject)
				{
					hull = ((GameObject) gameObject).getConvexHull();
				}
				else if (gameObject instanceof WallObject)
				{
					hull = ((WallObject) gameObject).getConvexHull();
					hull2 = ((WallObject) gameObject).getConvexHull2();
				}
				else if (gameObject instanceof DecorativeObject)
				{
					hull = ((DecorativeObject) gameObject).getConvexHull();
					hull2 = ((DecorativeObject) gameObject).getConvexHull2();
				}
				else if (gameObject instanceof GroundObject)
				{
					hull = ((GroundObject) gameObject).getConvexHull();
				}
				else
				{
					hull = gameObject.getCanvasTilePoly();
				}

				if (hull != null)
				{
					OverlayUtil.renderPolygon(graphics, hull, color);
				}
				if (hull2 != null)
				{
					OverlayUtil.renderPolygon(graphics, hull2, color);
				}
				break;
			case OUTLINE:
				modelOutlineRenderer.drawOutline(gameObject, 2, color, 0);
				break;
			case CLICKBOX:
				Shape clickbox = gameObject.getClickbox();
				if (clickbox != null)
				{
					OverlayUtil.renderPolygon(graphics, clickbox, color);
				}
				break;
			case TILE:
				Polygon poly = gameObject.getCanvasTilePoly();
				if (poly != null)
				{
					OverlayUtil.renderPolygon(graphics, poly, color);
				}
				break;
		}
	}
}

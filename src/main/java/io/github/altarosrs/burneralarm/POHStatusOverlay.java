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

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

public class POHStatusOverlay extends OverlayPanel
{
	private final HouseDetectionService houseDetectionService;
	private final BurnerAlarmConfig config;

	@Inject
	private POHStatusOverlay(BurnerAlarmPlugin plugin, HouseDetectionService houseDetectionService,
		BurnerAlarmConfig config)
	{
		super(plugin);
		this.houseDetectionService = houseDetectionService;
		this.config = config;
		setPosition(OverlayPosition.TOP_RIGHT);
		setPriority(Overlay.PRIORITY_HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.pohStatusOverlayEnabled() || !houseDetectionService.isInPOH())
		{
			return null;
		}

		panelComponent.getChildren().clear();
		panelComponent.setPreferredSize(new Dimension(150, 0));

		String title;
		Color color;
		if (!config.advancedPohDetectionEnabled())
		{
			title = "POH Detected";
			color = Color.YELLOW;
		}
		else if (houseDetectionService.isMyPOH())
		{
			title = "My POH (Host)";
			color = Color.GREEN;
		}
		else
		{
			title = "Guest POH";
			color = Color.RED;
		}

		panelComponent.setBackgroundColor(
			new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Status:")
			.right(title)
			.rightColor(color)
			.build());
		return super.render(graphics);
	}
}

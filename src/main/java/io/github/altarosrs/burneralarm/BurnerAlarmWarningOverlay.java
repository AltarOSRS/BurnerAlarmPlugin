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
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

public class BurnerAlarmWarningOverlay extends Overlay
{
	private final Client client;
	private final BurnerService burnerService;
	private final BurnerAlarmConfig config;
	private final PanelComponent panelComponent = new PanelComponent();

	@Inject
	private BurnerAlarmWarningOverlay(Client client, BurnerService burnerService,
		BurnerAlarmConfig config)
	{
		this.client = client;
		this.burnerService = burnerService;
		this.config = config;
		setPosition(OverlayPosition.TOP_CENTER);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		BurnerService.BurnerOverlayState state = burnerService.getBurnerOverlayState();
		if (state == BurnerService.BurnerOverlayState.OFF)
		{
			return null;
		}

		panelComponent.getChildren().clear();
		panelComponent.setPreferredSize(new Dimension(220, 0));

		String title;
		String message;
		Color backgroundColor;

		switch (state)
		{
			case PRE_WARNING:
				title = "Burner Pre-Warning!";
				message = "A burner will go out soon.";
				Color preWarningColor = config.burnerPreWarningColor();
				backgroundColor = new Color(
					preWarningColor.getRed(), preWarningColor.getGreen(),
					preWarningColor.getBlue(), 200);
				break;
			case CAN_EXTINGUISH:
				title = "Burner Alarm!";
				message = "A burner may extinguish now.";
				backgroundColor = new Color(220, 0, 0, 200);
				break;
			case FINAL_ALARM:
			default:
				title = "BURNERS ARE OUT!";
				message = "Relight incense burners.";
				if (client.getGameCycle() % 40 >= 20)
				{
					backgroundColor = new Color(220, 0, 0, 200);
				}
				else
				{
					backgroundColor = new Color(70, 70, 70, 200);
				}
				break;
		}

		panelComponent.setBackgroundColor(backgroundColor);
		panelComponent.getChildren().add(LineComponent.builder()
			.left(title)
			.leftColor(Color.WHITE)
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left(message)
			.leftColor(Color.WHITE)
			.build());
		return panelComponent.render(graphics);
	}
}

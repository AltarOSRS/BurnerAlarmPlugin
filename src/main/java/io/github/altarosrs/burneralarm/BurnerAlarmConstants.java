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

import com.google.common.collect.ImmutableSet;
import java.awt.Color;
import java.util.Set;
import java.util.regex.Pattern;

final class BurnerAlarmConstants
{
	private BurnerAlarmConstants()
	{
	}

	// Plugin Name Prefix for chat messages
	static final String PLUGIN_PREFIX = "[House Hosting] ";

	// Game Object IDs for burner state
	static final Set<Integer> LIT_BURNER_IDS = ImmutableSet.of(13211, 13213, 57735);
	static final Set<Integer> UNLIT_BURNER_IDS = ImmutableSet.of(13210, 13212, 57734);

	// POH detection object IDs (Gilded Altar, Exit Portal, Tip Jar, Dungeon Entrance, Ornate Pool)
	static final Set<Integer> POH_DETECTION_OBJECT_IDS = ImmutableSet.of(
		13197, 13198, 13199,
		4525, 60789,
		29146,
		4529,
		29241
	);

	// Burner highlight colors (hardcoded traffic light convention)
	static final Color UNLIT_BURNER_COLOR = Color.RED;
	static final Color LIT_BURNER_COLOR = Color.GREEN;
	static final Color RANDOM_BURNOUT_COLOR = Color.YELLOW;

	// Item IDs
	static final int CLEAN_MARRENTILL_ID = 251;

	// Graphic/SpotAnim IDs for level-up animations
	static final int GENERIC_LEVEL_UP_GRAPHIC_ID = 199;
	static final int LEVEL_99_GRAPHIC_ID = 1388;

	// Animation IDs
	static final int HUMAN_LIGHT_TORCH_ANIMATION_ID = 3687;

	// Sound file names
	static final String FINAL_ALARM_SOUND_FILE = "alarm.wav";

	// Cooldown periods for notifications in game ticks
	static final int NOTIFICATION_COOLDOWN_TICKS = 25;

	// Regex patterns for chat messages
	static final Pattern TIP_JAR_PATTERN = Pattern.compile(
		"(.+) has left you a tip: Coins x ((?:\\d{1,3},)*\\d+)");
	static final Pattern OPPONENT_VALUE_PATTERN = Pattern.compile(
		"In return you will receive:\\(Value: ([\\d,]* coins|Lots!)\\)");

	// Advertisement Warning Prefix
	static final String AD_WARNING_PREFIX =
		"Your house advertisement will be removed from the board in";

	// Constants related to the game scene
	static final int SCENE_SIZE = 104;
	static final int MAX_PLANE = 4;

	// POH Detection Radius (in tiles)
	static final int POH_DETECTION_RADIUS = 20;

	// Second trade screen widget IDs
	static final int TRADE_WINDOW_OPPONENT_NAME_CHILD_ID = 30;
	static final int TRADE_WINDOW_OPPONENT_VALUE_TEXT_CHILD_ID = 24;
}

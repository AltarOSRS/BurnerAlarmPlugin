package net.runelite.client.plugins.burneralarm;

import com.google.common.collect.ImmutableSet;

import java.util.Set;
import java.util.regex.Pattern;

final class BurnerAlarmConstants {
    private BurnerAlarmConstants() {}

    // Plugin Name Prefix for chat messages
    static final String PLUGIN_PREFIX = "[House Hosting] ";

    // Game Object IDs for various POH elements
    static final Set<Integer> LIT_BURNER_IDS = ImmutableSet.of(13211, 13213);
    static final Set<Integer> UNLIT_BURNER_IDS = ImmutableSet.of(13210, 13212);
    static final Set<Integer> GILDED_ALTAR_IDS = ImmutableSet.of(13197, 13198, 13199);
    static final int EXIT_PORTAL_ID = 4525;

    // Item IDs
    static final int CLEAN_MARRENTILL_ID = 251;

    // Graphic/SpotAnim IDs for level-up animations
    static final int GENERIC_LEVEL_UP_GRAPHIC_ID = 199;
    static final int LEVEL_99_GRAPHIC_ID = 1388;

    // Sound file names
    static final String FINAL_ALARM_SOUND_FILE = "alarm.wav";

    // Cooldown periods for notifications in game ticks
    static final int NOTIFICATION_COOLDOWN_TICKS = 25;

    // Regex pattern for parsing tip jar messages
    static final Pattern TIP_JAR_PATTERN = Pattern.compile("(.+) has left you a tip: Coins x ((?:\\d{1,3},)*\\d+)");

    // Constants related to the game scene
    static final int SCENE_SIZE = 104;
    static final int MAX_PLANE = 4;
}
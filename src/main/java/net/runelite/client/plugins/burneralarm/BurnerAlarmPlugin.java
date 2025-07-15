package net.runelite.client.plugins.burneralarm;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.Notifier;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
        name = "Burner Alarm",
        description = "Notifies you when gilded altar burners are about to despawn.",
        tags = {"firemaking", "altar", "prayer", "gilded", "burner"}
)
public class BurnerAlarmPlugin extends Plugin {
    private static final Set<Integer> LIT_BURNER_IDS = ImmutableSet.of(13211, 13213);
    private static final int NOTIFICATION_COOLDOWN_TICKS = 25;

    @RequiredArgsConstructor
    private static class BurnerState {
        final int startTick;
        boolean preNotificationSent = false;
        boolean soundNotificationSent = false;
    }

    private final Map<GameObject, BurnerState> litBurners = new HashMap<>();
    private int lastTextAlertTick = 0;
    private int lastSoundAlertTick = 0;

    @Inject
    private Client client;

    @Inject
    private Notifier notifier;

    @Inject
    private BurnerAlarmConfig config;

    @Inject
    private AudioPlayer audioPlayer;

    @Provides
    BurnerAlarmConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BurnerAlarmConfig.class);
    }

    @Override
    protected void startUp() {
    }

    @Override
    protected void shutDown() {
        litBurners.clear();
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event) {
        final GameObject gameObject = event.getGameObject();
        if (LIT_BURNER_IDS.contains(gameObject.getId())) {
            litBurners.put(gameObject, new BurnerState(client.getTickCount()));
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event) {
        litBurners.remove(event.getGameObject());
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() != GameState.LOGGED_IN && event.getGameState() != GameState.LOADING) {
            if (!litBurners.isEmpty()) {
                log.debug("Cleared lit burners due to game state change: {}", event.getGameState());
                litBurners.clear();
            }
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (litBurners.isEmpty()) {
            return;
        }

        final int currentTick = client.getTickCount();
        final int fmLevel = client.getRealSkillLevel(Skill.FIREMAKING);
        final int certainDurationTicks = 200 + fmLevel;
        final int preNotificationTriggerTicks = certainDurationTicks - config.preNotificationLeadTimeTicks();

        for (BurnerState burnerState : litBurners.values()) {
            final int ticksSinceLit = currentTick - burnerState.startTick;

            if (!burnerState.soundNotificationSent && ticksSinceLit >= certainDurationTicks) {
                if (config.playAlertSound() && currentTick >= lastSoundAlertTick + NOTIFICATION_COOLDOWN_TICKS) {
                    playSound();
                    lastSoundAlertTick = currentTick;
                    burnerState.soundNotificationSent = true;
                }
            }

            if (!burnerState.preNotificationSent && ticksSinceLit >= preNotificationTriggerTicks) {
                if (config.sendNotification() && currentTick >= lastTextAlertTick + NOTIFICATION_COOLDOWN_TICKS) {
                    notifier.notify("A gilded altar burner will enter its random burnout phase soon!");
                    lastTextAlertTick = currentTick;
                    burnerState.preNotificationSent = true;
                }
            }
        }
    }

    private void playSound() {
        try {
            audioPlayer.play(getClass(), "alarm.wav", config.soundVolume());
        } catch (Exception e) {
            log.warn("Failed to play Burner Alarm sound", e);
        }
    }
}
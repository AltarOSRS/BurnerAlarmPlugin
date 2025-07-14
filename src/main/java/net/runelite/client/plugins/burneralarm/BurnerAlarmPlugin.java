package net.runelite.client.plugins.burneralarm;

import com.google.inject.Provides;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Skill;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.client.Notifier;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
        name = "Burner Alarm"
)
public class BurnerAlarmPlugin extends Plugin
{
    private static final int LIT_BURNER_1 = 13211;
    private static final int LIT_BURNER_2 = 13213;
    private static final double GAME_TICK_SECONDS = 0.6;
    private static final int PRE_NOTIFICATION_LEAD_TIME_SECONDS = 10;

    private final Map<GameObject, Instant> litBurners = new HashMap<>();

    private boolean preNotificationFiredThisCycle = false;
    private boolean soundFiredThisCycle = false;

    @Inject
    private Client client;

    @Inject
    private Notifier notifier;

    @Inject
    private BurnerAlarmConfig config;

    @Inject
    private AudioPlayer audioPlayer;

    @Provides
    BurnerAlarmConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(BurnerAlarmConfig.class);
    }

    @Override
    protected void startUp()
    {
        resetCycle();
    }

    @Override
    protected void shutDown()
    {
        resetCycle();
    }

    private void resetCycle()
    {
        litBurners.clear();
        preNotificationFiredThisCycle = false;
        soundFiredThisCycle = false;
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        GameObject gameObject = event.getGameObject();
        int gameObjectId = gameObject.getId();
        if (gameObjectId == LIT_BURNER_1 || gameObjectId == LIT_BURNER_2)
        {
            litBurners.put(gameObject, Instant.now());
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event)
    {
        litBurners.remove(event.getGameObject());
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (litBurners.isEmpty())
        {
            if (preNotificationFiredThisCycle || soundFiredThisCycle)
            {
                resetCycle();
            }
            return;
        }

        final int fmLevel = client.getRealSkillLevel(Skill.FIREMAKING);
        final double certainDurationSeconds = (200 + fmLevel) * GAME_TICK_SECONDS;
        final double preNotificationTriggerTime = certainDurationSeconds - PRE_NOTIFICATION_LEAD_TIME_SECONDS;

        boolean playSound = false;
        List<GameObject> burnersToRemove = new ArrayList<>();

        for (Map.Entry<GameObject, Instant> entry : litBurners.entrySet())
        {
            long secondsSinceLit = Duration.between(entry.getValue(), Instant.now()).getSeconds();

            if (!soundFiredThisCycle && secondsSinceLit >= certainDurationSeconds)
            {
                playSound = true;
                soundFiredThisCycle = true;
            }

            if (secondsSinceLit >= certainDurationSeconds)
            {
                burnersToRemove.add(entry.getKey());
            }

            if (!preNotificationFiredThisCycle && secondsSinceLit >= preNotificationTriggerTime)
            {
                if (config.sendNotification())
                {
                    notifier.notify("A gilded altar burner will enter its random burnout phase soon!");
                }
                preNotificationFiredThisCycle = true;
            }
        }

        if (playSound && config.playAlertSound())
        {
            new Thread(() -> {
                try
                {
                    audioPlayer.play(getClass(), "alarm.wav", config.soundVolume());
                }
                catch (Exception e)
                {
                    log.warn("Failed to play Burner Alarm sound", e);
                }
            }).start();
        }

        burnersToRemove.forEach(litBurners::remove);
    }
}
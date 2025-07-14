package net.runelite.client.plugins.burneralarm;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("burneralarm")
public interface BurnerAlarmConfig extends Config
{
    @ConfigItem(
            keyName = "sendNotification",
            name = "Send Notification",
            description = "Toggle the standard text-based notification.",
            position = 1
    )
    default boolean sendNotification()
    {
        return true;
    }

    @ConfigItem(
            keyName = "playAnnoyingSound",
            name = "Play Custom Sound",
            description = "Toggle the custom alarm sound.",
            position = 2
    )
    default boolean playAnnoyingSound()
    {
        return true;
    }

    @Range(
            min = 1,
            max = 100
    )
    @ConfigItem(
            keyName = "annoyingSoundVolume",
            name = "Custom Sound Volume",
            description = "Adjust the volume of the custom alarm sound. (Requires custom sound to be enabled)",
            position = 3
    )
    default int annoyingSoundVolume()
    {
        return 25; // Default to a reasonable 25% volume
    }
}

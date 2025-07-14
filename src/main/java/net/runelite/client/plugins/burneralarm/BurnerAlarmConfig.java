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
            description = "Toggle the notification pre-warning.",
            position = 1
    )
    default boolean sendNotification()
    {
        return true;
    }

    @ConfigItem(
            keyName = "playAlertSound",
            name = "Play Alert Sound",
            description = "Toggle the main audible alarm. Volume is controlled by RuneLite's 'Sound Effects' slider.",
            position = 2
    )
    default boolean playAlertSound()
    {
        return true;
    }

    @Range(
            min = -40,
            max = 6
    )
    @ConfigItem(
            keyName = "soundVolume",
            name = "Sound Volume (dB)",
            description = "Adjust the volume of the alarm sound in decibels (dB).",
            position = 3
    )
    default int soundVolume()
    {
        return -20;
    }
}

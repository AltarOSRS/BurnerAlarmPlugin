# Burner Alarm

A configurable plugin that provides a two-stage alert when POH incense burners are about to expire.

## Features

* Timer scales with your Firemaking level.
* Two-stage alert: a notification pre-warning, followed by a sound alarm.
* Custom sound with adjustable volume.
* Sends only one alert per set of burners.
* Re-lighting burners cancels pending alerts.

## How It Works

1.  A notification pre-warning is sent 10 seconds **before** burners enter their random burnout phase.
2.  If not re-lit, the main sound alarm plays 10 seconds **later**, as the random phase begins.

## Configuration

* **Send Notification:** Toggles the notification pre-warning.
* **Play Custom Sound:** Toggles the main audible alarm.
* **Custom Sound Volume:** Adjusts the volume of the main alarm.
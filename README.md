# House Hosting Plugin

A RuneLite plugin designed to assist players hosting their Player-Owned Houses (POHs).

## Features

*Note: All notifications are fully customizable in the plugin configuration menu. The defaults are good to get started with.*

This plugin provides several key functionalities to help you manage your POH hosting:

### Burner Alarm

Receive notifications before your incense burners extinguish.

* **Two-Stage Alarm:** A pre-warning notification with countdown timer, followed by a final alarm sound.

* **Configurable Lead Time:** Adjust the pre-warning timing.

* **Volume Control:** Customize the final alarm sound volume.

* **Grace Period:** Configurable grace period after burners expire before the alarm triggers, giving you time to relight.

* **Hiscore Lookups:** Optionally looks up guests' Firemaking levels to calculate accurate burn durations.

### Burner Highlights

Easily identify burner states at a glance.

* **Unlit Burner Highlight:** Draws a flashing outline around unlit incense burners.

* **Lit Burner Highlight:** Highlights lit burners with a solid outline that flashes during pre-warning and random burnout states.

* **Multiple Styles:** Choose from hull, outline, clickbox, or tile highlight styles.

* **Configurable Color & Width:** Choose highlight colors and outline thickness.

### Tip Jar Notifications

Get notified when players add coins to your tip jar.

* **Tiered Notifications:** Different notifications and chat recoloring based on tip amount.

* **Configurable Thresholds:** Set custom thresholds for tip tiers.

* **Chat Recolor:** Automatically recolors in-game tip jar messages.

### Tip Tracker Panel

Track and manage tips received during hosting sessions.

* **Sidebar Panel:** Dedicated panel showing tip history and leaderboard.

* **Persistent Leaderboard:** All-time leaderboard that persists across sessions.

* **Time Filtering:** View tips by session, weekly, monthly, or all-time.

* **Manual Entry:** Add, edit, or delete tip entries manually.

* **Trade Tip Detection:** Automatically captures tips received via the trade window.

### Player Level-Up Notifications

Get notified when guests achieve level-ups in your house.

* **Generic Level-Up:** Notification for any level-up. This option is disabled by default (it can get pretty spammy). If you decide to use, enable the "Collapse game chat" option in the Chat Filter plugin to condense.

* **Level 99 Achievement:** Special notifications for level 99.

* **Combat Level 126:** Distinct notification for combat level 126.

* **Chat Messages:** Configurable chat messages for level-up events (off, public-style, or filtered).

### Player Highlighting

Highlight players in your house based on their tip contributions.

* **Tiered Highlights:** Players are highlighted with colors based on their all-time tip total.

* **Configurable Tiers:** Set thresholds and colors for each highlight tier.

* **Level-Up Highlights:** Temporarily highlight players who achieve level-ups.

### Marrentill Tracker

Monitor your Marrentill supply for altars.

* **Inventory Monitoring:** Tracks unnoted Clean Marrentills.

* **Low Stock Warning:** Notifies you when Marrentill stock is low or depleted.

### POH Detection

Automatically detects whether you are in your own house or visiting a guest's house.

* **House Fingerprinting:** Saves a fingerprint of your house layout to distinguish it from other houses.

* **POH Status Overlay:** Shows whether you are in your own POH or a guest's POH.

* **Feature Gating:** Certain features (burner alarm, ad warnings) only activate in your own house.

### POH Guest Tracker

View the number of guests in your house.

* **Overlay Display:** An overlay shows the current guest count in your POH.

### Advertisement Warning

Get notified when your house advertisement is about to be removed.

* **Countdown Timer:** An infobox timer shows the remaining time before your ad is removed.

* **Notifications:** Alerts at 3, 2, and 1 minute remaining.

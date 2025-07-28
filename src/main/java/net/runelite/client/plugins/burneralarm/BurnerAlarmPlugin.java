package net.runelite.client.plugins.burneralarm;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.Notifier;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Notification;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;

import javax.inject.Inject;
import java.awt.*;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

@Slf4j
@PluginDescriptor(
        name = "House Hosting",
        description = "Tools to assist players hosting their player owned house to guests.",
        tags = {"poh", "player owned house", "house", "host", "hosting", "altar", "gilded", "gilded altar", "burner", "burners", "tip", "jar", "tip jar", "notification", "alarm"}
)
public class BurnerAlarmPlugin extends Plugin {

    private static class BurnerState {
        final int startTick;
        boolean preNotificationSent = false;
        boolean finalAlarmSent = false;

        BurnerState(int startTick) {
            this.startTick = startTick;
        }
    }

    @Inject
    private Client client;

    @Inject
    private BurnerAlarmConfig config;

    @Inject
    private Notifier notifier;

    @Inject
    private AudioPlayer audioPlayer;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private BurnerAlarmOverlay burnerAlarmOverlay;

    @Getter
    private int guestCount = -1;

    private final Map<GameObject, BurnerState> litBurners = new HashMap<>();
    @Getter
    private final Map<GameObject, Boolean> unlitBurners = new HashMap<>();

    private int lastPreWarningTick = 0;
    private int lastFinalAlarmTick = 0;
    private int unnotedMarrentillCount = -1;
    private boolean notifiedLowStock = false;

    private final Map<String, Integer> playerCombatLevels = new HashMap<>();
    // This flag tracks if the next Level 99 graphic for a player should be suppressed,
    // specifically when a Combat Level 126 achievement has just occurred.
    private final Map<String, Boolean> playerSuppressNext99GraphicFor126 = new HashMap<>();


    @Provides
    BurnerAlarmConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BurnerAlarmConfig.class);
    }

    @Override
    protected void startUp() {
        overlayManager.add(burnerAlarmOverlay);
        log.debug("POH Assistant plugin started.");
    }

    @Override
    protected void shutDown() {
        litBurners.clear();
        unlitBurners.clear();
        unnotedMarrentillCount = -1;
        notifiedLowStock = false;
        guestCount = -1;
        playerCombatLevels.clear();
        playerSuppressNext99GraphicFor126.clear();
        overlayManager.remove(burnerAlarmOverlay);
        log.debug("POH Assistant plugin shut down.");
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event) {
        final GameObject gameObject = event.getGameObject();
        if (BurnerAlarmConstants.LIT_BURNER_IDS.contains(gameObject.getId())) {
            litBurners.put(gameObject, new BurnerState(client.getTickCount()));
            unlitBurners.remove(gameObject);
        } else if (BurnerAlarmConstants.UNLIT_BURNER_IDS.contains(gameObject.getId())) {
            unlitBurners.put(gameObject, true);
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event) {
        litBurners.remove(event.getGameObject());
        unlitBurners.remove(event.getGameObject());
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() != GameState.LOGGED_IN) {
            litBurners.clear();
            unlitBurners.clear();
            unnotedMarrentillCount = -1;
            notifiedLowStock = false;
            guestCount = -1;
            playerCombatLevels.clear();
            playerSuppressNext99GraphicFor126.clear();
            log.debug("POH Assistant: Game state changed to non-logged in. All trackers reset.");
        }
        if (event.getGameState() == GameState.LOGGED_IN) {
            updateUnnotedMarrentillCount();
            if (isInPOH()) {
                playerCombatLevels.clear();
                playerSuppressNext99GraphicFor126.clear();
                for (Player player : client.getTopLevelWorldView().players()) {
                    if (player != null && !player.equals(client.getLocalPlayer())) {
                        playerCombatLevels.put(player.getName(), player.getCombatLevel());
                        log.debug("POH Assistant: On login, tracked player: {} - Combat Level: {}", player.getName(), player.getCombatLevel());
                    }
                }
            }
            log.debug("POH Assistant: Game state changed to LOGGED_IN. Marrentill and initial player levels updated.");
        }
    }

    @Subscribe
    public void onPlayerSpawned(PlayerSpawned event) {
        final Player player = event.getPlayer();
        if (player != null && !player.equals(client.getLocalPlayer()) && isInPOH()) {
            playerCombatLevels.put(player.getName(), player.getCombatLevel());
            log.debug("POH Assistant: Player spawned: {} - Combat Level: {}", player.getName(), player.getCombatLevel());
        }
    }

    @Subscribe
    public void onPlayerDespawned(PlayerDespawned event) {
        if (!isInPOH()) {
            return;
        }

        final Player player = event.getPlayer();
        if (player != null) {
            Integer removedLevel = playerCombatLevels.remove(player.getName());
            playerSuppressNext99GraphicFor126.remove(player.getName());
            log.debug("POH Assistant: Player despawned: {} - Last known Combat Level: {}", player.getName(), removedLevel);
        }
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        // --- Burner Alarm Logic ---
        if (client.getGameState() == GameState.LOGGED_IN && !litBurners.isEmpty()) {
            final int currentTick = client.getTickCount();
            final int firemakingLevel = client.getRealSkillLevel(Skill.FIREMAKING);
            final int certainDurationTicks = 200 + firemakingLevel;
            final int preNotificationTriggerTicks = certainDurationTicks - config.burnerLeadTime();

            boolean triggerPreWarningThisTick = false;
            boolean triggerFinalAlarmThisTick = false;

            for (BurnerState burnerState : litBurners.values()) {
                final int ticksSinceLit = currentTick - burnerState.startTick;

                if (!burnerState.preNotificationSent && ticksSinceLit >= preNotificationTriggerTicks) {
                    burnerState.preNotificationSent = true;
                    triggerPreWarningThisTick = true;
                }

                if (!burnerState.finalAlarmSent && ticksSinceLit >= certainDurationTicks) {
                    burnerState.finalAlarmSent = true;
                    triggerFinalAlarmThisTick = true;
                }
            }

            if (triggerPreWarningThisTick) {
                if (currentTick >= lastPreWarningTick + BurnerAlarmConstants.NOTIFICATION_COOLDOWN_TICKS) {
                    Notification preWarningNotification = config.burnerPreWarningNotification();
                    if (preWarningNotification.isEnabled()) {
                        notifier.notify(preWarningNotification, "A POH burner will enter its random burnout phase soon!");
                    }

                    if (config.burnerPreWarningGameMessage()) {
                        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                                ColorUtil.wrapWithColorTag("A POH burner will enter its random burnout phase soon!", config.burnerPreWarningColor()), null);
                    }
                    lastPreWarningTick = currentTick;
                    log.debug("POH Assistant: Burner pre-warning triggered.");
                }
            }

            if (triggerFinalAlarmThisTick && config.playFinalAlarm()) {
                if (currentTick >= lastFinalAlarmTick + BurnerAlarmConstants.NOTIFICATION_COOLDOWN_TICKS) {
                    playFinalAlarmSound();
                    lastFinalAlarmTick = currentTick;
                    log.debug("POH Assistant: Final burner alarm triggered.");
                }
            }
        }

        // --- Guest Counter Logic ---
        if (config.pohGuestTrackerEnabled() && client.getGameState() == GameState.LOGGED_IN) {
            updateGuestCount();
        } else {
            guestCount = -1;
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (event.getItemContainer().getId() == InventoryID.INV) {
            updateUnnotedMarrentillCount();
        }
    }


    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() != ChatMessageType.PLAYERRELATED) {
            return;
        }

        Matcher matcher = BurnerAlarmConstants.TIP_JAR_PATTERN.matcher(event.getMessage());
        if (!matcher.matches()) {
            return;
        }

        String playerName = matcher.group(1);
        String amountString = matcher.group(2).replace(",", "");

        try {
            long amount = Long.parseLong(amountString);
            String formattedAmount = NumberFormat.getInstance(Locale.US).format(amount);
            String notificationMessage = playerName + " tipped " + formattedAmount + " coins!";

            Notification notification = null;
            Color color = null;

            if (amount >= config.tipJarTier1Threshold()) {
                notification = config.tipJarTier1Notification();
                color = config.tipJarTier1Color();
            } else if (amount >= config.tipJarTier2Threshold()) {
                notification = config.tipJarTier2Notification();
                color = config.tipJarTier2Color();
            } else if (amount >= config.tipJarTier3Threshold()) {
                notification = config.tipJarTier3Notification();
                color = config.tipJarTier3Color();
            }

            if (notification != null && notification.isEnabled()) {
                notifier.notify(notification, notificationMessage);
                log.info("POH Assistant: Tip jar notification for {} tipped {} coins.", playerName, formattedAmount);

                if (config.tipJarRecolorChatMessage() && color != null) {
                    final MessageNode messageNode = event.getMessageNode();
                    messageNode.setValue(ColorUtil.wrapWithColorTag(messageNode.getValue(), color));
                    client.refreshChat();
                }
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to parse tip jar amount: {}", amountString, e);
        }
    }

    @Subscribe
    public void onGraphicChanged(GraphicChanged event) {
        final Actor actor = event.getActor();
        if (!(actor instanceof Player)) {
            return;
        }

        final Player player = (Player) actor;
        if (player.equals(client.getLocalPlayer())) {
            return;
        }

        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }

        if (!isInPOH()) {
            return;
        }

        final boolean isGenericLevelUpGraphic = player.hasSpotAnim(BurnerAlarmConstants.GENERIC_LEVEL_UP_GRAPHIC_ID);
        final boolean isLevel99Graphic = player.hasSpotAnim(BurnerAlarmConstants.LEVEL_99_GRAPHIC_ID);

        log.debug("POH Assistant: GraphicChanged for {}. Has 99 graphic: {}. Has Generic graphic: {}. Current Game State: {}.",
                player.getName(), isLevel99Graphic, isGenericLevelUpGraphic, client.getGameState());

        if (!isGenericLevelUpGraphic && !isLevel99Graphic) {
            log.debug("POH Assistant: GraphicChanged for {} is not a recognized level-up graphic. Skipping further processing.", player.getName());
            return;
        }

        Integer previousCombatLevel = playerCombatLevels.get(player.getName());
        int currentCombatLevel = player.getCombatLevel();

        log.debug("POH Assistant: Level-up Graphic for {}. Previous Combat Level (from map): {}. Current Combat Level (now): {}.",
                player.getName(), previousCombatLevel, currentCombatLevel);

        // Combat Level 126 Detection (Highest Priority when combat changes from 125 to 126)
        if (previousCombatLevel != null && previousCombatLevel == 125 && currentCombatLevel == 126) {
            String notificationMessage = player.getName() + " has achieved Combat Level 126!";
            Notification notificationConfig = config.level126CombatNotification();
            Color messageColor = config.level126CombatColor();
            boolean sendGameMessage = config.level126CombatGameMessage();
            log.info("POH Assistant: Detected Combat Level 126 for {} (graphic: {}).", player.getName(), (isLevel99Graphic ? "99 graphic" : "generic graphic"));

            if (notificationConfig.isEnabled()) {
                notifier.notify(notificationConfig, notificationMessage);
                if (sendGameMessage && messageColor != null) {
                    client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", ColorUtil.wrapWithColorTag(notificationMessage, messageColor), null);
                }
            }
            // If Combat Level 126 was just achieved, the next 99/126 graphic is likely for this achievement, so suppress it.
            playerSuppressNext99GraphicFor126.put(player.getName(), true);
        }

        // Handle Generic Level Up Graphic
        if (isGenericLevelUpGraphic) {
            String notificationMessage = player.getName() + " has leveled up!";
            Notification notificationConfig = config.levelUpNotification();
            Color messageColor = config.levelUpColor();
            boolean sendGameMessage = config.levelUpGameMessage();
            log.info("POH Assistant: Detected Generic Level-Up for {}", player.getName());

            if (notificationConfig.isEnabled()) {
                notifier.notify(notificationConfig, notificationMessage);
                if (sendGameMessage && messageColor != null) {
                    client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", ColorUtil.wrapWithColorTag(notificationMessage, messageColor), null);
                }
            }
        }
        // Level 99 Skill Detection (from LEVEL_99_GRAPHIC_ID)
        else if (isLevel99Graphic) {
            // Check if this 99 graphic should be suppressed because it's the expected 126 combat animation.
            boolean shouldSuppressThis99Graphic = playerSuppressNext99GraphicFor126.getOrDefault(player.getName(), false);

            if (shouldSuppressThis99Graphic) {
                log.debug("POH Assistant: Level 99 graphic for {} suppressed (it's the expected 126 combat animation).", player.getName());
                // Reset the flag immediately after suppressing, so any subsequent 99/126 graphics can be real 99s.
                playerSuppressNext99GraphicFor126.put(player.getName(), false);
            } else {
                // If not suppressed, it's a genuine Level 99 skill.
                String notificationMessage = player.getName() + " has achieved level 99!";
                Notification notificationConfig = config.level99Notification();
                Color messageColor = config.level99Color();
                boolean sendGameMessage = config.level99GameMessage();
                log.info("POH Assistant: Detected Level 99 for {}", player.getName());

                if (notificationConfig.isEnabled()) {
                    notifier.notify(notificationConfig, notificationMessage);
                    if (sendGameMessage && messageColor != null) {
                        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", ColorUtil.wrapWithColorTag(notificationMessage, messageColor), null);
                    }
                }
            }
        }

        // Update the combat level in the map if it has changed.
        if (previousCombatLevel == null || previousCombatLevel != currentCombatLevel) {
            playerCombatLevels.put(player.getName(), currentCombatLevel);
            log.debug("POH Assistant: Final update for {}'s combat level in map to {}.", player.getName(), currentCombatLevel);
        }
    }

    /**
     * Checks if a Gilded Altar or Exit Portal is present in the current scene.
     * This method is used for POH detection.
     *
     * @return true if a Gilded Altar or Exit Portal is found, false otherwise.
     */
    private boolean isGildedAltarOrExitPortalPresent() {
        for (int x = 0; x < BurnerAlarmConstants.SCENE_SIZE; x++) {
            for (int y = 0; y < BurnerAlarmConstants.SCENE_SIZE; y++) {
                for (int plane = 0; plane < BurnerAlarmConstants.MAX_PLANE; plane++) {
                    Tile tile = client.getTopLevelWorldView().getScene().getTiles()[plane][x][y];
                    if (tile == null) {
                        continue;
                    }

                    for (GameObject gameObject : tile.getGameObjects()) {
                        if (gameObject != null && (BurnerAlarmConstants.GILDED_ALTAR_IDS.contains(gameObject.getId()) || gameObject.getId() == BurnerAlarmConstants.EXIT_PORTAL_ID)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Updates the count of players in the POH, excluding the local player.
     * This method uses `isInPOH()` for POH detection.
     */
    private void updateGuestCount() {
        if (isInPOH()) {
            int currentGuestCount = 0;
            Player localPlayer = client.getLocalPlayer();
            if (localPlayer != null) {
                for (Player player : client.getTopLevelWorldView().players()) {
                    if (player != null && !player.equals(localPlayer)) {
                        currentGuestCount++;
                    }
                }
            }
            this.guestCount = currentGuestCount;
        } else {
            this.guestCount = -1;
        }
    }

    /**
     * Checks if the player is currently inside their Player-Owned House (by checking for Gilded Altar or Exit Portal).
     * Used by the HouseHostingOverlay to determine visibility and by other plugin logic.
     * @return true if in a POH with a Gilded Altar or Exit Portal, false otherwise.
     */
    public boolean isInPOH() {
        return isGildedAltarOrExitPortalPresent();
    }

    private void playFinalAlarmSound() {
        try {
            audioPlayer.play(getClass(), BurnerAlarmConstants.FINAL_ALARM_SOUND_FILE, config.finalAlarmVolume());
        } catch (Exception e) {
            log.warn("Failed to play POH Assistant final alarm sound", e);
        }
    }

    /**
     * Checks the player's inventory for unnoted Clean Marrentills (ID 251)
     * and updates the count. This method also triggers the notification logic.
     */
    private void updateUnnotedMarrentillCount() {
        if (client.getGameState() != GameState.LOGGED_IN || !isInPOH()) {
            if (unnotedMarrentillCount != -1)
            {
                unnotedMarrentillCount = -1;
                log.debug("POH Assistant: Not in POH or not logged in. Marrentill count tracking paused (-1).");
            }
            return;
        }

        ItemContainer inventory = client.getItemContainer(InventoryID.INV);
        int count = 0;
        if (inventory != null) {
            for (Item item : inventory.getItems()) {
                if (item != null && item.getId() == BurnerAlarmConstants.CLEAN_MARRENTILL_ID && client.getItemDefinition(item.getId()).getNote() == -1) {
                    count += item.getQuantity();
                }
            }
        }

        int previousCount = this.unnotedMarrentillCount;
        this.unnotedMarrentillCount = count;

        log.debug("POH Assistant: Marrentill count updated. Previous: {}, Current: {}. NotifiedLowStock: {}",
                previousCount, count, notifiedLowStock);

        if (config.marrentillNotification().isEnabled()) {
            if (count <= 1 && !notifiedLowStock && previousCount > 1) {
                String message;
                if (count == 0) {
                    message = "You're out of unnoted Marrentills!";
                } else {
                    message = "You're almost out of unnoted Marrentills!";
                }

                notifier.notify(config.marrentillNotification(), message);
                if (config.marrentillGameMessage()) {
                    client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", ColorUtil.wrapWithColorTag(message, config.marrentillGameMessageColor()), null);
                }
                notifiedLowStock = true;
                log.info("POH Assistant: Notified - Low Marrentill stock (Current: {}).", count);
            }
            else if (count > 1) {
                notifiedLowStock = false;
                log.debug("POH Assistant: Marrentill stock sufficient (Current: {}). Resetting notification flag.", count);
            }
        }
        else {
            notifiedLowStock = false;
            log.debug("POH Assistant: Marrentill notification disabled. Resetting flags.");
        }
    }
}
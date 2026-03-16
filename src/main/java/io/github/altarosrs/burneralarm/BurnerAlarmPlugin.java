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

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.regex.Matcher;
import javax.inject.Inject;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MessageNode;
import net.runelite.api.Player;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.PlayerDespawned;
import net.runelite.api.events.PlayerSpawned;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Notification;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "House Hosting",
	description = "Tools to assist players hosting their player owned house to guests.",
	tags = {"poh", "player owned house", "house", "host", "hosting", "altar", "gilded",
		"gilded altar", "burner", "burners", "tip", "jar", "tip jar", "notification", "alarm"}
)
public class BurnerAlarmPlugin extends Plugin
{
	@Inject private Client client;
	@Inject private BurnerAlarmConfig config;
	@Inject private ConfigManager configManager;
	@Inject private Gson gson;
	@Inject private Notifier notifier;
	@Inject private OverlayManager overlayManager;
	@Inject private InfoBoxManager infoBoxManager;
	@Inject private ItemManager itemManager;
	@Inject private ClientToolbar clientToolbar;

	// Services
	@Inject private BurnerService burnerService;
	@Inject private HouseDetectionService houseDetectionService;
	@Inject private LevelUpService levelUpService;
	@Inject private PlayerHighlightService playerHighlightService;

	// Overlays
	@Inject private BurnerAlarmOverlay burnerAlarmOverlay;
	@Inject private BurnerAlarmWarningOverlay burnerAlarmWarningOverlay;
	@Inject private POHGuestOverlay pohGuestOverlay;
	@Inject private POHStatusOverlay pohStatusOverlay;
	@Inject private MarrentillTrackerOverlay marrentillTrackerOverlay;
	@Inject private PlayerHighlightOverlay playerHighlightOverlay;

	// Tip tracker (manually managed lifecycle)
	private TipTrackerManager tipTrackerManager;
	private TipTrackerPanel tipPanel;
	private NavigationButton tipNavButton;

	// Ad warning
	private AdvertisementWarningInfobox adWarningInfobox = null;

	// Guest tracking
	@Getter
	private int guestCount = -1;

	// Marrentill tracking
	@Getter
	private int unnotedMarrentillCount = -1;
	private boolean notifiedLowStock = false;
	private boolean showMarrentillOverlay = false;

	// Trade capture
	private TradeInfo pendingTrade = null;
	private static final NumberFormat VALUE_FORMAT = NumberFormat.getNumberInstance(Locale.UK);

	@Provides
	BurnerAlarmConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BurnerAlarmConfig.class);
	}

	@Override
	protected void startUp()
	{
		burnerService.setPlugin(this);
		houseDetectionService.loadFingerprint();

		overlayManager.add(burnerAlarmOverlay);
		overlayManager.add(burnerAlarmWarningOverlay);
		overlayManager.add(pohGuestOverlay);
		overlayManager.add(pohStatusOverlay);
		overlayManager.add(marrentillTrackerOverlay);
		overlayManager.add(playerHighlightOverlay);

		updateTipTrackerPanelVisibility();
		updateExpelGuestsButtonVisibility();
	}

	@Override
	protected void shutDown()
	{
		burnerService.shutDown();
		burnerService.reset();
		houseDetectionService.reset();
		levelUpService.reset();

		unnotedMarrentillCount = -1;
		notifiedLowStock = false;
		guestCount = -1;
		pendingTrade = null;
		showMarrentillOverlay = false;

		if (adWarningInfobox != null)
		{
			infoBoxManager.removeInfoBox(adWarningInfobox);
			adWarningInfobox = null;
		}

		overlayManager.remove(burnerAlarmOverlay);
		overlayManager.remove(burnerAlarmWarningOverlay);
		overlayManager.remove(pohGuestOverlay);
		overlayManager.remove(pohStatusOverlay);
		overlayManager.remove(marrentillTrackerOverlay);
		overlayManager.remove(playerHighlightOverlay);

		if (tipNavButton != null)
		{
			clientToolbar.removeNavigation(tipNavButton);
			tipNavButton = null;
		}
		if (tipTrackerManager != null)
		{
			tipTrackerManager.shutdown();
			tipTrackerManager = null;
		}

		Widget expelGuestsButton = client.getWidget(InterfaceID.POH_OPTIONS, 20);
		if (expelGuestsButton != null)
		{
			expelGuestsButton.setHidden(false);
		}
	}

	public boolean shouldShowMarrentillOverlay()
	{
		return showMarrentillOverlay;
	}

	public boolean isInPOH()
	{
		return houseDetectionService.isInPOH();
	}

	public boolean isMyPOH()
	{
		return houseDetectionService.isMyPOH();
	}

	// --- Event Dispatchers ---

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (tipTrackerManager != null)
		{
			tipTrackerManager.onGameStateChanged(event);
		}

		if (event.getGameState() != GameState.LOGGED_IN)
		{
			burnerService.reset();
			houseDetectionService.reset();
			levelUpService.reset();
			unnotedMarrentillCount = -1;
			notifiedLowStock = false;
			guestCount = -1;
			pendingTrade = null;
			showMarrentillOverlay = false;
		}

		if (event.getGameState() == GameState.LOGGED_IN)
		{
			houseDetectionService.loadFingerprint();
			updateUnnotedMarrentillCount();
			levelUpService.initCombatLevels();
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			burnerService.onGameTick();
			houseDetectionService.onGameTick();

			// Remove ad warning timer if player is in their own POH
			if (adWarningInfobox != null && houseDetectionService.isMyPOH())
			{
				infoBoxManager.removeInfoBox(adWarningInfobox);
				adWarningInfobox = null;
			}

			if (tipTrackerManager != null)
			{
				tipTrackerManager.onGameTick();
			}

			playerHighlightService.removeExpiredHighlights();

			if (config.pohGuestTrackerEnabled() && houseDetectionService.isInPOH())
			{
				updateGuestCount();
			}
			else
			{
				guestCount = -1;
			}
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		burnerService.onGameObjectSpawned(event);
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		burnerService.onGameObjectDespawned(event);
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		burnerService.onAnimationChanged(event);
	}

	@Subscribe
	public void onPlayerSpawned(PlayerSpawned event)
	{
		levelUpService.onPlayerSpawned(event);
	}

	@Subscribe
	public void onPlayerDespawned(PlayerDespawned event)
	{
		levelUpService.onPlayerDespawned(event);
	}

	@Subscribe
	public void onGraphicChanged(GraphicChanged event)
	{
		levelUpService.onGraphicChanged(event);
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getItemContainer().getId() == InventoryID.INV)
		{
			updateUnnotedMarrentillCount();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.PLAYERRELATED)
		{
			// Trade acceptance
			if (event.getType() == ChatMessageType.TRADE
				&& "Accepted trade.".equals(event.getMessage()))
			{
				if (houseDetectionService.shouldRunFeature(config.allowFeaturesInGuestPOH()))
				{
					if (pendingTrade != null)
					{
						Tip tradeTip = new Tip(pendingTrade.getAmount(), pendingTrade.getPlayerName(),
							Instant.now(), TipSource.TRADE);
						if (tipTrackerManager != null)
						{
							tipTrackerManager.addTip(tradeTip);
						}
						if (config.highlightTradeTips())
						{
							playerHighlightService.addTemporaryHighlight(
								pendingTrade.getPlayerName(), config.tradeTipColor());
						}
					}
				}
				pendingTrade = null;
			}

			// Ad warning
			if (config.enableAdWarning() && event.getType() == ChatMessageType.GAMEMESSAGE)
			{
				handleAdWarning(Text.removeTags(event.getMessage()));
			}

			return;
		}

		// Tip jar message
		handleTipJarMessage(event);
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.POH_OPTIONS)
		{
			updateExpelGuestsButtonVisibility();
		}

		if (event.getGroupId() == InterfaceID.TRADECONFIRM)
		{
			if (houseDetectionService.shouldRunFeature(config.allowFeaturesInGuestPOH()))
			{
				String opponentName = getTradePartnersName();
				long opponentValue = getOpponentTradeValue();

				if (opponentName != null && opponentValue > 0)
				{
					pendingTrade = new TradeInfo(opponentName, opponentValue);
				}
			}
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.getGroupId() == InterfaceID.TRADECONFIRM)
		{
			pendingTrade = null;
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("burneralarm"))
		{
			return;
		}

		String key = event.getKey();
		switch (key)
		{
			case "marrentillOverlay":
				if ("false".equals(event.getNewValue()))
				{
					showMarrentillOverlay = false;
				}
				else
				{
					updateUnnotedMarrentillCount();
				}
				return;
			case "burnerAlarmOverlay":
				burnerService.updateBurnerOverlayState();
				return;
			case "enableAdWarning":
				if ("false".equals(event.getNewValue()) && adWarningInfobox != null)
				{
					infoBoxManager.removeInfoBox(adWarningInfobox);
					adWarningInfobox = null;
				}
				return;
			case "resetCurrentCharacterTips":
				if ("true".equals(event.getNewValue()))
				{
					configManager.setConfiguration("burneralarm",
						"resetCurrentCharacterTips", false);
					SwingUtilities.invokeLater(this::handleReset);
				}
				break;
			case "startHouseScan":
				if ("true".equals(event.getNewValue()))
				{
					log.debug("Scan house toggle clicked.");
					configManager.setConfiguration("burneralarm", key, false);
					houseDetectionService.scanHouse();
				}
				break;
			case "advancedPohDetectionEnabled":
				if ("true".equals(event.getNewValue()))
				{
					houseDetectionService.onAdvancedDetectionEnabled();
					SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
						"For this feature to work, you need to scan your house. Enter your POH"
							+ " and stand on the exact tile that you enter on and click the"
							+ " 'Scan My House (Toggle)' option in the config. Modifying your"
							+ " house will fail detection and require a re-scan.",
						"Advanced POH Detection Enabled",
						JOptionPane.INFORMATION_MESSAGE));
				}
				else
				{
					houseDetectionService.onAdvancedDetectionDisabled();
				}
				break;
		}

		if (key.equals("hideExpelGuests"))
		{
			updateExpelGuestsButtonVisibility();
		}

		if (key.equals("enableTipTracker"))
		{
			updateTipTrackerPanelVisibility();
		}


		if (key.equals("highlightTopTippers") && playerHighlightService != null)
		{
			playerHighlightService.updatePermanentHighlights();
		}
	}

	// --- Tip Jar ---

	private void handleTipJarMessage(ChatMessage event)
	{
		Matcher matcher = BurnerAlarmConstants.TIP_JAR_PATTERN.matcher(event.getMessage());
		if (!matcher.matches())
		{
			return;
		}

		if (houseDetectionService.isMyPOH())
		{
			String playerName = Text.removeTags(matcher.group(1));
			String amountString = matcher.group(2).replace(",", "");
			try
			{
				long amount = Long.parseLong(amountString);
				if (tipTrackerManager != null)
				{
					Tip newTip = new Tip(amount, playerName, Instant.now(), TipSource.TIP_JAR);
					tipTrackerManager.addTip(newTip);
				}

				String formattedAmount = NumberFormat.getInstance(Locale.US).format(amount);
				String notificationMessage = playerName + " tipped " + formattedAmount + " coins!";
				Notification notification = null;
				Color color = null;
				boolean highlight = false;
				BurnerAlarmConfig.TipHighlightMode highlightMode = config.tipHighlightMode();
				if (amount >= config.tipJarTier1Threshold())
				{
					notification = config.tipJarTier1Notification();
					color = config.tipJarTier1Color();
					highlight = highlightMode != BurnerAlarmConfig.TipHighlightMode.OFF;
				}
				else if (amount >= config.tipJarTier2Threshold())
				{
					notification = config.tipJarTier2Notification();
					color = config.tipJarTier2Color();
					highlight = highlightMode == BurnerAlarmConfig.TipHighlightMode.TIER_2_AND_ABOVE
						|| highlightMode == BurnerAlarmConfig.TipHighlightMode.ALL_TIERS;
				}
				else if (amount >= config.tipJarTier3Threshold())
				{
					notification = config.tipJarTier3Notification();
					color = config.tipJarTier3Color();
					highlight = highlightMode == BurnerAlarmConfig.TipHighlightMode.ALL_TIERS;
				}

				if (notification != null && notification.isEnabled())
				{
					notifier.notify(notification,
						BurnerAlarmConstants.PLUGIN_PREFIX + notificationMessage);
				}
				if (color != null)
				{
					final MessageNode messageNode = event.getMessageNode();
					messageNode.setValue(
						ColorUtil.wrapWithColorTag(messageNode.getValue(), color));
					client.refreshChat();
				}

				if (highlight && color != null)
				{
					playerHighlightService.addTemporaryHighlight(playerName, color);
				}
			}
			catch (NumberFormatException e)
			{
				log.warn("Failed to parse tip jar amount: {}", amountString, e);
			}
		}
	}

	// --- Ad Warning ---

	private void handleAdWarning(String message)
	{
		if (!message.startsWith(BurnerAlarmConstants.AD_WARNING_PREFIX))
		{
			return;
		}

		Notification adNotification = config.adWarningNotification();
		if (message.contains("3 minutes"))
		{
			notifier.notify(adNotification,
				"Your house advertisement will be removed in 3 minutes!");
			if (config.advancedPohDetectionEnabled())
			{
				if (adWarningInfobox != null)
				{
					infoBoxManager.removeInfoBox(adWarningInfobox);
				}
				final BufferedImage noticeboardIcon = itemManager.getImage(8168);
				adWarningInfobox = new AdvertisementWarningInfobox(
					Duration.ofMinutes(3), noticeboardIcon, this);
				infoBoxManager.addInfoBox(adWarningInfobox);
			}
		}
		else if (message.contains("2 minutes"))
		{
			notifier.notify(adNotification,
				"Your house advertisement will be removed in 2 minutes!");
		}
		else if (message.contains("1 minute"))
		{
			notifier.notify(adNotification,
				"Your house advertisement will be removed in 1 minute!");
		}
	}

	// --- Marrentill Tracking ---

	private void updateUnnotedMarrentillCount()
	{
		if (!houseDetectionService.shouldRunFeature(config.allowFeaturesInGuestPOH()))
		{
			showMarrentillOverlay = false;
			notifiedLowStock = false;
			return;
		}

		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		int count = 0;
		if (inventory != null)
		{
			for (Item item : inventory.getItems())
			{
				if (item != null && item.getId() == BurnerAlarmConstants.CLEAN_MARRENTILL_ID
					&& client.getItemDefinition(item.getId()).getNote() == -1)
				{
					count += item.getQuantity();
				}
			}
		}
		int previousCount = this.unnotedMarrentillCount;
		this.unnotedMarrentillCount = count;

		if (config.marrentillNotification().isEnabled() || config.marrentillGameMessage()
			|| config.marrentillOverlay())
		{
			if (count <= 1 && !notifiedLowStock && previousCount > 1)
			{
				if (config.marrentillOverlay())
				{
					showMarrentillOverlay = true;
				}

				String message = (count == 0)
					? "You're out of unnoted Marrentills!"
					: "You're almost out of unnoted Marrentills!";
				if (config.marrentillNotification().isEnabled())
				{
					notifier.notify(config.marrentillNotification(),
						BurnerAlarmConstants.PLUGIN_PREFIX + message);
				}
				if (config.marrentillGameMessage())
				{
					client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
						ColorUtil.wrapWithColorTag(
							BurnerAlarmConstants.PLUGIN_PREFIX + message,
							config.marrentillGameMessageColor()),
						null);
				}
				notifiedLowStock = true;
			}
			else if (count > 1)
			{
				showMarrentillOverlay = false;
				notifiedLowStock = false;
			}
		}
		else
		{
			showMarrentillOverlay = false;
			notifiedLowStock = false;
		}
	}

	// --- Guest Tracking ---

	private void updateGuestCount()
	{
		if (houseDetectionService.isInPOH())
		{
			int currentGuestCount = 0;
			Player localPlayer = client.getLocalPlayer();
			if (localPlayer != null)
			{
				for (Player player : client.getTopLevelWorldView().players())
				{
					if (player != null && !player.equals(localPlayer))
					{
						currentGuestCount++;
					}
				}
			}
			this.guestCount = currentGuestCount;
		}
		else
		{
			guestCount = -1;
		}
	}

	// --- Trade Capture ---

	private String getTradePartnersName()
	{
		Widget nameWidget = client.getWidget(InterfaceID.TRADECONFIRM,
			BurnerAlarmConstants.TRADE_WINDOW_OPPONENT_NAME_CHILD_ID);
		if (nameWidget == null)
		{
			return null;
		}
		return Text.removeTags(nameWidget.getText()).replace("Trading with:", "").trim();
	}

	private long getOpponentTradeValue()
	{
		Widget valueWidget = client.getWidget(InterfaceID.TRADECONFIRM,
			BurnerAlarmConstants.TRADE_WINDOW_OPPONENT_VALUE_TEXT_CHILD_ID);
		if (valueWidget == null)
		{
			return 0;
		}
		return parseValueFromWidgetText(valueWidget.getText());
	}

	private long parseValueFromWidgetText(String text)
	{
		Matcher m = BurnerAlarmConstants.OPPONENT_VALUE_PATTERN.matcher(Text.removeTags(text));
		if (!m.matches())
		{
			return 0;
		}

		String matchedText = m.group(1);
		if (matchedText.equals("Lots!"))
		{
			return Integer.MAX_VALUE;
		}

		matchedText = matchedText.replace(" coins", "");
		try
		{
			return VALUE_FORMAT.parse(matchedText).longValue();
		}
		catch (ParseException e)
		{
			return 0;
		}
	}

	// --- Expel Guests ---

	private void updateExpelGuestsButtonVisibility()
	{
		Widget expelGuestsButton = client.getWidget(InterfaceID.POH_OPTIONS, 20);
		if (expelGuestsButton != null)
		{
			expelGuestsButton.setHidden(config.hideExpelGuests());
		}
	}

	// --- Tip Tracker Panel ---

	private void updateTipTrackerPanelVisibility()
	{
		if (config.enableTipTracker())
		{
			if (tipTrackerManager == null)
			{
				tipTrackerManager = new TipTrackerManager(client, configManager, config, gson);
				tipTrackerManager.init();
				tipPanel = new TipTrackerPanel(tipTrackerManager, config);
				tipTrackerManager.setPanel(tipPanel);

				tipTrackerManager.setPlayerHighlightService(playerHighlightService);
				playerHighlightService.setTipTrackerManager(tipTrackerManager);

				final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "house.png");
				tipNavButton = NavigationButton.builder()
					.tooltip("House Hosting")
					.icon(icon)
					.priority(5)
					.panel(tipPanel)
					.build();
				clientToolbar.addNavigation(tipNavButton);
				SwingUtilities.invokeLater(() -> tipPanel.rebuild());
			}
		}
		else
		{
			if (tipTrackerManager != null)
			{
				clientToolbar.removeNavigation(tipNavButton);
				tipTrackerManager.shutdown();

				playerHighlightService.setTipTrackerManager(null);

				tipTrackerManager = null;
				tipPanel = null;
				tipNavButton = null;
			}
		}
	}

	private void handleReset()
	{
		if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
		{
			JOptionPane.showMessageDialog(tipPanel,
				"You must be logged in to reset character data.",
				"Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		final String playerName = client.getLocalPlayer().getName();
		final String confirmationText = "reset";
		int result = JOptionPane.showConfirmDialog(tipPanel,
			"Are you sure you want to delete all tips for " + playerName
				+ "?\nThis action cannot be undone.",
			"Confirm Deletion",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE);
		if (result != JOptionPane.YES_OPTION)
		{
			return;
		}

		String input = JOptionPane.showInputDialog(tipPanel,
			"To confirm, please type '" + confirmationText + "' in the box below.",
			"Final Confirmation",
			JOptionPane.WARNING_MESSAGE);
		if (confirmationText.equalsIgnoreCase(input))
		{
			if (tipTrackerManager != null)
			{
				tipTrackerManager.clearAllTips();
			}
			JOptionPane.showMessageDialog(tipPanel,
				"All tips for " + playerName + " have been deleted.",
				"Success", JOptionPane.INFORMATION_MESSAGE);
		}
		else if (input != null)
		{
			JOptionPane.showMessageDialog(tipPanel,
				"The confirmation text did not match. No data has been deleted.",
				"Cancelled", JOptionPane.ERROR_MESSAGE);
		}
	}
}

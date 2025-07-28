package net.runelite.client.plugins.burneralarm;

import net.runelite.api.GameObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

import javax.inject.Inject;
import java.awt.*;

/**
 * HouseHostingOverlay displays the current number of guests in the player's POH
 * and draws outlines around unlit burners.
 */
public class BurnerAlarmOverlay extends Overlay {
    private final BurnerAlarmPlugin plugin;
    private final BurnerAlarmConfig config;
    private final PanelComponent panelComponent = new PanelComponent();
    private final ModelOutlineRenderer modelOutlineRenderer;

    @Inject
    public BurnerAlarmOverlay(BurnerAlarmPlugin plugin, BurnerAlarmConfig config,
                              ModelOutlineRenderer modelOutlineRenderer) {
        this.plugin = plugin;
        this.config = config;
        this.modelOutlineRenderer = modelOutlineRenderer;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        // Render POH Guests counter
        if (config.pohGuestTrackerEnabled() && plugin.isInPOH() && plugin.getGuestCount() >= 0) {
            panelComponent.getChildren().clear();

            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("POH Guests")
                    .build());

            panelComponent.getChildren().add(TitleComponent.builder()
                    .text(String.valueOf(plugin.getGuestCount()))
                    .build());

            panelComponent.render(graphics);
        }

        // Draw outlines around unlit burners
        if (config.enableUnlitBurnerOutline()) {
            Color highlightColor = config.unlitBurnerHighlightColor();
            int borderWidth = config.unlitBurnerBorderWidth();
            int outlineFeather = config.unlitBurnerOutlineFeather();

            for (GameObject unlitBurner : plugin.getUnlitBurners().keySet()) {
                modelOutlineRenderer.drawOutline(unlitBurner, borderWidth, highlightColor, outlineFeather);
            }
        }

        return null;
    }
}
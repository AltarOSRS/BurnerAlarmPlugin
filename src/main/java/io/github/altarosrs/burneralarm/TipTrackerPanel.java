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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.QuantityFormatter;

public class TipTrackerPanel extends PluginPanel
{
	private enum Timeframe
	{
		SESSION("Session"),
		WEEK("Week"),
		MONTH("Month"),
		ALL("All Time");

		private final String name;

		Timeframe(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return this.name;
		}
	}

	private static final int RECENT_TIPS_LIMIT = 500;
	private static final int LEADERBOARD_LIMIT = 100;

	private static final DateTimeFormatter TIME_FORMATTER =
		DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter DATE_FORMATTER =
		DateTimeFormatter.ofPattern("MMM d").withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter DATE_YEAR_FORMATTER =
		DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter INPUT_DATE_FORMATTER =
		DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

	private static final ImageIcon ADD_ICON;
	private static final ImageIcon EDIT_ICON;
	private static final ImageIcon DELETE_ICON;

	static
	{
		final BufferedImage addIcon = ImageUtil.loadImageResource(TipTrackerPanel.class, "add_icon.png");
		final BufferedImage editIcon = ImageUtil.loadImageResource(TipTrackerPanel.class, "edit_icon.png");
		final BufferedImage deleteIcon = ImageUtil.loadImageResource(TipTrackerPanel.class, "delete_icon.png");

		ADD_ICON = new ImageIcon(addIcon);
		EDIT_ICON = new ImageIcon(editIcon);
		DELETE_ICON = new ImageIcon(deleteIcon);
	}

	private final TipTrackerManager manager;
	private final BurnerAlarmConfig config;

	private final JPanel recentTipsContainer = new JPanel();
	private final JPanel leaderboardContainer = new JPanel();
	private final JLabel sessionTotalLabel = new JLabel();
	private final JLabel weeklyTotalLabel = new JLabel();
	private final JLabel monthlyTotalLabel = new JLabel();
	private final JLabel allTimeTotalLabel = new JLabel();
	private final JTextField searchBar = new JTextField();
	private final JComboBox<Timeframe> filterDropdown;

	public TipTrackerPanel(TipTrackerManager manager, BurnerAlarmConfig config)
	{
		super(false);
		this.manager = manager;
		this.config = config;
		this.filterDropdown = new JComboBox<>(Timeframe.values());
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(createHeaderPanel(), BorderLayout.NORTH);

		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Recent Tips", createRecentTipsPanel());
		tabbedPane.addTab("Leaderboard", createLeaderboardPanel());
		mainPanel.add(tabbedPane, BorderLayout.CENTER);

		add(mainPanel, BorderLayout.CENTER);
	}

	private JPanel createHeaderPanel()
	{
		JPanel headerPanel = new JPanel(new BorderLayout());
		headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

		JLabel titleLabel = new JLabel("Tip Tracker");
		titleLabel.setForeground(Color.WHITE);
		headerPanel.add(titleLabel, BorderLayout.WEST);

		JButton addButton = new JButton(ADD_ICON);
		addButton.setToolTipText("Add a manual tip");
		addButton.addActionListener(e -> addManualTip());
		headerPanel.add(addButton, BorderLayout.EAST);

		return headerPanel;
	}

	private JPanel createRecentTipsPanel()
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(new EmptyBorder(8, 8, 8, 8));

		JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
		searchPanel.setBorder(new EmptyBorder(0, 0, 8, 0));
		searchBar.setToolTipText("Search by player name");
		searchBar.getDocument().addDocumentListener((SimpleDocumentListener) e -> rebuild());
		searchPanel.add(new JLabel("Search: "), BorderLayout.WEST);
		searchPanel.add(searchBar, BorderLayout.CENTER);
		panel.add(searchPanel, BorderLayout.NORTH);

		recentTipsContainer.setLayout(new BoxLayout(recentTipsContainer, BoxLayout.Y_AXIS));
		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.add(recentTipsContainer, BorderLayout.NORTH);
		JScrollPane scrollPane = new JScrollPane(wrapper);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		panel.add(scrollPane, BorderLayout.CENTER);

		return panel;
	}

	private JPanel createLeaderboardPanel()
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(new EmptyBorder(8, 8, 8, 8));

		JPanel totalsPanel = new JPanel(new GridLayout(2, 2, 5, 5));
		totalsPanel.add(createTotalBox("Session:", sessionTotalLabel));
		totalsPanel.add(createTotalBox("This Week:", weeklyTotalLabel));
		totalsPanel.add(createTotalBox("This Month:", monthlyTotalLabel));
		totalsPanel.add(createTotalBox("All Time:", allTimeTotalLabel));

		filterDropdown.setSelectedItem(Timeframe.ALL);
		filterDropdown.addActionListener(e -> rebuild());

		JPanel dropdownWrapper = new JPanel(new BorderLayout());
		dropdownWrapper.setBorder(new EmptyBorder(8, 0, 8, 0));
		dropdownWrapper.add(filterDropdown, BorderLayout.CENTER);

		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.add(totalsPanel, BorderLayout.NORTH);
		topPanel.add(dropdownWrapper, BorderLayout.CENTER);
		panel.add(topPanel, BorderLayout.NORTH);

		leaderboardContainer.setLayout(new BoxLayout(leaderboardContainer, BoxLayout.Y_AXIS));
		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.add(leaderboardContainer, BorderLayout.NORTH);
		JScrollPane scrollPane = new JScrollPane(wrapper);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		panel.add(scrollPane, BorderLayout.CENTER);

		return panel;
	}

	private JPanel createTotalBox(String title, JLabel valueLabel)
	{
		JPanel box = new JPanel(new BorderLayout());
		box.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		box.setBorder(new EmptyBorder(8, 8, 8, 8));

		JLabel titleLabel = new JLabel(title);
		titleLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		valueLabel.setForeground(Color.WHITE);

		box.add(titleLabel, BorderLayout.NORTH);
		box.add(valueLabel, BorderLayout.CENTER);
		return box;
	}

	public void rebuild()
	{
		rebuildRecentTips();
		rebuildLeaderboard();
		revalidate();
		repaint();
	}

	private void rebuildRecentTips()
	{
		recentTipsContainer.removeAll();
		List<Tip> allTips = manager.getTips();
		String searchQuery = searchBar.getText().toLowerCase();
		List<Tip> tipsToDisplay;

		if (searchQuery.isEmpty())
		{
			int listSize = allTips.size();
			tipsToDisplay = allTips.subList(Math.max(0, listSize - RECENT_TIPS_LIMIT), listSize);
		}
		else
		{
			tipsToDisplay = allTips.stream()
				.filter(tip -> tip.getPlayerName().toLowerCase().contains(searchQuery))
				.collect(Collectors.toList());
		}

		if (tipsToDisplay.isEmpty())
		{
			PluginErrorPanel errorPanel = new PluginErrorPanel();
			if (searchQuery.isEmpty())
			{
				errorPanel.setContent("No tips recorded yet.", "Click the '+' icon to add a manual tip.");
			}
			else
			{
				errorPanel.setContent("No tips found.", "Your search query did not match any tips.");
			}
			recentTipsContainer.add(errorPanel);
		}
		else
		{
			for (int i = tipsToDisplay.size() - 1; i >= 0; i--)
			{
				Tip tip = tipsToDisplay.get(i);
				recentTipsContainer.add(createTipRow(tip));
				recentTipsContainer.add(Box.createRigidArea(new Dimension(0, 2)));
			}
		}
	}

	private void rebuildLeaderboard()
	{
		long sessionTotal = manager.getSessionTotal();
		long weeklyTotal = manager.getWeeklyTotal();
		long monthlyTotal = manager.getMonthlyTotal();
		long allTimeTotal = manager.getAllTimeTotal();
		sessionTotalLabel.setText(QuantityFormatter.quantityToRSDecimalStack((int) sessionTotal, true) + " gp");
		weeklyTotalLabel.setText(QuantityFormatter.quantityToRSDecimalStack((int) weeklyTotal, true) + " gp");
		monthlyTotalLabel.setText(QuantityFormatter.quantityToRSDecimalStack((int) monthlyTotal, true) + " gp");
		allTimeTotalLabel.setText(QuantityFormatter.quantityToRSDecimalStack((int) allTimeTotal, true) + " gp");

		leaderboardContainer.removeAll();

		Timeframe selectedTimeframe = (Timeframe) filterDropdown.getSelectedItem();
		if (selectedTimeframe == null)
		{
			return;
		}

		Instant timeFilter;
		switch (selectedTimeframe)
		{
			case WEEK:
				timeFilter = Instant.now().minus(7, ChronoUnit.DAYS);
				break;
			case MONTH:
				timeFilter = Instant.now().minus(30, ChronoUnit.DAYS);
				break;
			case SESSION:
				timeFilter = manager.getSessionStartTime();
				break;
			case ALL:
			default:
				timeFilter = Instant.EPOCH;
				break;
		}

		List<LeaderboardEntry> fullLeaderboard = selectedTimeframe == Timeframe.ALL
			? manager.getAllTimeLeaderboard()
			: manager.getLeaderboard(timeFilter);

		List<LeaderboardEntry> leaderboardToDisplay = fullLeaderboard.subList(
			0, Math.min(LEADERBOARD_LIMIT, fullLeaderboard.size()));

		if (leaderboardToDisplay.isEmpty())
		{
			PluginErrorPanel errorPanel = new PluginErrorPanel();
			errorPanel.setContent("No tips to display.", "Tips from the tip jar or trades will appear here.");
			leaderboardContainer.add(errorPanel);
		}
		else
		{
			for (LeaderboardEntry entry : leaderboardToDisplay)
			{
				leaderboardContainer.add(createLeaderboardRow(entry));
				leaderboardContainer.add(Box.createRigidArea(new Dimension(0, 2)));
			}
		}
	}

	private String getFormattedTimestamp(Instant timestamp)
	{
		LocalDate tipDate = timestamp.atZone(ZoneId.systemDefault()).toLocalDate();
		LocalDate today = LocalDate.now(ZoneId.systemDefault());

		if (tipDate.getYear() != today.getYear())
		{
			return DATE_YEAR_FORMATTER.format(timestamp) + ", " + TIME_FORMATTER.format(timestamp);
		}
		if (tipDate.equals(today))
		{
			return TIME_FORMATTER.format(timestamp);
		}
		if (tipDate.equals(today.minusDays(1)))
		{
			return "Yesterday, " + TIME_FORMATTER.format(timestamp);
		}
		return DATE_FORMATTER.format(timestamp) + ", " + TIME_FORMATTER.format(timestamp);
	}

	private JPanel createTipRow(Tip tip)
	{
		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new EmptyBorder(5, 5, 5, 5));

		JPanel topLine = new JPanel(new BorderLayout(5, 0));
		topLine.setOpaque(false);

		String formattedAmount = QuantityFormatter.quantityToRSDecimalStack((int) tip.getAmount(), true);
		JLabel amountLabel = new JLabel(formattedAmount + " gp");
		amountLabel.setForeground(manager.getColorForAmount(tip.getAmount()));
		topLine.add(amountLabel, BorderLayout.WEST);

		String sourceText = getSourceText(tip);
		JLabel sourceLabel = new JLabel(sourceText);
		sourceLabel.setFont(FontManager.getRunescapeSmallFont());
		sourceLabel.setForeground(Color.LIGHT_GRAY);
		topLine.add(sourceLabel, BorderLayout.CENTER);

		JPanel bottomLine = new JPanel(new BorderLayout());
		bottomLine.setOpaque(false);

		String timeText = getFormattedTimestamp(tip.getTimestamp());
		JLabel timeLabel = new JLabel(timeText);
		timeLabel.setFont(FontManager.getRunescapeSmallFont());
		timeLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		bottomLine.add(timeLabel, BorderLayout.WEST);

		JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
		controls.setOpaque(false);

		JButton editButton = new JButton(EDIT_ICON);
		editButton.setPreferredSize(new Dimension(16, 16));
		editButton.setToolTipText("Edit tip amount");
		editButton.addActionListener(e -> editTip(tip));

		JButton deleteButton = new JButton(DELETE_ICON);
		deleteButton.setPreferredSize(new Dimension(16, 16));
		deleteButton.setToolTipText("Delete tip");
		deleteButton.addActionListener(e -> deleteTip(tip));

		controls.add(editButton);
		controls.add(deleteButton);
		bottomLine.add(controls, BorderLayout.EAST);

		row.add(topLine);
		row.add(bottomLine);
		return row;
	}

	private String getSourceText(Tip tip)
	{
		switch (tip.getSource())
		{
			case TIP_JAR:
				return "from " + tip.getPlayerName() + " (Tip Jar)";
			case TRADE:
				return "from " + tip.getPlayerName() + " (Trade)";
			case MANUAL:
			default:
				return tip.getPlayerName().equalsIgnoreCase("Manual")
					? "Manual Entry"
					: "from " + tip.getPlayerName() + " (Manual)";
		}
	}

	private JPanel createLeaderboardRow(LeaderboardEntry entry)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new EmptyBorder(5, 5, 5, 5));

		String formattedAmount = QuantityFormatter.quantityToRSDecimalStack((int) entry.getTotalAmount(), true);
		String tipCount = entry.getTipCount() + (entry.getTipCount() > 1 ? " tips" : " tip");

		JLabel infoLabel = new JLabel("<html>" + entry.getPlayerName()
			+ "<br><font color='#888888'>" + tipCount + "</font></html>");
		JLabel amountLabel = new JLabel(formattedAmount + " gp");
		amountLabel.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);

		row.add(infoLabel, BorderLayout.CENTER);
		row.add(amountLabel, BorderLayout.EAST);
		return row;
	}

	private void addManualTip()
	{
		JPanel dialogPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2, 2, 2, 2);

		c.gridx = 0;
		c.gridy = 0;
		dialogPanel.add(new JLabel("Player Name:"), c);
		c.gridx = 1;
		c.gridy = 0;
		JTextField nameField = new JTextField("Manual");
		dialogPanel.add(nameField, c);

		c.gridx = 0;
		c.gridy = 1;
		dialogPanel.add(new JLabel("Amount:"), c);
		c.gridx = 1;
		c.gridy = 1;
		JTextField amountField = new JTextField();
		dialogPanel.add(amountField, c);

		c.gridx = 0;
		c.gridy = 2;
		dialogPanel.add(new JLabel("Date (Optional):"), c);
		c.gridx = 1;
		c.gridy = 2;
		JTextField dateField = new JTextField();
		dateField.setToolTipText("Format: YYYY-MM-DD");
		dialogPanel.add(dateField, c);

		int result = JOptionPane.showConfirmDialog(this, dialogPanel,
			"Add Manual Tip", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (result == JOptionPane.OK_OPTION)
		{
			try
			{
				long amount = QuantityFormatter.parseQuantity(amountField.getText());
				String name = nameField.getText().isEmpty() ? "Manual" : nameField.getText();
				String date = dateField.getText();

				Instant timestamp = Instant.now();
				if (!date.isEmpty())
				{
					try
					{
						LocalDate localDate = LocalDate.parse(date, INPUT_DATE_FORMATTER);
						timestamp = localDate.atTime(LocalTime.now())
							.atZone(ZoneId.systemDefault()).toInstant();
					}
					catch (DateTimeParseException ex)
					{
						JOptionPane.showMessageDialog(this,
							"Invalid date format. Please use YYYY-MM-DD.",
							"Date Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
				}
				manager.addTip(new Tip(amount, name, timestamp, TipSource.MANUAL));
			}
			catch (ParseException ex)
			{
				JOptionPane.showMessageDialog(this,
					"Invalid amount entered.", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void editTip(Tip tip)
	{
		String input = JOptionPane.showInputDialog(this,
			"Enter new tip amount:", String.valueOf(tip.getAmount()));
		if (input == null || input.isEmpty())
		{
			return;
		}
		try
		{
			long newAmount = QuantityFormatter.parseQuantity(input);
			manager.updateTipAmount(tip, newAmount);
		}
		catch (ParseException ex)
		{
			JOptionPane.showMessageDialog(this,
				"Invalid amount entered.", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void deleteTip(Tip tip)
	{
		int result = JOptionPane.showConfirmDialog(this,
			"Are you sure you want to delete this tip?",
			"Confirm Deletion", JOptionPane.YES_NO_OPTION);
		if (result == JOptionPane.YES_OPTION)
		{
			manager.deleteTip(tip);
		}
	}

	@FunctionalInterface
	private interface SimpleDocumentListener extends DocumentListener
	{
		void update(DocumentEvent e);

		@Override
		default void insertUpdate(DocumentEvent e)
		{
			update(e);
		}

		@Override
		default void removeUpdate(DocumentEvent e)
		{
			update(e);
		}

		@Override
		default void changedUpdate(DocumentEvent e)
		{
			update(e);
		}
	}
}

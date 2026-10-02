package me.monstermaze.engine.play;

import me.monstermaze.engine.api.KitType;
import me.monstermaze.engine.api.MazeMode;
import me.monstermaze.engine.game.StageTimer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

public final class ConfigDialog extends JDialog {

    private final GameConfig config;
    private final UiTheme theme;
    private final JComboBox<String> kitBox;
    private final JComboBox<String> modeBox;
    private final JComboBox<String> layoutBox;
    private final JCheckBox useDefaultMobs;
    private final JSpinner monsters;
    private final JSpinner zoom;
    private final JSpinner windowSize;
    private final JCheckBox sfx;
    private final JCheckBox darkMode;
    private final JCheckBox showHints;
    private final JTextField seedField;
    private boolean accepted = false;

    public ConfigDialog(Frame owner, GameConfig config) {
        super(owner, "Settings", true);
        this.config = config;
        this.theme = UiTheme.of(config.darkMode);
        theme.applyLookAndFeel();

        getContentPane().setBackground(theme.bg);
        setLayout(new BorderLayout(12, 12));
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(14, 16, 8, 16));
        root.setBackground(theme.bg);

        JPanel gameplay = section("Gameplay");
        kitBox = combo(new String[]{"Jumper", "Body Builder", "Slowballer", "Repulsor", "Maverick"});
        kitBox.setSelectedItem(displayKit(config.kitType()));
        modeBox = combo(new String[]{"ORIGINAL", "SPEED", "MODERN"});
        modeBox.setSelectedItem(config.mazeMode().name());
        layoutBox = combo(new String[]{"0", "1", "2"});
        layoutBox.setSelectedItem(String.valueOf(config.layout));
        useDefaultMobs = check("Use mode default monster count", config.useDefaultMobs);
        monsters = spinner(Math.max(5, config.monsters), 5, 300, 5);
        monsters.setEnabled(!config.useDefaultMobs);
        if (config.useDefaultMobs) monsters.setValue(StageTimer.starterMonsters(config.mazeMode()));
        seedField = field(config.lastSeed == 0 ? "" : String.valueOf(config.lastSeed));
        seedField.setToolTipText("Leave empty for a random seed each run");

        modeBox.addActionListener(e -> {
            if (useDefaultMobs.isSelected()) {
                monsters.setValue(StageTimer.starterMonsters(MazeMode.valueOf((String) modeBox.getSelectedItem())));
            }
        });
        useDefaultMobs.addActionListener(e -> {
            monsters.setEnabled(!useDefaultMobs.isSelected());
            if (useDefaultMobs.isSelected()) {
                monsters.setValue(StageTimer.starterMonsters(MazeMode.valueOf((String) modeBox.getSelectedItem())));
            }
        });

        addRow(gameplay, "Kit", kitBox);
        addRow(gameplay, "Mode", modeBox);
        addRow(gameplay, "Layout", layoutBox);
        addRow(gameplay, "", useDefaultMobs);
        addRow(gameplay, "Monsters", monsters);
        addRow(gameplay, "Seed (optional)", seedField);
        root.add(gameplay);
        root.add(Box.createVerticalStrut(8));

        JPanel display = section("Display & Audio");
        zoom = spinnerD(config.zoom, 0.8, 6.0, 0.1);
        windowSize = spinner(config.windowSize, 360, 900, 20);
        darkMode = check("Dark mode", config.darkMode);
        showHints = check("Show controls overlay in-game", config.showControlsHint);
        sfx = check("Enable sound effects", config.sfxEnabled);
        addRow(display, "Default zoom", zoom);
        addRow(display, "Window size (px)", windowSize);
        addRow(display, "", darkMode);
        addRow(display, "", showHints);
        addRow(display, "", sfx);
        root.add(display);

        JLabel pathHint = new JLabel("<html><small>Saved to " + GameConfig.configPath() + "</small></html>");
        pathHint.setForeground(theme.textMuted);
        pathHint.setBorder(new EmptyBorder(8, 4, 4, 4));
        root.add(pathHint);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBackground(theme.bg);
        JButton cancel = theme.secondaryButton("Cancel");
        JButton save = theme.primaryButton("Save");
        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> {
            if (!apply()) return;
            accepted = true;
            dispose();
        });
        buttons.add(cancel);
        buttons.add(save);

        add(root, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    public boolean isAccepted() { return accepted; }

    private boolean apply() {
        KitType kit = mapKit((String) kitBox.getSelectedItem());
        MazeMode mode = MazeMode.valueOf((String) modeBox.getSelectedItem());
        if (kit == KitType.MAVERICK && mode == MazeMode.ORIGINAL) {
            JOptionPane.showMessageDialog(this, "Maverick requires SPEED or MODERN mode.");
            return false;
        }
        config.kit = kit.name();
        config.mode = mode.name();
        config.layout = Integer.parseInt((String) layoutBox.getSelectedItem());
        config.useDefaultMobs = useDefaultMobs.isSelected();
        config.monsters = (Integer) monsters.getValue();
        config.zoom = ((Number) zoom.getValue()).doubleValue();
        config.windowSize = (Integer) windowSize.getValue();
        config.sfxEnabled = sfx.isSelected();
        config.darkMode = darkMode.isSelected();
        config.showControlsHint = showHints.isSelected();
        String seedText = seedField.getText().trim();
        if (seedText.isEmpty()) config.lastSeed = 0;
        else {
            try { config.lastSeed = Long.parseLong(seedText); }
            catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Seed must be a number or empty.");
                return false;
            }
        }
        config.save();
        return true;
    }

    private JPanel section(String title) {
        JPanel p = new JPanel(new GridBagLayout());
        TitledBorder tb = BorderFactory.createTitledBorder(title);
        tb.setTitleColor(theme.accent);
        p.setBorder(tb);
        p.setBackground(theme.bgRaised);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private void addRow(JPanel panel, String label, JComponent field) {
        GridBagConstraints lc = new GridBagConstraints();
        lc.gridx = 0; lc.anchor = GridBagConstraints.WEST;
        lc.insets = new Insets(4, 6, 4, 10);
        GridBagConstraints fc = new GridBagConstraints();
        fc.gridx = 1; fc.fill = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1; fc.insets = new Insets(4, 0, 4, 6);
        JLabel l = new JLabel(label.isEmpty() ? " " : label);
        l.setForeground(theme.text);
        panel.add(l, lc);
        panel.add(field, fc);
    }

    private JComboBox<String> combo(String[] items) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setBackground(theme.bgPanel);
        c.setForeground(theme.text);
        return c;
    }

    private JCheckBox check(String text, boolean on) {
        JCheckBox c = new JCheckBox(text, on);
        c.setBackground(theme.bgRaised);
        c.setForeground(theme.text);
        return c;
    }

    private JSpinner spinner(int val, int min, int max, int step) {
        return new JSpinner(new SpinnerNumberModel(val, min, max, step));
    }

    private JSpinner spinnerD(double val, double min, double max, double step) {
        return new JSpinner(new SpinnerNumberModel(val, min, max, step));
    }

    private JTextField field(String text) {
        JTextField f = new JTextField(text, 14);
        f.setBackground(theme.bgPanel);
        f.setForeground(theme.text);
        f.setCaretColor(theme.text);
        return f;
    }

    private static String displayKit(KitType k) {
        switch (k) {
            case SLOWBALL: return "Slowballer";
            case BODY_BUILDER: return "Body Builder";
            case JUMPER: return "Jumper";
            case REPULSOR: return "Repulsor";
            case MAVERICK: return "Maverick";
            default: return k.name();
        }
    }

    private static KitType mapKit(String name) {
        switch (name) {
            case "Body Builder": return KitType.BODY_BUILDER;
            case "Slowballer": return KitType.SLOWBALL;
            case "Repulsor": return KitType.REPULSOR;
            case "Maverick": return KitType.MAVERICK;
            default: return KitType.JUMPER;
        }
    }
}

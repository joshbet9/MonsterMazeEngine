package me.monstermaze.engine.play;

import me.monstermaze.engine.api.KitType;
import me.monstermaze.engine.api.MazeMode;
import me.monstermaze.engine.game.StageTimer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/** Settings dialog: gameplay + display + audio. Saves to GameConfig. */
public final class ConfigDialog extends JDialog {

    private final GameConfig config;
    private final JComboBox<String> kitBox;
    private final JComboBox<String> modeBox;
    private final JComboBox<String> layoutBox;
    private final JCheckBox useDefaultMobs;
    private final JSpinner monsters;
    private final JSpinner zoom;
    private final JSpinner windowSize;
    private final JCheckBox sfx;
    private final JTextField seedField;
    private boolean accepted = false;

    public ConfigDialog(Frame owner, GameConfig config) {
        super(owner, "Settings", true);
        this.config = config;

        setLayout(new BorderLayout(12, 12));
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(14, 16, 8, 16));

        JPanel gameplay = section("Gameplay");
        kitBox = new JComboBox<>(new String[]{
                "Jumper", "Body Builder", "Slowballer", "Repulsor", "Maverick"
        });
        kitBox.setSelectedItem(displayKit(config.kitType()));
        modeBox = new JComboBox<>(new String[]{"ORIGINAL", "SPEED", "MODERN"});
        modeBox.setSelectedItem(config.mazeMode().name());
        layoutBox = new JComboBox<>(new String[]{"0", "1", "2"});
        layoutBox.setSelectedItem(String.valueOf(config.layout));
        useDefaultMobs = new JCheckBox("Use mode default monster count", config.useDefaultMobs);
        monsters = new JSpinner(new SpinnerNumberModel(Math.max(5, config.monsters), 5, 300, 5));
        monsters.setEnabled(!config.useDefaultMobs);
        if (config.useDefaultMobs) {
            monsters.setValue(StageTimer.starterMonsters(config.mazeMode()));
        }
        seedField = new JTextField(config.lastSeed == 0 ? "" : String.valueOf(config.lastSeed), 14);
        seedField.setToolTipText("Leave empty for a random seed each run");

        modeBox.addActionListener(e -> {
            if (useDefaultMobs.isSelected()) {
                MazeMode m = MazeMode.valueOf((String) modeBox.getSelectedItem());
                monsters.setValue(StageTimer.starterMonsters(m));
            }
        });
        useDefaultMobs.addActionListener(e -> {
            monsters.setEnabled(!useDefaultMobs.isSelected());
            if (useDefaultMobs.isSelected()) {
                MazeMode m = MazeMode.valueOf((String) modeBox.getSelectedItem());
                monsters.setValue(StageTimer.starterMonsters(m));
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

        JPanel display = section("Display");
        zoom = new JSpinner(new SpinnerNumberModel(config.zoom, 0.8, 6.0, 0.1));
        windowSize = new JSpinner(new SpinnerNumberModel(config.windowSize, 360, 900, 20));
        addRow(display, "Default zoom", zoom);
        addRow(display, "Window size (px)", windowSize);
        root.add(display);
        root.add(Box.createVerticalStrut(8));

        JPanel audio = section("Audio");
        sfx = new JCheckBox("Enable sound effects", config.sfxEnabled);
        addRow(audio, "", sfx);
        root.add(audio);

        JLabel pathHint = new JLabel("<html><small>Saved to " + GameConfig.configPath() + "</small></html>");
        pathHint.setBorder(new EmptyBorder(8, 4, 4, 4));
        root.add(pathHint);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton("Cancel");
        JButton save = new JButton("Save");
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

    public boolean isAccepted() {
        return accepted;
    }

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
        String seedText = seedField.getText().trim();
        if (seedText.isEmpty()) {
            config.lastSeed = 0;
        } else {
            try {
                config.lastSeed = Long.parseLong(seedText);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Seed must be a number or empty.");
                return false;
            }
        }
        config.save();
        return true;
    }

    private static JPanel section(String title) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new TitledBorder(title));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private static void addRow(JPanel panel, String label, JComponent field) {
        GridBagConstraints lc = new GridBagConstraints();
        lc.gridx = 0; lc.anchor = GridBagConstraints.WEST;
        lc.insets = new Insets(4, 6, 4, 10);
        GridBagConstraints fc = new GridBagConstraints();
        fc.gridx = 1; fc.fill = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1; fc.insets = new Insets(4, 0, 4, 6);
        panel.add(new JLabel(label.isEmpty() ? " " : label), lc);
        panel.add(field, fc);
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

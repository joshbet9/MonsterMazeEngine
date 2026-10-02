package me.monstermaze.engine.play;

import me.monstermaze.engine.api.KitType;
import me.monstermaze.engine.api.MazeMode;
import me.monstermaze.engine.audio.Sfx;
import me.monstermaze.engine.game.StageTimer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Main GUI entry: title, config summary, Play / Settings / Quit. */
public final class MainMenu extends JFrame {

    private final GameConfig config;
    private final JLabel summary;

    public MainMenu() {
        super("Monster Maze");
        this.config = GameConfig.load();
        Sfx.setEnabled(config.sfxEnabled);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(new EmptyBorder(24, 28, 20, 28));
        root.setBackground(new Color(22, 22, 30));

        JLabel title = new JLabel("MONSTER MAZE", SwingConstants.CENTER);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        title.setForeground(new Color(90, 200, 255));

        JLabel subtitle = new JLabel("1.8 engine \u00b7 local play", SwingConstants.CENTER);
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        subtitle.setForeground(new Color(160, 160, 175));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        header.add(title);
        header.add(Box.createVerticalStrut(6));
        header.add(subtitle);

        summary = new JLabel(buildSummary(), SwingConstants.CENTER);
        summary.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        summary.setForeground(new Color(200, 200, 210));
        summary.setBorder(new EmptyBorder(16, 8, 16, 8));

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        buttons.setOpaque(false);

        JButton play = bigButton("Play");
        JButton settings = bigButton("Settings");
        JButton quit = bigButton("Quit");

        play.addActionListener(e -> startGame());
        settings.addActionListener(e -> openSettings());
        quit.addActionListener(e -> System.exit(0));

        buttons.add(play);
        buttons.add(Box.createVerticalStrut(8));
        buttons.add(settings);
        buttons.add(Box.createVerticalStrut(8));
        buttons.add(quit);

        root.add(header, BorderLayout.NORTH);
        root.add(summary, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setSize(Math.max(getWidth(), 360), Math.max(getHeight(), 380));
        setLocationRelativeTo(null);
    }

    private void openSettings() {
        ConfigDialog dlg = new ConfigDialog(this, config);
        dlg.setVisible(true);
        if (dlg.isAccepted()) {
            Sfx.setEnabled(config.sfxEnabled);
            summary.setText(buildSummary());
        }
    }

    private void startGame() {
        KitType kit = config.kitType();
        MazeMode mode = config.mazeMode();
        if (kit == KitType.MAVERICK && mode == MazeMode.ORIGINAL) {
            JOptionPane.showMessageDialog(this, "Maverick requires SPEED or MODERN.\nOpen Settings to change.");
            return;
        }
        setVisible(false);
        PlayFrame frame = new PlayFrame(
                mode, kit, config.layout, config.seedOrRandom(),
                config.monsterCount(), config, this);
        frame.setVisible(true);
        frame.startLoop();
    }

    public void returnFromGame() {
        Sfx.setEnabled(config.sfxEnabled);
        summary.setText(buildSummary());
        setVisible(true);
        toFront();
    }

    private String buildSummary() {
        int mobs = config.useDefaultMobs
                ? StageTimer.starterMonsters(config.mazeMode())
                : config.monsters;
        String seed = config.lastSeed == 0 ? "random" : String.valueOf(config.lastSeed);
        return String.format(
                "<html><div style='text-align:center'>"
                        + "Kit <b>%s</b> \u00b7 Mode <b>%s</b> \u00b7 Layout <b>%d</b><br>"
                        + "Mobs <b>%d</b> \u00b7 Zoom <b>%.1f</b> \u00b7 SFX <b>%s</b><br>"
                        + "Seed <b>%s</b>"
                        + "</div></html>",
                displayKit(config.kitType()),
                config.mazeMode().name(),
                config.layout,
                mobs,
                config.zoom,
                config.sfxEnabled ? "on" : "off",
                seed);
    }

    private static JButton bigButton(String text) {
        JButton b = new JButton(text);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setMaximumSize(new Dimension(220, 40));
        b.setPreferredSize(new Dimension(220, 40));
        b.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        b.setFocusPainted(false);
        return b;
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

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> new MainMenu().setVisible(true));
    }
}

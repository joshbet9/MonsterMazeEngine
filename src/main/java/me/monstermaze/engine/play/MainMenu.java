package me.monstermaze.engine.play;

import me.monstermaze.engine.api.KitType;
import me.monstermaze.engine.api.MazeMode;
import me.monstermaze.engine.audio.Sfx;
import me.monstermaze.engine.game.StageTimer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class MainMenu extends JFrame {

    private final GameConfig config;
    private UiTheme theme;
    private JLabel summary;
    private JPanel root;

    public MainMenu() {
        super("Monster Maze");
        this.config = GameConfig.load();
        Sfx.setEnabled(config.sfxEnabled);
        rebuild();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        pack();
        setSize(Math.max(getWidth(), 380), Math.max(getHeight(), 420));
        setLocationRelativeTo(null);
    }

    private void rebuild() {
        this.theme = UiTheme.of(config.darkMode);
        theme.applyLookAndFeel();

        root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(new EmptyBorder(28, 32, 24, 32));
        root.setBackground(theme.bg);

        JLabel title = new JLabel("MONSTER MAZE", SwingConstants.CENTER);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
        title.setForeground(theme.accent);

        JLabel subtitle = new JLabel("1.8 engine \u00b7 local play", SwingConstants.CENTER);
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        subtitle.setForeground(theme.textMuted);

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
        summary.setForeground(theme.text);
        summary.setBorder(new EmptyBorder(18, 8, 18, 8));

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        buttons.setOpaque(false);

        JButton play = theme.primaryButton("Play");
        JButton settings = theme.secondaryButton("Settings");
        JButton quit = theme.secondaryButton("Quit");
        for (JButton b : new JButton[]{play, settings, quit}) {
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setMaximumSize(new Dimension(240, 42));
            b.setPreferredSize(new Dimension(240, 42));
        }

        play.addActionListener(e -> startGame());
        settings.addActionListener(e -> openSettings());
        quit.addActionListener(e -> System.exit(0));

        buttons.add(play);
        buttons.add(Box.createVerticalStrut(10));
        buttons.add(settings);
        buttons.add(Box.createVerticalStrut(10));
        buttons.add(quit);

        root.add(header, BorderLayout.NORTH);
        root.add(summary, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
        revalidate();
        repaint();
    }

    private void openSettings() {
        ConfigDialog dlg = new ConfigDialog(this, config);
        dlg.setVisible(true);
        if (dlg.isAccepted()) {
            Sfx.setEnabled(config.sfxEnabled);
            rebuild();
            pack();
            setSize(Math.max(getWidth(), 380), Math.max(getHeight(), 420));
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
        rebuild();
        setVisible(true);
        toFront();
    }

    private String buildSummary() {
        int mobs = config.useDefaultMobs
                ? StageTimer.starterMonsters(config.mazeMode())
                : config.monsters;
        String seed = config.lastSeed == 0 ? "random" : String.valueOf(config.lastSeed);
        String themeName = config.darkMode ? "dark" : "light";
        return String.format(
                "<html><div style='text-align:center'>"
                        + "Kit <b>%s</b> \u00b7 Mode <b>%s</b> \u00b7 Layout <b>%d</b><br>"
                        + "Mobs <b>%d</b> \u00b7 Zoom <b>%.1f</b> \u00b7 SFX <b>%s</b><br>"
                        + "Theme <b>%s</b> \u00b7 Seed <b>%s</b>"
                        + "</div></html>",
                displayKit(config.kitType()), config.mazeMode().name(), config.layout,
                mobs, config.zoom, config.sfxEnabled ? "on" : "off",
                themeName, seed);
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
        SwingUtilities.invokeLater(() -> new MainMenu().setVisible(true));
    }
}

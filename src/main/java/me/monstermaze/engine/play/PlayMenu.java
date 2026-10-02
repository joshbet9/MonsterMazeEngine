package me.monstermaze.engine.play;

import me.monstermaze.engine.api.KitType;
import me.monstermaze.engine.api.MazeMode;
import me.monstermaze.engine.game.StageTimer;

import javax.swing.*;
import java.awt.*;

/** Setup menu: kit, mode, layout; monsters default per mode. */
public final class PlayMenu {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PlayMenu::show);
    }

    static void show() {
        JFrame menu = new JFrame("Monster Maze \u2014 Setup");
        menu.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JComboBox<String> kitBox = new JComboBox<>(new String[]{
                "Jumper", "Body Builder", "Slowballer", "Repulsor", "Maverick"
        });
        JComboBox<String> modeBox = new JComboBox<>(new String[]{
                "ORIGINAL", "SPEED", "MODERN"
        });
        JComboBox<String> layoutBox = new JComboBox<>(new String[]{"0", "1", "2"});
        JCheckBox useDefaultMobs = new JCheckBox("Use mode default", true);
        JSpinner monsters = new JSpinner(new SpinnerNumberModel(150, 5, 300, 5));
        monsters.setEnabled(false);

        modeBox.addActionListener(e -> {
            MazeMode m = MazeMode.valueOf((String) modeBox.getSelectedItem());
            if (useDefaultMobs.isSelected()) {
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
        monsters.setValue(StageTimer.starterMonsters(MazeMode.ORIGINAL));

        p.add(new JLabel("Kit"));
        p.add(kitBox);
        p.add(new JLabel("Mode"));
        p.add(modeBox);
        p.add(new JLabel("Layout"));
        p.add(layoutBox);
        p.add(useDefaultMobs);
        p.add(monsters);

        JLabel hint = new JLabel("<html>Defaults: ORIGINAL/SPEED = 150, MODERN = 225</html>");
        hint.setFont(hint.getFont().deriveFont(11f));

        JButton start = new JButton("Play");
        start.addActionListener(e -> {
            KitType kit = mapKit((String) kitBox.getSelectedItem());
            MazeMode mode = MazeMode.valueOf((String) modeBox.getSelectedItem());
            if (kit == KitType.MAVERICK && mode == MazeMode.ORIGINAL) {
                JOptionPane.showMessageDialog(menu, "Maverick requires SPEED or MODERN mode.");
                return;
            }
            int layout = Integer.parseInt((String) layoutBox.getSelectedItem());
            int mobs = useDefaultMobs.isSelected() ? -1 : (Integer) monsters.getValue();
            menu.dispose();
            PlayFrame frame = new PlayFrame(mode, kit, layout, System.currentTimeMillis(), mobs);
            frame.setVisible(true);
            frame.startLoop();
        });

        menu.add(p, BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout());
        south.add(hint, BorderLayout.NORTH);
        south.add(start, BorderLayout.SOUTH);
        menu.add(south, BorderLayout.SOUTH);
        menu.pack();
        menu.setLocationRelativeTo(null);
        menu.setVisible(true);
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

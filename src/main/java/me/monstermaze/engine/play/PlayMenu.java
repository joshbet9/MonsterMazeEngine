package me.monstermaze.engine.play;

import me.monstermaze.engine.api.KitType;
import me.monstermaze.engine.api.MazeMode;

import javax.swing.*;
import java.awt.*;

/** Simple start menu: pick kit, mode, layout, then launch PlayFrame. */
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
        JSpinner monsters = new JSpinner(new SpinnerNumberModel(40, 5, 150, 5));

        p.add(new JLabel("Kit"));
        p.add(kitBox);
        p.add(new JLabel("Mode"));
        p.add(modeBox);
        p.add(new JLabel("Layout"));
        p.add(layoutBox);
        p.add(new JLabel("Monsters (sim)"));
        p.add(monsters);

        JButton start = new JButton("Play");
        start.addActionListener(e -> {
            KitType kit = mapKit((String) kitBox.getSelectedItem());
            MazeMode mode = MazeMode.valueOf((String) modeBox.getSelectedItem());
            if (kit == KitType.MAVERICK && mode == MazeMode.ORIGINAL) {
                JOptionPane.showMessageDialog(menu, "Maverick requires SPEED or MODERN mode.");
                return;
            }
            int layout = Integer.parseInt((String) layoutBox.getSelectedItem());
            int mobs = (Integer) monsters.getValue();
            menu.dispose();
            PlayFrame frame = new PlayFrame(mode, kit, layout, System.currentTimeMillis(), mobs);
            frame.setVisible(true);
            frame.startLoop();
        });

        menu.add(p, BorderLayout.CENTER);
        menu.add(start, BorderLayout.SOUTH);
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

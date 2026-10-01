package me.monstermaze.engine.play;

import me.monstermaze.engine.api.*;
import me.monstermaze.engine.game.EngineImpl;
import me.monstermaze.engine.render.TopDownRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local playable Monster Maze: top-down view, WASD, Space jump,
 * Q primary ability, E enhanced ability, arrows turn.
 * Tick rate 20 Hz matching the engine.
 */
public final class PlayFrame extends JFrame {

    private final MonsterMazeEngine engine;
    private GameState state;
    private final TopDownRenderer renderer;
    private final GamePanel panel;
    private final JLabel hud;
    private final Set<Integer> keys = ConcurrentHashMap.newKeySet();
    private float yaw = 0f;
    private volatile boolean running = true;

    public PlayFrame(MazeMode mode, KitType kit, int layoutId, long seed, int monsterOverride) {
        super("Monster Maze \u2014 " + displayKit(kit));
        this.engine = new EngineImpl(monsterOverride);
        this.state = engine.initialState(mode, layoutId, kit, seed);
        this.renderer = new TopDownRenderer(4);
        this.panel = new GamePanel();
        this.hud = new JLabel(" ");
        hud.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        hud.setForeground(Color.WHITE);
        hud.setOpaque(true);
        hud.setBackground(new Color(20, 20, 28));
        hud.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(hud, BorderLayout.NORTH);
        add(panel, BorderLayout.CENTER);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);

        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { keys.add(e.getKeyCode()); }
            @Override public void keyReleased(KeyEvent e) { keys.remove(e.getKeyCode()); }
        });
        setFocusable(true);
        requestFocusInWindow();
    }

    public void startLoop() {
        Thread t = new Thread(() -> {
            final long frameNanos = 50_000_000L;
            long next = System.nanoTime();
            while (running) {
                try {
                    tickOnce();
                    long now = System.nanoTime();
                    long sleep = (next + frameNanos - now) / 1_000_000L;
                    next += frameNanos;
                    if (sleep > 0) Thread.sleep(sleep);
                    else next = now;
                } catch (InterruptedException ie) {
                    break;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    break;
                }
            }
        }, "mm-tick");
        t.setDaemon(true);
        t.start();
    }

    private void tickOnce() {
        if (state.phase == GamePhase.ENDING) {
            SwingUtilities.invokeLater(panel::repaint);
            return;
        }

        if (keys.contains(KeyEvent.VK_LEFT)) yaw -= 6f;
        if (keys.contains(KeyEvent.VK_RIGHT)) yaw += 6f;

        double moveX = 0, moveZ = 0;
        if (keys.contains(KeyEvent.VK_W) || keys.contains(KeyEvent.VK_UP)) moveZ += 1;
        if (keys.contains(KeyEvent.VK_S) || keys.contains(KeyEvent.VK_DOWN)) moveZ -= 1;
        if (keys.contains(KeyEvent.VK_A)) moveX -= 1;
        if (keys.contains(KeyEvent.VK_D)) moveX += 1;

        boolean sprint = keys.contains(KeyEvent.VK_SHIFT);
        boolean jump = keys.contains(KeyEvent.VK_SPACE);
        boolean primary = keys.contains(KeyEvent.VK_Q);
        boolean enhanced = keys.contains(KeyEvent.VK_E);

        Action action = new Action(moveX, moveZ, sprint, jump, jump, yaw, 0f, primary, enhanced, null);
        TickResult result = engine.tick(state, action);
        state = result.next;

        final String hudText = formatHud(state);
        SwingUtilities.invokeLater(() -> {
            hud.setText(hudText);
            panel.repaint();
        });
    }

    private static String formatHud(GameState s) {
        PlayerState p = s.player;
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("  Stage %d   Timer %ds   HP %.0f/%.0f   Kit %s",
                s.stage, s.phaseTimerTicks / 20, p.health, p.maxHealth, displayKit(p.kit)));
        if (p.kit == KitType.JUMPER) {
            sb.append(String.format("   Jumps %d", p.jumpCharges));
        } else if (p.kit == KitType.SLOWBALL) {
            sb.append(String.format("   Snowballs %d", p.abilityCharges));
            if (p.enhancedCooldownTicks > 0)
                sb.append(String.format("   Cryo %ds", p.enhancedCooldownTicks / 20));
        } else if (p.kit == KitType.REPULSOR) {
            sb.append(String.format("   Charges %d", p.abilityCharges));
        } else if (p.kit == KitType.BODY_BUILDER) {
            sb.append(String.format("   Rush left %d", p.abilityCharges));
            if (p.abilityCooldownTicks > 0)
                sb.append(String.format("   Rush %.1fs", p.abilityCooldownTicks / 20.0));
        }
        sb.append(String.format("   Mobs %d   Phase %s", s.monsters.size(), s.phase));
        if (s.phase == GamePhase.ENDING) {
            sb.append("   \u2014 ELIMINATED \u2014 stage reached ").append(s.stage);
        }
        if (p.onSafePad) sb.append("   [SAFE PAD]");
        return sb.toString();
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

    private final class GamePanel extends JPanel {
        GamePanel() {
            int size = 99 * 4;
            setPreferredSize(new Dimension(size, size));
            setBackground(Color.BLACK);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            BufferedImage img = renderer.render(state);
            g.drawImage(img, 0, 0, null);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MazeMode mode = MazeMode.ORIGINAL;
            KitType kit = KitType.JUMPER;
            int layout = 0;
            int monsters = 40;
            long seed = System.currentTimeMillis();

            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--mode": mode = MazeMode.valueOf(args[++i].toUpperCase()); break;
                    case "--kit": {
                        String k = args[++i].toUpperCase().replace('-', '_');
                        if (k.equals("SLOWBALLER")) k = "SLOWBALL";
                        if (k.equals("BODYBUILDER") || k.equals("BODY_BUILDER")) k = "BODY_BUILDER";
                        kit = KitType.valueOf(k);
                        break;
                    }
                    case "--layout": layout = Integer.parseInt(args[++i]); break;
                    case "--monsters": monsters = Integer.parseInt(args[++i]); break;
                    case "--seed": seed = Long.parseLong(args[++i]); break;
                }
            }

            PlayFrame frame = new PlayFrame(mode, kit, layout, seed, monsters);
            frame.setVisible(true);
            frame.startLoop();
        });
    }
}

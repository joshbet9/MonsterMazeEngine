package me.monstermaze.engine.play;

import me.monstermaze.engine.api.*;
import me.monstermaze.engine.audio.Sfx;
import me.monstermaze.engine.game.EngineImpl;
import me.monstermaze.engine.game.StageTimer;
import me.monstermaze.engine.maze.Layouts;
import me.monstermaze.engine.render.TopDownRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local playable Monster Maze (human player only).
 * AI training uses the same engine Action API from outside this UI
 * (e.g. MonsterMazeAI) — not a separate in-game mode.
 */
public final class PlayFrame extends JFrame {

    private static final int VIEW = 520;

    private final EngineImpl engine;
    private GameState state;
    private final TopDownRenderer renderer;
    private final GamePanel panel;
    private final JLabel hud;
    private final Set<Integer> keys = ConcurrentHashMap.newKeySet();
    private float yaw = 0f;
    private double zoom = 2.2;
    private volatile boolean running = true;
    private int peakStage = 0;
    private long runStartMs = System.currentTimeMillis();
    private boolean endShown = false;

    public PlayFrame(MazeMode mode, KitType kit, int layoutId, long seed, int monsterOverride) {
        super("Monster Maze \u2014 " + displayKit(kit));
        int monsters = monsterOverride >= 0 ? monsterOverride : StageTimer.starterMonsters(mode);
        this.engine = new EngineImpl(monsters);
        this.state = engine.initialState(mode, layoutId, kit, seed);
        this.renderer = new TopDownRenderer(6);
        this.panel = new GamePanel();
        this.hud = new JLabel(" ");
        hud.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        hud.setForeground(Color.WHITE);
        hud.setOpaque(true);
        hud.setBackground(new Color(18, 18, 26));
        hud.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(hud, BorderLayout.NORTH);
        add(panel, BorderLayout.CENTER);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);

        addKeyListener(new KeyAdapter() {
            private boolean sfxOn = true;
            @Override public void keyPressed(KeyEvent e) {
                keys.add(e.getKeyCode());
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_EQUALS || code == KeyEvent.VK_ADD) zoom = Math.min(6.0, zoom + 0.25);
                else if (code == KeyEvent.VK_MINUS || code == KeyEvent.VK_SUBTRACT) zoom = Math.max(0.8, zoom - 0.25);
                else if (code == KeyEvent.VK_0) zoom = 2.2;
                else if (code == KeyEvent.VK_M) { sfxOn = !sfxOn; Sfx.setEnabled(sfxOn); }
            }
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
                } catch (InterruptedException ie) { break;
                } catch (Exception ex) { ex.printStackTrace(); break; }
            }
        }, "mm-tick");
        t.setDaemon(true);
        t.start();
    }

    private void tickOnce() {
        if (state.phase == GamePhase.ENDING) {
            if (!endShown) {
                endShown = true;
                final int stage = Math.max(peakStage, state.stage);
                final long secs = (System.currentTimeMillis() - runStartMs) / 1000;
                final int mobs = state.monsters.size();
                SwingUtilities.invokeLater(() -> {
                    panel.repaint();
                    JOptionPane.showMessageDialog(this,
                            "Eliminated!\nPeak stage: " + stage + "\nTime: " + secs + "s\nMobs at end: " + mobs,
                            "Run over", JOptionPane.INFORMATION_MESSAGE);
                });
            }
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

        Action action = new Action(moveX, moveZ,
                keys.contains(KeyEvent.VK_SHIFT),
                keys.contains(KeyEvent.VK_SPACE),
                keys.contains(KeyEvent.VK_SPACE),
                yaw, 0f,
                keys.contains(KeyEvent.VK_Q),
                keys.contains(KeyEvent.VK_E),
                null);

        TickResult result = engine.tick(state, action);
        Sfx.playEvents(result.events);
        state = result.next;
        if (state.stage > peakStage) peakStage = state.stage;

        final String hudText = formatHud(state);
        SwingUtilities.invokeLater(() -> { hud.setText(hudText); panel.repaint(); });
    }

    private String formatHud(GameState s) {
        PlayerState p = s.player;
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("  Stage %d (peak %d)  Timer %ds  HP %.0f/%.0f  %s",
                s.stage, peakStage, s.phaseTimerTicks / 20, p.health, p.maxHealth, displayKit(p.kit)));
        if (p.kit == KitType.JUMPER) sb.append("  Jumps ").append(p.jumpCharges);
        else if (p.kit == KitType.SLOWBALL) {
            sb.append("  Balls ").append(p.abilityCharges);
            if (p.enhancedCooldownTicks > 0) sb.append("  Cryo ").append(p.enhancedCooldownTicks / 20).append('s');
        } else if (p.kit == KitType.REPULSOR) sb.append("  Charges ").append(p.abilityCharges);
        else if (p.kit == KitType.BODY_BUILDER) {
            sb.append("  Rush ").append(p.abilityCharges);
            if (p.abilityCooldownTicks > 0) sb.append(String.format("  %.1fs", p.abilityCooldownTicks / 20.0));
        }
        sb.append(String.format("  Mobs %d  %s  zoom %.1fx", s.monsters.size(), s.phase, zoom));
        if (p.onSafePad) sb.append("  [PAD]");
        sb.append("   +/- zoom  M=mute");
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
            setPreferredSize(new Dimension(VIEW, VIEW));
            setBackground(Color.BLACK);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            BufferedImage img = renderer.renderCamera(state, getSize(), zoom);
            g.drawImage(img, 0, 0, null);
            drawMinimap((Graphics2D) g);
        }

        private void drawMinimap(Graphics2D g) {
            int ms = 110;
            int ox = getWidth() - ms - 10;
            int oy = 10;
            g.setColor(new Color(0, 0, 0, 160));
            g.fillRoundRect(ox - 2, oy - 2, ms + 4, ms + 4, 6, 6);
            double scale = ms / (double) Layouts.SIZE;
            boolean[][] trav = state.maze.traversable;
            for (int r = 0; r < Layouts.SIZE; r += 2) {
                for (int c = 0; c < Layouts.SIZE; c += 2) {
                    if (trav[r][c]) {
                        g.setColor(new Color(70, 70, 80));
                        g.fillRect(ox + (int) (c * scale), oy + (int) (r * scale), 2, 2);
                    }
                }
            }
            if (state.activePad != null) {
                int pr = state.activePad.centerX + Layouts.HALF;
                int pc = state.activePad.centerZ + Layouts.HALF;
                g.setColor(new Color(80, 255, 120));
                g.fillRect(ox + (int) (pc * scale) - 2, oy + (int) (pr * scale) - 2, 5, 5);
            }
            int pr = (int) Math.floor(state.player.pos.x) + Layouts.HALF;
            int pc = (int) Math.floor(state.player.pos.z) + Layouts.HALF;
            g.setColor(new Color(100, 200, 255));
            g.fillOval(ox + (int) (pc * scale) - 2, oy + (int) (pr * scale) - 2, 5, 5);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MazeMode mode = MazeMode.ORIGINAL;
            KitType kit = KitType.JUMPER;
            int layout = 0;
            int monsters = -1;
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

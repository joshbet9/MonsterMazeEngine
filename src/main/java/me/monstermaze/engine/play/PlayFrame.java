package me.monstermaze.engine.play;

import me.monstermaze.engine.ai.HeuristicAgent;
import me.monstermaze.engine.api.*;
import me.monstermaze.engine.audio.Sfx;
import me.monstermaze.engine.game.EngineImpl;
import me.monstermaze.engine.game.StageTimer;
import me.monstermaze.engine.render.TopDownRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Playable Monster Maze with camera follow, zoom, AI mode, SFX.
 * Controls: WASD, arrows, Space, Shift, Q/E, +/- zoom, F AI, M mute.
 */
public final class PlayFrame extends JFrame {

    private static final int VIEW = 520;

    private final MonsterMazeEngine engine;
    private GameState state;
    private final TopDownRenderer renderer;
    private final GamePanel panel;
    private final JLabel hud;
    private final Set<Integer> keys = ConcurrentHashMap.newKeySet();
    private final HeuristicAgent agent = new HeuristicAgent();
    private float yaw = 0f;
    private double zoom = 2.2;
    private volatile boolean running = true;
    private volatile boolean aiEnabled = false;

    /** @param monsterOverride -1 = mode default (150 Original/Speed, 225 Modern) */
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
                else if (code == KeyEvent.VK_F) aiEnabled = !aiEnabled;
                else if (code == KeyEvent.VK_M) { sfxOn = !sfxOn; Sfx.setEnabled(sfxOn); }
            }
            @Override public void keyReleased(KeyEvent e) { keys.remove(e.getKeyCode()); }
        });
        setFocusable(true);
        requestFocusInWindow();
    }

    public void setAiEnabled(boolean on) { this.aiEnabled = on; }

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
            SwingUtilities.invokeLater(panel::repaint);
            return;
        }

        Action action;
        if (aiEnabled) {
            action = agent.act(state);
            yaw = action.yaw;
        } else {
            if (keys.contains(KeyEvent.VK_LEFT)) yaw -= 6f;
            if (keys.contains(KeyEvent.VK_RIGHT)) yaw += 6f;
            double moveX = 0, moveZ = 0;
            if (keys.contains(KeyEvent.VK_W) || keys.contains(KeyEvent.VK_UP)) moveZ += 1;
            if (keys.contains(KeyEvent.VK_S) || keys.contains(KeyEvent.VK_DOWN)) moveZ -= 1;
            if (keys.contains(KeyEvent.VK_A)) moveX -= 1;
            if (keys.contains(KeyEvent.VK_D)) moveX += 1;
            action = new Action(moveX, moveZ,
                    keys.contains(KeyEvent.VK_SHIFT),
                    keys.contains(KeyEvent.VK_SPACE),
                    keys.contains(KeyEvent.VK_SPACE),
                    yaw, 0f,
                    keys.contains(KeyEvent.VK_Q),
                    keys.contains(KeyEvent.VK_E),
                    null);
        }

        TickResult result = engine.tick(state, action);
        Sfx.playEvents(result.events);
        state = result.next;

        final String hudText = formatHud(state);
        SwingUtilities.invokeLater(() -> { hud.setText(hudText); panel.repaint(); });
    }

    private String formatHud(GameState s) {
        PlayerState p = s.player;
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("  Stage %d  Timer %ds  HP %.0f/%.0f  %s",
                s.stage, s.phaseTimerTicks / 20, p.health, p.maxHealth, displayKit(p.kit)));
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
        if (aiEnabled) sb.append("  [AI]");
        if (s.phase == GamePhase.ENDING) sb.append("  \u2014 ELIMINATED \u2014 stage ").append(s.stage);
        if (p.onSafePad) sb.append("  [PAD]");
        sb.append("   F=AI  +/-=zoom  M=mute");
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
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MazeMode mode = MazeMode.ORIGINAL;
            KitType kit = KitType.JUMPER;
            int layout = 0;
            int monsters = -1;
            long seed = System.currentTimeMillis();
            boolean startAi = false;

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
                    case "--ai": startAi = true; break;
                }
            }

            PlayFrame frame = new PlayFrame(mode, kit, layout, seed, monsters);
            if (startAi) frame.setAiEnabled(true);
            frame.setVisible(true);
            frame.startLoop();
        });
    }
}

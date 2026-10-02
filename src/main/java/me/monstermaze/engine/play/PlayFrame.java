package me.monstermaze.engine.play;

import me.monstermaze.engine.api.*;
import me.monstermaze.engine.audio.Sfx;
import me.monstermaze.engine.game.EngineImpl;
import me.monstermaze.engine.game.StageTimer;
import me.monstermaze.engine.maze.Layouts;
import me.monstermaze.engine.render.PerspectiveRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** In-game: dark HUD, HP bar, pause, controls help, minimap. */
public final class PlayFrame extends JFrame {

    private final EngineImpl engine;
    private GameState state;
    private final PerspectiveRenderer renderer;
    private final GamePanel panel;
    private final JPanel hudBar;
    private final JLabel hudLine1;
    private final JLabel hudLine2;
    private final JProgressBar hpBar;
    private final Set<Integer> keys = ConcurrentHashMap.newKeySet();
    private final GameConfig config;
    private final MainMenu mainMenu;
    private final UiTheme theme;
    private double zoom;
    private boolean firstPerson;
    private volatile float cameraYaw;
    private float cameraPitch = -18.0f;
    private double cameraDistance = 9.0;
    private int lastMouseX;
    private int lastMouseY;
    private boolean dragging;
    private final int viewSize;
    private volatile boolean running = true;
    private volatile boolean paused = false;
    private boolean showHelp;
    private int peakStage = 0;
    private long runStartMs = System.currentTimeMillis();
    private boolean endShown = false;
    private boolean abilityHeld = false;

    public PlayFrame(MazeMode mode, KitType kit, int layoutId, long seed, int monsterOverride) {
        this(mode, kit, layoutId, seed, monsterOverride, null, null);
    }

    public PlayFrame(MazeMode mode, KitType kit, int layoutId, long seed, int monsterOverride,
                     GameConfig config, MainMenu mainMenu) {
        super("Monster Maze \u2014 " + displayKit(kit));
        this.config = config != null ? config : GameConfig.load();
        this.mainMenu = mainMenu;
        this.theme = UiTheme.of(this.config.darkMode);
        this.zoom = this.config.zoom;
        this.cameraDistance = 9.0 / Math.max(0.8, Math.min(6.0, this.zoom));
        this.viewSize = this.config.windowSize;
        this.showHelp = this.config.showControlsHint;
        Sfx.setEnabled(this.config.sfxEnabled);
        theme.applyLookAndFeel();

        int monsters = monsterOverride >= 0 ? monsterOverride : StageTimer.starterMonsters(mode);
        this.engine = new EngineImpl(monsters);
        this.state = engine.initialState(mode, layoutId, kit, seed);
        this.renderer = new PerspectiveRenderer();
        this.cameraYaw = state.player.yaw;
        this.panel = new GamePanel();

        hudLine1 = new JLabel(" ");
        hudLine2 = new JLabel(" ");
        hudLine1.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        hudLine2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        hudLine1.setForeground(theme.text);
        hudLine2.setForeground(theme.textMuted);

        hpBar = new JProgressBar(0, 100);
        hpBar.setValue(100);
        hpBar.setStringPainted(true);
        hpBar.setString("HP");
        hpBar.setForeground(theme.success);
        hpBar.setBackground(theme.bgPanel);
        hpBar.setPreferredSize(new Dimension(120, 18));

        hudBar = new JPanel(new BorderLayout(10, 4));
        hudBar.setBackground(theme.bgRaised);
        hudBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, theme.border),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        JPanel hudText = new JPanel();
        hudText.setLayout(new BoxLayout(hudText, BoxLayout.Y_AXIS));
        hudText.setOpaque(false);
        hudText.add(hudLine1);
        hudText.add(hudLine2);
        hudBar.add(hudText, BorderLayout.CENTER);
        hudBar.add(hpBar, BorderLayout.EAST);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        add(hudBar, BorderLayout.NORTH);
        add(panel, BorderLayout.CENTER);
        getContentPane().setBackground(theme.bg);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                running = false;
                if (mainMenu != null) mainMenu.returnFromGame();
            }
        });


        panel.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                dragging = true;
                lastMouseX = e.getX();
                lastMouseY = e.getY();
            }
            @Override public void mouseReleased(MouseEvent e) {
                dragging = false;
            }
        });
        panel.addMouseMotionListener(new MouseMotionAdapter() {
            private void updateCamera(MouseEvent e) {
                int dx = e.getX() - lastMouseX;
                int dy = e.getY() - lastMouseY;
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                if (dx == 0 && dy == 0) return;
                // Minecraft-style free look: ordinary mouse movement changes
                // the camera. No click-and-drag gesture is required.
                cameraYaw = normaliseYaw(cameraYaw + dx * 0.45f);
                cameraPitch = clamp(cameraPitch - dy * 0.30f, -70.0f, 30.0f);
                panel.repaint();
            }
            @Override public void mouseMoved(MouseEvent e) {
                updateCamera(e);
            }
            @Override public void mouseDragged(MouseEvent e) {
                updateCamera(e);
            }
        });
        panel.addMouseWheelListener(e -> {
            cameraDistance = clamp(cameraDistance + e.getPreciseWheelRotation(), 4.0, 18.0);
            panel.repaint();
        });

        installKeyBindings();
        panel.setFocusable(true);
        panel.setFocusTraversalKeysEnabled(false);
        setFocusable(true);
        SwingUtilities.invokeLater(() -> {
            toFront();
            requestFocus();
            panel.requestFocusInWindow();
        });
    }

    public void startLoop() {
        Thread t = new Thread(() -> {
            final long frameNanos = 50_000_000L;
            long next = System.nanoTime();
            while (running) {
                try {
                    if (!paused) tickOnce();
                    else SwingUtilities.invokeLater(panel::repaint);
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

    /**
     * Use Swing's window-scoped key bindings rather than a JFrame KeyListener.
     * A JFrame is not normally the focused component after the game panel is
     * clicked, so the old listener could silently stop receiving W/A/S/D.
     */
    /**
     * Track physical keyboard state at the AWT level. Unlike Swing key bindings,
     * this receives both press and release events for modifier keys such as Shift
     * regardless of which child component currently owns focus.
     */
    private void installKeyBindings() {
        final java.awt.KeyEventDispatcher dispatcher = event -> {
            Component source = event.getComponent();
            if (source == null || SwingUtilities.getWindowAncestor(source) != PlayFrame.this) {
                return false;
            }

            int code = event.getKeyCode();
            if (event.getID() == KeyEvent.KEY_PRESSED) {
                keys.add(code);
            } else if (event.getID() == KeyEvent.KEY_RELEASED) {
                keys.remove(code);
            }
            return false;
        };
        java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(dispatcher);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager()
                        .removeKeyEventDispatcher(dispatcher);
                keys.clear();
            }
        });
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
                            "Eliminated!\nPeak stage: " + stage + "\nTime: " + secs + "s\nMobs: " + mobs
                                    + "\n\nEsc \u2192 menu",
                            "Run over", JOptionPane.INFORMATION_MESSAGE);
                });
            }
            SwingUtilities.invokeLater(panel::repaint);
            return;
        }

        if (keys.contains(KeyEvent.VK_LEFT)) cameraYaw = normaliseYaw(cameraYaw - 6.0f);
        if (keys.contains(KeyEvent.VK_RIGHT)) cameraYaw = normaliseYaw(cameraYaw + 6.0f);

        // The rendered camera and the player's horizontal look direction share
        // one heading. This makes W/A/S/D evaluate relative to the camera.
        state.player.yaw = cameraYaw;
        float yawDelta = 0.0f;

        double strafe = 0.0, forward = 0.0;
        if (keys.contains(KeyEvent.VK_W) || keys.contains(KeyEvent.VK_UP)) forward += 1.0;
        if (keys.contains(KeyEvent.VK_S) || keys.contains(KeyEvent.VK_DOWN)) forward -= 1.0;
        if (keys.contains(KeyEvent.VK_A)) strafe -= 1.0;
        if (keys.contains(KeyEvent.VK_D)) strafe += 1.0;

        // Keep jump input available to every kit. The engine's Jump Boost -10
        // state suppresses vertical motion for jumpless/exhausted kits while
        // preserving the sprint horizontal impulse used for speeding.
        boolean jump = keys.contains(KeyEvent.VK_SPACE);
        boolean abilityKeyDown = keys.contains(KeyEvent.VK_Q) || keys.contains(KeyEvent.VK_E);
        boolean ability = abilityKeyDown && !abilityHeld;
        abilityHeld = abilityKeyDown;
        me.monstermaze.engine.api.Action action = new me.monstermaze.engine.api.Action(
                forward, strafe,
                jump,
                keys.contains(KeyEvent.VK_SHIFT),
                yawDelta,
                ability
        );

        TickResult result = engine.tick(state, action);
        Sfx.playEvents(result.events);
        state = result.next;
        cameraYaw = state.player.yaw;
        if (state.stage > peakStage) peakStage = state.stage;

        final String l1 = formatLine1(state);
        final String l2 = formatLine2(state);
        final int hpPct = (int) Math.round(100.0 * state.player.health / Math.max(1.0, state.player.maxHealth));
        SwingUtilities.invokeLater(() -> {
            hudLine1.setText(l1);
            hudLine2.setText(l2);
            hpBar.setValue(Math.max(0, Math.min(100, hpPct)));
            hpBar.setString(String.format("HP %.0f", state.player.health));
            hpBar.setForeground(hpPct > 40 ? theme.success : theme.danger);
            panel.repaint();
        });
    }

    private String formatLine1(GameState s) {
        return String.format("  Stage %d  \u00b7  peak %d  \u00b7  %ds  \u00b7  %s  \u00b7  %s",
                s.stage, peakStage, s.phaseTimerTicks / 20, displayKit(s.player.kit), s.phase,
                firstPerson ? "1P" : "3P");
    }

    private String formatLine2(GameState s) {
        PlayerState p = s.player;
        StringBuilder sb = new StringBuilder("  ");
        if (p.kit == KitType.JUMPER) sb.append("Jumps ").append(p.jumpCharges);
        else if (p.kit == KitType.SLOWBALL) {
            sb.append("Balls ").append(p.abilityCharges);
            long cooldown = Math.max(0L, p.abilityCooldownUntilTick - s.tick);
            if (cooldown > 0) sb.append("  Cryo ").append(cooldown / 20).append('s');
        } else if (p.kit == KitType.REPULSOR) sb.append("Charges ").append(p.abilityCharges);
        else if (p.kit == KitType.BODY_BUILDER) {
            sb.append("Rush ").append(p.abilityActivations);
            long remaining = Math.max(0L, p.abilityActiveUntilTick - s.tick);
            if (remaining > 0) sb.append(String.format("  %.1fs", remaining / 20.0));
        } else sb.append("Maverick");
        sb.append("  \u00b7  Mobs ").append(s.monsters.size());
        sb.append(String.format("  \u00b7  zoom %.1fx", zoom));
        if (p.onSafePad) sb.append("  \u00b7  ON PAD");
        if (paused) sb.append("  \u00b7  PAUSED");
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
            setPreferredSize(new Dimension(viewSize, viewSize));
            setBackground(theme.bg);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            PerspectiveRenderer.BufferedFrame frame =
                    renderer.render(state, getSize(), firstPerson, cameraYaw, cameraPitch, cameraDistance);
            g.drawImage(frame.image(), 0, 0, null);
            drawMinimap(g);
            if (showHelp) drawHelp(g);
            if (paused) drawPause(g);
            if (state.phase == GamePhase.ENDING) drawEliminated(g);
        }

        private void drawMinimap(Graphics2D g) {
            int ms = 120;
            int ox = getWidth() - ms - 12;
            int oy = 12;
            g.setColor(new Color(0, 0, 0, 170));
            g.fillRoundRect(ox - 4, oy - 4, ms + 8, ms + 8, 8, 8);
            g.setColor(theme.border);
            g.drawRoundRect(ox - 4, oy - 4, ms + 8, ms + 8, 8, 8);
            double scale = ms / (double) Layouts.SIZE;
            boolean[][] trav = state.maze.traversable;
            for (int r = 0; r < Layouts.SIZE; r += 2) {
                for (int c = 0; c < Layouts.SIZE; c += 2) {
                    if (trav[r][c]) {
                        g.setColor(new Color(70, 74, 90));
                        g.fillRect(ox + (int) (c * scale), oy + (int) (r * scale), 2, 2);
                    }
                }
            }
            if (state.previewPad != null) {
                int pr = state.previewPad.centerX + Layouts.HALF;
                int pc = state.previewPad.centerZ + Layouts.HALF;
                g.setColor(new Color(255, 220, 80));
                g.fillRect(ox + (int) (pc * scale) - 2, oy + (int) (pr * scale) - 2, 5, 5);
            }
            if (state.activePad != null) {
                int pr = state.activePad.centerX + Layouts.HALF;
                int pc = state.activePad.centerZ + Layouts.HALF;
                g.setColor(theme.success);
                g.fillRect(ox + (int) (pc * scale) - 2, oy + (int) (pr * scale) - 2, 5, 5);
            }
            int pr = (int) Math.floor(state.player.pos.x) + Layouts.HALF;
            int pc = (int) Math.floor(state.player.pos.z) + Layouts.HALF;
            g.setColor(theme.accent);
            g.fillOval(ox + (int) (pc * scale) - 3, oy + (int) (pr * scale) - 3, 6, 6);
        }

        private void drawHelp(Graphics2D g) {
            String[] lines = {
                    "WASD move   Arrows turn   Space jump   Shift sprint",
                    "Q primary   E enhanced   P pause   F first-person",
                    "Move mouse camera   Wheel/[ ] distance   H help   Esc menu"
            };
            int pad = 10, lineH = 16, w = 420;
            int h = lines.length * lineH + pad * 2;
            int x = 12, y = getHeight() - h - 12;
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRoundRect(x, y, w, h, 8, 8);
            g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            g.setColor(theme.text);
            for (int i = 0; i < lines.length; i++) {
                g.drawString(lines[i], x + pad, y + pad + (i + 1) * lineH - 4);
            }
        }

        private void drawPause(Graphics2D g) {
            g.setColor(new Color(0, 0, 0, 140));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 36));
            g.setColor(theme.text);
            String msg = "PAUSED";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2 - 10);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            g.setColor(theme.textMuted);
            String sub = "P resume   \u00b7   Esc main menu";
            fm = g.getFontMetrics();
            g.drawString(sub, (getWidth() - fm.stringWidth(sub)) / 2, getHeight() / 2 + 24);
        }

        private void drawEliminated(Graphics2D g) {
            g.setColor(new Color(40, 0, 0, 120));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
            g.setColor(theme.danger);
            String msg = "ELIMINATED";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
        }
    }

    private static float normaliseYaw(float yaw) {
        while (yaw >= 180.0f) yaw -= 360.0f;
        while (yaw < -180.0f) yaw += 360.0f;
        return yaw;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameConfig cfg = GameConfig.load();
            MazeMode mode = cfg.mazeMode();
            KitType kit = cfg.kitType();
            int layout = cfg.layout;
            int monsters = cfg.monsterCount();
            long seed = cfg.seedOrRandom();
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
            PlayFrame frame = new PlayFrame(mode, kit, layout, seed, monsters, cfg, null);
            frame.setVisible(true);
            frame.startLoop();
        });
    }
}

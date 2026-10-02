package me.monstermaze.engine.play;

import me.monstermaze.engine.api.KitType;
import me.monstermaze.engine.api.MazeMode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Persistent player settings. */
public final class GameConfig {

    private static final Path PATH = Path.of(System.getProperty("user.home"), ".monstermaze-engine.properties");

    public String kit = "JUMPER";
    public String mode = "ORIGINAL";
    public int layout = 0;
    public boolean useDefaultMobs = true;
    public int monsters = 150;
    public double zoom = 2.2;
    public boolean sfxEnabled = true;
    public int windowSize = 900;
    public long lastSeed = 0;
    public boolean darkMode = true;
    public boolean showControlsHint = true;

    public static GameConfig load() {
        GameConfig c = new GameConfig();
        if (!Files.isRegularFile(PATH)) return c;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(PATH)) {
            p.load(in);
            c.kit = p.getProperty("kit", c.kit);
            c.mode = p.getProperty("mode", c.mode);
            c.layout = Integer.parseInt(p.getProperty("layout", "0"));
            c.useDefaultMobs = Boolean.parseBoolean(p.getProperty("useDefaultMobs", "true"));
            c.monsters = Integer.parseInt(p.getProperty("monsters", "150"));
            c.zoom = Double.parseDouble(p.getProperty("zoom", "2.2"));
            c.sfxEnabled = Boolean.parseBoolean(p.getProperty("sfxEnabled", "true"));
            c.windowSize = Math.max(900, Integer.parseInt(p.getProperty("windowSize", "900")));
            c.lastSeed = Long.parseLong(p.getProperty("lastSeed", "0"));
            c.darkMode = Boolean.parseBoolean(p.getProperty("darkMode", "true"));
            c.showControlsHint = Boolean.parseBoolean(p.getProperty("showControlsHint", "true"));
        } catch (Exception ignored) {
        }
        return c;
    }

    public void save() {
        Properties p = new Properties();
        p.setProperty("kit", kit);
        p.setProperty("mode", mode);
        p.setProperty("layout", String.valueOf(layout));
        p.setProperty("useDefaultMobs", String.valueOf(useDefaultMobs));
        p.setProperty("monsters", String.valueOf(monsters));
        p.setProperty("zoom", String.valueOf(zoom));
        p.setProperty("sfxEnabled", String.valueOf(sfxEnabled));
        p.setProperty("windowSize", String.valueOf(windowSize));
        p.setProperty("lastSeed", String.valueOf(lastSeed));
        p.setProperty("darkMode", String.valueOf(darkMode));
        p.setProperty("showControlsHint", String.valueOf(showControlsHint));
        try (OutputStream out = Files.newOutputStream(PATH)) {
            p.store(out, "MonsterMazeEngine player config");
        } catch (IOException ignored) {
        }
    }

    public KitType kitType() {
        String k = kit.toUpperCase().replace('-', '_');
        if (k.equals("SLOWBALLER")) k = "SLOWBALL";
        if (k.equals("BODYBUILDER") || k.equals("BODY_BUILDER")) k = "BODY_BUILDER";
        try { return KitType.valueOf(k); } catch (Exception e) { return KitType.JUMPER; }
    }

    public MazeMode mazeMode() {
        try { return MazeMode.valueOf(mode.toUpperCase()); } catch (Exception e) { return MazeMode.ORIGINAL; }
    }

    public int monsterCount() { return useDefaultMobs ? -1 : monsters; }

    public long seedOrRandom() { return lastSeed != 0 ? lastSeed : System.currentTimeMillis(); }

    public static Path configPath() { return PATH; }
}

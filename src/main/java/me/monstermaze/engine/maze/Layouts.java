package me.monstermaze.engine.maze;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

/**
 * Ripped maze layouts from original Mineplex Monster Maze (via MonsterMaze 1.8 plugin).
 * Cell values: 0 empty; 1 path; 2 path+spawn; 3/4 center; 5/6 center+path; 4/6 barrier.
 * Layout rows are stored gzip+base64 to keep the class small.
 */
public final class Layouts {

    public static final int SIZE = 99;
    public static final int HALF = 49;
    public static final int LAYOUT_COUNT = 3;

    public static final int[][][] ALL;

    private static final String[] PACKED = {
        "H4sIAFfjvmoC/71aWZbiMAz8z2kkE7j/0abJYleVlA7Ma4sH3STEKqu0WF7Mvny1bxvY8nULn4vROkabgtFO8f1P+3MMDxj+Bxgt6BAx2ieusNwwQzpEDH6mfYXhZNztKr7iMx9jJMwMifjvuHvF3m8Yl8y8Re5fN+F37F1iNMU4afFD9nHtJyoqc3zaDQa17Z0lyS53QgP/AOPoGbQcyBtGotlo8AHG8RSI7mIPLbx33IYeo0EHvOOK9Ee3Ig7Hr70DN1w1cf4hCUIEuOJfXUKoXenh3Q+pd3AhX9yji5xSfsFAfzIOs5Pq5C4+f4nRpF/KgAHVTumFrkHvluqBdlPLdIpscJU/Gcy+JMExHB60yW2OKcVc7JJhkAtmChA9iTro9BGjEedu9AZLOnKq6iBRaJGFvQq4lYSEtqcMY+NRJ4/OucLOEGVOyXh8nEkSCXcYZ7/YK03zCkYF8JlhNNC225P9xgNXnvgdInaDLCE8QI45DxQ81MFvkDfBUFe+i/kcR1HNUkZVA7Vwy30XegZjA3mWhegRr5JQusYQrsw07jFMNDdbnhQXTYmQ39M8IQVWLE9Gv1rUo7se+pfxwKCGGjbnbECaLJGpYdM+zFEAc0Cb8eMe2VqyEAR+TOtDSFyUS6gauMbgNERp5QaDW7gk3kXTrl+VhkntkxeNrol3kaRrTvp7nlEkizjxOiQpV9BvDtdQmlha9mrSsiuMWJSlFZpWebHkCxgtG/UpdajU9L5lFUcDPWSECWJoPJHuApCMcswVY3QokGBhmMe8YOIHOUaWjjSo19e6v0PoZ80Bo2UVGLvZ2cv1+Xi83ytUjeRIsQpsqodgjN5sl+u6PvfXzzckTGAwXMQeWUzTzdeP9F2P5/OlYZ7HPmOQ8/XBjYro9ZD/2NmiX50zO82nllDq4ryF4+aN8dzeHUNKXJmjnBhNp7Cxlj6bvbnalBhccUbHKQ5YvUSPYntM96u58VEc59PyVWHenTt+VI2Ds8fzwrpkcn1VVCdOr3fr6vai+cfUeVTRfHD6vLZqfl6xzjB3vaRy3Wfm+lXpOtzM9cS6ddG567tl69TT19ur9g1m73/U7OMU7UdN31cr3R+cvs9ZsF9bse9csX9edA6g4jxD1bmMivMlVedk6s/7VJxbqjp/9b/nyP4BZ/tBMawmAAA=",
        "H4sIAFfjvmoC/71a27LCMAh892vY6On/f9qx2ia7QOtlDB2dVicNAZaFXMzWq9mMa+v18nzE78WMXlcZ2D/txwKen7uMhu16/vGba+1su5rq8VsZpMd6H6q89mD6K8jYO3zIaGQnnL6LvevWfx2OBWSvtulh3ictdeIYSgaR5n1hUFuxAdsYZhMDq4ykTRNf7LZ64kp9QkiGIlEwmLdhXwxcdU1IOI+bBDwNayxG29hw7qaFDRl9CJa/P/5ViWEc1o1BMlpHGVTbbv8OawjIybT8Tu9oC8FNj70JO1UxrgHEHapYHtbe+kLwZ6cCiVLdlrBk6O7NEUBehsISDoqZz4fi2kGQMYiRhkJARupzMFzZuJ0OnR62g4EHzYoP5IzhUxtiJCjXsAx0WlE7qNFNXRXsaR2iiQxqM+wAp5BCw2JTisIgo7EtiSjN+RbyzOqMUBJvqB57aILpxSPLERZ6wAikhPm9DASCtUAeiKkTSju5jMZUKSQShh6VcpRCNJLbStiBiGUQhHZqLs59AB7I0GcoE44bsxn1eyaj6SjHaJUqoe7qbfglhZXq"
    };

    static {
        ALL = new int[LAYOUT_COUNT][][];
        for (int i = 0; i < LAYOUT_COUNT; i++) {
            ALL[i] = parse(unpack(PACKED[i]));
        }
    }

    private static String unpack(String b64) {
        try {
            byte[] gz = Base64.getDecoder().decode(b64);
            try (GZIPInputStream gin = new GZIPInputStream(new ByteArrayInputStream(gz));
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = gin.read(buf)) >= 0) {
                    out.write(buf, 0, n);
                }
                return out.toString(StandardCharsets.UTF_8.name());
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static int[][] parse(String text) {
        String[] rows = text.trim().split("\\R");
        if (rows.length != SIZE) {
            throw new ExceptionInInitializerError("expected " + SIZE + " rows, got " + rows.length);
        }
        int[][] maze = new int[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) {
            String row = rows[r].trim();
            if (row.length() != SIZE) {
                throw new ExceptionInInitializerError("row " + r + " length " + row.length());
            }
            for (int c = 0; c < SIZE; c++) {
                maze[r][c] = row.charAt(c) - '0';
            }
        }
        return maze;
    }

    public static boolean isRawPath(int cell) {
        return cell == 1 || cell == 2 || cell == 5 || cell == 6;
    }

    public static boolean isSpawn(int cell) {
        return cell == 2;
    }

    public static boolean isCenter(int cell) {
        return cell >= 3 && cell <= 6;
    }

    public static boolean isBarrier(int cell) {
        return cell == 4 || cell == 6;
    }

    public static int[][] copy(int layoutId) {
        if (layoutId < 0 || layoutId >= ALL.length) {
            throw new IllegalArgumentException("layoutId must be 0.." + (ALL.length - 1));
        }
        int[][] src = ALL[layoutId];
        int[][] dst = new int[src.length][];
        for (int i = 0; i < src.length; i++) {
            dst[i] = src[i].clone();
        }
        return dst;
    }

    private Layouts() {}
}

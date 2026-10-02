package me.monstermaze.engine.play;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

/** Dark (default) / light UI palette + Swing defaults. */
public final class UiTheme {

    public final boolean dark;
    public final Color bg;
    public final Color bgRaised;
    public final Color bgPanel;
    public final Color text;
    public final Color textMuted;
    public final Color accent;
    public final Color danger;
    public final Color success;
    public final Color border;

    private UiTheme(boolean dark) {
        this.dark = dark;
        if (dark) {
            bg = new Color(18, 18, 24);
            bgRaised = new Color(28, 28, 38);
            bgPanel = new Color(24, 24, 34);
            text = new Color(230, 232, 240);
            textMuted = new Color(150, 154, 170);
            accent = new Color(90, 190, 255);
            danger = new Color(240, 90, 90);
            success = new Color(90, 220, 140);
            border = new Color(50, 52, 68);
        } else {
            bg = new Color(242, 243, 248);
            bgRaised = Color.WHITE;
            bgPanel = new Color(232, 234, 240);
            text = new Color(28, 30, 40);
            textMuted = new Color(100, 104, 120);
            accent = new Color(30, 120, 210);
            danger = new Color(190, 40, 40);
            success = new Color(30, 150, 80);
            border = new Color(190, 192, 205);
        }
    }

    public static UiTheme of(boolean dark) {
        return new UiTheme(dark);
    }

    public void applyLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        if (dark) {
            UIManager.put("Panel.background", bg);
            UIManager.put("OptionPane.background", bgRaised);
            UIManager.put("OptionPane.messageForeground", text);
            UIManager.put("Label.foreground", text);
            UIManager.put("Button.background", bgRaised);
            UIManager.put("Button.foreground", text);
            UIManager.put("CheckBox.background", bgRaised);
            UIManager.put("CheckBox.foreground", text);
            UIManager.put("ComboBox.background", bgRaised);
            UIManager.put("ComboBox.foreground", text);
            UIManager.put("TextField.background", bgPanel);
            UIManager.put("TextField.foreground", text);
            UIManager.put("TextField.caretForeground", text);
            UIManager.put("TitledBorder.titleColor", accent);
            UIManager.put("ToolTip.background", bgRaised);
            UIManager.put("ToolTip.foreground", text);
        }
    }

    public JButton primaryButton(String label) {
        JButton b = new JButton(label);
        b.setFocusPainted(false);
        b.setBackground(accent);
        b.setForeground(dark ? new Color(12, 20, 30) : Color.WHITE);
        b.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(accent.darker(), 1, true),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public JButton secondaryButton(String label) {
        JButton b = new JButton(label);
        b.setFocusPainted(false);
        b.setBackground(bgRaised);
        b.setForeground(text);
        b.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        b.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(border, 1, true),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}

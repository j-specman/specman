package specman.styles;

import net.atlanticbb.tantlinger.ui.text.actions.SpecmanFontSizeAction;
import specman.Specman;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class Styles {
    public static final int FONTSIZE = SpecmanFontSizeAction.DEFAULT_FONTSIZE;
    public static final int SCHRITTNR_FONTSIZE = 10;

    public static MutableAttributeSet geloeschtStil = new SimpleAttributeSet();
    public static MutableAttributeSet ganzerSchrittGeloeschtStil = new SimpleAttributeSet();
    public static MutableAttributeSet standardTextBackground = new SimpleAttributeSet();
    public static MutableAttributeSet quellschrittStil = new SimpleAttributeSet();
    public static MutableAttributeSet deletedStepnumberLinkStyle = new SimpleAttributeSet();

    public static final java.util.List<String> FONTFILES = java.util.List.of(
      "fonts/sitka/SitkaDisplay.ttf",
      "fonts/sitka/SitkaDisplay-Italic.ttf",
      "fonts/sitka/SitkaDisplay-Bold.ttf",
      "fonts/sitka/SitkaDisplay-BoldItalic.ttf",

      "fonts/roboto/Roboto-Regular.ttf",
      "fonts/roboto/Roboto-Italic.ttf",
      "fonts/roboto/Roboto-Bold.ttf",
      "fonts/roboto/Roboto-BoldItalic.ttf",

      "fonts/courierprime/CourierPrime.ttf",
      "fonts/courierprime/CourierPrime-Italic.ttf",
      "fonts/courierprime/CourierPrime-Bold.ttf",
      "fonts/courierprime/CourierPrime-BoldItalic.ttf"
    );

    public static Font DEFAULTFONT;

    static {
        try {
          for (String fontFile : FONTFILES) {
            Font registeredFont = registerFont(fontFile);
            if (DEFAULTFONT == null) {
              DEFAULTFONT = registeredFont;
            }
          }
        }
        catch(Exception x) {
            x.printStackTrace();
        }
    }

    private static Font registerFont(String ttfFilename) throws FontFormatException, IOException {
      try (InputStream fontsStream = Specman.class.getClassLoader().getResourceAsStream(ttfFilename)) {
        Font font = Font.createFont(Font.TRUETYPE_FONT, fontsStream);
        GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont( font );
        return font;
      }
    }

    public static Font labelFont = new Font(Font.SANS_SERIF, Font.BOLD, SCHRITTNR_FONTSIZE);

    public static final Color Schriftfarbe_Geloescht = Color.LIGHT_GRAY;
    public static final Color Hintergrundfarbe_Deviderbar = Color.LIGHT_GRAY;
    public static final Color DIAGRAMM_LINE_COLOR = Color.black;
    public static final Color TEXT_BACKGROUND_COLOR_STANDARD = new Color(255, 255, 255, 0);
    public static final Color BACKGROUND_COLOR_STANDARD = Color.white;
    public static final Color SCHRITTNUMMER_VORDERGRUNDFARBE = BACKGROUND_COLOR_STANDARD;
    public static final String INDIKATOR_GELOESCHT_MARKIERT = "line-through";
    public static final ReadWriteColor SCHRITTNUMMER_FARBE = new ReadWriteColor(Color.LIGHT_GRAY);
    public static final ReadWriteColor DELETED_BACKGROUND_COLOR = new ReadWriteColor(Color.BLACK);

    public static final ReadWriteColor STEPNUMBER_LINK_COLOR = new ReadWriteColor(new Color(220, 220, 220));
    public static final ChangeColorSet AENDERUNGSFARBE = new ChangeColorSet(
      Color.yellow,
      new Color(255, 255, 200),
      STEPNUMBER_LINK_COLOR.color);

    static {
        StyleConstants.setBackground(geloeschtStil, AENDERUNGSFARBE.text.color);
        StyleConstants.setStrikeThrough(geloeschtStil, true);

        StyleConstants.setBackground(ganzerSchrittGeloeschtStil, DELETED_BACKGROUND_COLOR.color);
        StyleConstants.setStrikeThrough(ganzerSchrittGeloeschtStil, true);
        StyleConstants.setForeground(ganzerSchrittGeloeschtStil, Schriftfarbe_Geloescht);

        StyleConstants.setBackground(standardTextBackground, TEXT_BACKGROUND_COLOR_STANDARD);

        StyleConstants.setBackground(quellschrittStil, AENDERUNGSFARBE.text.color);
        StyleConstants.setStrikeThrough(quellschrittStil, true);
        StyleConstants.setForeground(quellschrittStil, Schriftfarbe_Geloescht);
        StyleConstants.setFontSize(quellschrittStil, 7);

        StyleConstants.setBackground(deletedStepnumberLinkStyle, AENDERUNGSFARBE.stepnumberLink.color);
        StyleConstants.setStrikeThrough(deletedStepnumberLinkStyle, true);
    }

    public static String toHTMLColor(Color color) {
        if (color == null) {
            return "#000000";
        }
        else if (color.getAlpha() != 255) {
            return String.format(Locale.US, "rgba(%d, %d, %d, %1.1f)",
              color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() / 255f);
        }
        return "#" + Integer.toHexString(color.getRGB()).substring(2).toLowerCase();
    }

    public static Color combineColors(Color color, Color anotherColor) {
        int r = (color.getRed() + anotherColor.getRed()) / 2;
        int g = (color.getGreen() + anotherColor.getGreen()) / 2;
        int b = (color.getBlue() + anotherColor.getBlue()) / 2;
        return new Color(r, g, b);
    }
}

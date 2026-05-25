package specman.graphics;

import net.atlanticbb.tantlinger.ui.text.actions.SpecmanFontSizeAction;
import specman.Specman;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.util.List;
import java.io.IOException;
import java.io.InputStream;

public class Styles {
    public static final int FONTSIZE = SpecmanFontSizeAction.DEFAULT_FONTSIZE;
    public static final int SCHRITTNR_FONTSIZE = 10;

    public static MutableAttributeSet ganzerSchrittGeloeschtStil = new SimpleAttributeSet();
    public static MutableAttributeSet standardTextBackground = new SimpleAttributeSet();

    public static final List<String> FONTFILES = List.of(
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

    static {
        StyleConstants.setBackground(ganzerSchrittGeloeschtStil, DELETED_BACKGROUND_COLOR.color);
        StyleConstants.setStrikeThrough(ganzerSchrittGeloeschtStil, true);
        StyleConstants.setForeground(ganzerSchrittGeloeschtStil, Schriftfarbe_Geloescht);

        StyleConstants.setBackground(standardTextBackground, TEXT_BACKGROUND_COLOR_STANDARD);
    }

}

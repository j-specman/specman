package specman.editarea;

import net.atlanticbb.tantlinger.ui.text.actions.SpecmanFontSizeAction;
import net.atlanticbb.tantlinger.ui.text.actions.SpecmanHTMLFontAction;
import specman.Specman;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class TextStyles {
    public static final int FONTSIZE = SpecmanFontSizeAction.DEFAULT_FONTSIZE;
    public static final int SCHRITTNR_FONTSIZE = 10;

    public static MutableAttributeSet geaendertTextBackground = new SimpleAttributeSet();
    public static MutableAttributeSet geaendertTextBackgroundPDF = new SimpleAttributeSet();
    public static MutableAttributeSet geloeschtStil = new SimpleAttributeSet();
    public static MutableAttributeSet ganzerSchrittGeloeschtStil = new SimpleAttributeSet();
    public static MutableAttributeSet standardStil = new SimpleAttributeSet();
    public static MutableAttributeSet standardTextBackground = new SimpleAttributeSet();
    public static MutableAttributeSet quellschrittStil = new SimpleAttributeSet();
    public static MutableAttributeSet stepnumberLinkStyle = new SimpleAttributeSet();
    public static MutableAttributeSet stepnumberLinkStylePDF = new SimpleAttributeSet();
    public static MutableAttributeSet changedStepnumberLinkStyle = new SimpleAttributeSet();
    public static MutableAttributeSet changedStepnumberLinkStylePDF = new SimpleAttributeSet();
    public static MutableAttributeSet deletedStepnumberLinkStyle = new SimpleAttributeSet();
    public static MutableAttributeSet autoSuggestStyle = new SimpleAttributeSet();

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


  //public static Font font = new Font(Font.SERIF, Font.PLAIN, FONTSIZE);
    //public static Font font = new Font("Times New Roman", Font.PLAIN, FONTSIZE);

    public static Font labelFont = new Font(Font.SANS_SERIF, Font.BOLD, SCHRITTNR_FONTSIZE);

    public static final Color Schriftfarbe_Geloescht = Color.LIGHT_GRAY;
    public static final Color Hintergrundfarbe_Schrittnummer = Color.LIGHT_GRAY;
    public static final Color Hintergrundfarbe_Deviderbar = Color.LIGHT_GRAY;
    public static final Color Hintergrundfarbe_Geloescht = Color.BLACK;
    public static final Color Schriftfarbe_Standard = Color.BLACK;
    public static final Color DIAGRAMM_LINE_COLOR = Color.black;
    public static final Color TEXT_BACKGROUND_COLOR_STANDARD = new Color(255, 255, 255, 0);
    public static final Color BACKGROUND_COLOR_STANDARD = Color.white;
    public static final Color AENDERUNGSMARKIERUNG_FARBE = Color.yellow;
    public static final Color AENDERUNGSMARKIERUNG_HINTERGRUNDFARBE = new Color(255, 255, 200);
    public static final Color SCHRITTNUMMER_VORDERGRUNDFARBE = BACKGROUND_COLOR_STANDARD;
    public static final Color SCHRITTNUMMER_HINTERGRUNDFARBE2 = Color.BLACK;
    public static final Color stepnumberLinkStyleColor = new Color(220, 220, 220);
    public static final Color changedStepnumberLinkColor = combineColors(stepnumberLinkStyleColor, AENDERUNGSMARKIERUNG_FARBE);
    public static final String INDIKATOR_GELB = toHTMLColor(AENDERUNGSMARKIERUNG_FARBE);
    public static final String INDIKATOR_GELOESCHT_MARKIERT = "line-through";

    public static final String INDIKATOR_GRAU = toHTMLColor(Hintergrundfarbe_Schrittnummer);
    public static final String INDIKATOR_SCHWARZ = toHTMLColor(SCHRITTNUMMER_HINTERGRUNDFARBE2);
    public static final String stepnumberLinkStyleHTMLColor = toHTMLColor(stepnumberLinkStyleColor);
    public static final String changedStepnumberLinkHTMLColor = toHTMLColor(changedStepnumberLinkColor);

    static {
      // TODO JL: Modify comment concerning usage in PDF
      // Das hier ist ein bisschen tricky:
      // Die Zeile mit StyleConstants.setBackground sorgt dafür, dass man die Hintergrundfarbe
      // unmittelbar beim Editieren in der Oberfläche sieht. Allerdings taucht sie dann nicht
      // im abgespeicherten HTML auf und geht auch verloren, sobald man einen Zeilenumbruch im
      // Text einfügt. Also braucht man noch ein weiteres, persistentes Styling über ein Span-Tag,
      // wie ich es hier gefunden habe:
      // https://stackoverflow.com/questions/13285526/jtextpane-text-background-color-does-not-work
      String htmlStyleGeaendert = "background-color:" + INDIKATOR_GELB;
      String htmlStyleSchwarz = "background-color:" + INDIKATOR_SCHWARZ;
      String htmlStyleStandard = "background-color:" + toHTMLColor(TEXT_BACKGROUND_COLOR_STANDARD);
      String htmlStyleSteplink = "background-color:" + stepnumberLinkStyleHTMLColor;
      String htmlStyleChangedSteplink = "background-color:" + changedStepnumberLinkHTMLColor;

      SimpleAttributeSet htmlBackgroundStyleChanged = new SimpleAttributeSet();
      SimpleAttributeSet htmlBackgroundStyleBlack = new SimpleAttributeSet();
      SimpleAttributeSet htmlBackgroundStyleStandard = new SimpleAttributeSet();
      SimpleAttributeSet htmlBackgroundStyleSteplink = new SimpleAttributeSet();
      SimpleAttributeSet htmlBackgroundStyleChangedSteplink = new SimpleAttributeSet();

      htmlBackgroundStyleChanged.addAttribute(HTML.Attribute.STYLE, htmlStyleGeaendert);
      StyleConstants.setBackground(geaendertTextBackground, AENDERUNGSMARKIERUNG_FARBE);
      StyleConstants.setBackground(geaendertTextBackgroundPDF, AENDERUNGSMARKIERUNG_FARBE);
      geaendertTextBackgroundPDF.addAttribute(HTML.Tag.SPAN, htmlBackgroundStyleChanged);

      StyleConstants.setBackground(geloeschtStil, AENDERUNGSMARKIERUNG_FARBE);
      StyleConstants.setStrikeThrough(geloeschtStil, true);

      htmlBackgroundStyleBlack.addAttribute(HTML.Attribute.STYLE, htmlStyleSchwarz);
      ganzerSchrittGeloeschtStil.addAttribute(HTML.Tag.SPAN, htmlBackgroundStyleBlack);
      StyleConstants.setBackground(ganzerSchrittGeloeschtStil, Hintergrundfarbe_Geloescht);
      StyleConstants.setStrikeThrough(ganzerSchrittGeloeschtStil, true);
      StyleConstants.setForeground(ganzerSchrittGeloeschtStil, Schriftfarbe_Geloescht);

      htmlBackgroundStyleStandard.addAttribute(HTML.Attribute.STYLE, htmlStyleStandard);
      standardStil.addAttribute(HTML.Tag.SPAN, htmlBackgroundStyleStandard);
      StyleConstants.setBackground(standardStil, TEXT_BACKGROUND_COLOR_STANDARD);
      StyleConstants.setStrikeThrough(standardStil, false);
      StyleConstants.setForeground(standardStil, Schriftfarbe_Standard);
      StyleConstants.setBackground(standardTextBackground, TEXT_BACKGROUND_COLOR_STANDARD);

      quellschrittStil.addAttribute(HTML.Tag.SPAN, htmlBackgroundStyleChanged);
      StyleConstants.setBackground(quellschrittStil, AENDERUNGSMARKIERUNG_FARBE);
      StyleConstants.setStrikeThrough(quellschrittStil, true);
      StyleConstants.setForeground(quellschrittStil, Schriftfarbe_Geloescht);
      StyleConstants.setFontSize(quellschrittStil, 7);

      StyleConstants.setBackground(stepnumberLinkStyle, stepnumberLinkStyleColor);
      htmlBackgroundStyleSteplink.addAttribute(HTML.Attribute.STYLE, htmlStyleSteplink);
      StyleConstants.setBackground(stepnumberLinkStylePDF, stepnumberLinkStyleColor);
      stepnumberLinkStylePDF.addAttribute(HTML.Tag.SPAN, htmlBackgroundStyleSteplink);

      StyleConstants.setBackground(changedStepnumberLinkStyle, changedStepnumberLinkColor);
      htmlBackgroundStyleChangedSteplink.addAttribute(HTML.Attribute.STYLE, htmlStyleChangedSteplink);
      StyleConstants.setBackground(changedStepnumberLinkStylePDF, changedStepnumberLinkColor);
      changedStepnumberLinkStylePDF.addAttribute(HTML.Tag.SPAN, htmlBackgroundStyleChangedSteplink);

      deletedStepnumberLinkStyle.addAttribute(HTML.Tag.SPAN, htmlBackgroundStyleChanged);
      StyleConstants.setBackground(deletedStepnumberLinkStyle, changedStepnumberLinkColor);
      StyleConstants.setStrikeThrough(deletedStepnumberLinkStyle, true);

      StyleConstants.setForeground(autoSuggestStyle, Color.GRAY);
      StyleConstants.setItalic(autoSuggestStyle, true);
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
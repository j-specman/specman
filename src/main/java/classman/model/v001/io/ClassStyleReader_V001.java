package classman.model.v001.io;

import classman.model.v001.ClassDiagram_V001Lexer;
import classman.model.v001.ClassDiagram_V001Parser.StyleEntryContext;
import classman.model.v001.ClassDiagram_V001Parser.StyleSpecContext;
import classman.model.v001.ClassStyleModel_V001;
import classman.model.v001.FontFamily_V001;
import org.antlr.v4.runtime.Token;

import java.util.Locale;
import java.util.regex.Pattern;

/** Reads the style(...) of a class head into the model and checks what the grammar leaves open. */
class ClassStyleReader_V001 {
    private static final Pattern COLOR = Pattern.compile("#[0-9a-fA-F]{6}");

    /** @return the style, a standard one if there is no style(...)
     * @throws ClassDiagramParseException if an entry stands twice, a color isn't written "#rrggbb", the font isn't
     * one of Specman's, or the font size isn't one of the sizes */
    ClassStyleModel_V001 read(StyleSpecContext spec) throws ClassDiagramParseException {
        ClassStyleModel_V001 style = new ClassStyleModel_V001();
        if (spec == null) {
            return style;
        }
        for (StyleEntryContext entry : spec.styleEntry()) {
            if (entry.FONT() != null) {
                style.font = set(entry, style.font, font(entry));
            }
            else if (entry.FONTSIZE() != null) {
                style.fontSize = set(entry, style.fontSize, fontSize(entry));
            }
            else {
                readColor(entry, style);
            }
        }
        return style;
    }

    private void readColor(StyleEntryContext entry, ClassStyleModel_V001 style) throws ClassDiagramParseException {
        String color = color(entry);
        switch (entry.colorKey.getType()) {
            case ClassDiagram_V001Lexer.FILL:
                style.fill = set(entry, style.fill, color);
                break;
            case ClassDiagram_V001Lexer.TEXTCOLOR:
                style.textColor = set(entry, style.textColor, color);
                break;
            default:
                style.lineColor = set(entry, style.lineColor, color);
        }
    }

    private String color(StyleEntryContext entry) throws ClassDiagramParseException {
        String text = entry.QUOTED_STRING().getText();
        String color = text.substring(1, text.length() - 1);
        if (!COLOR.matcher(color).matches()) {
            throw problem(entry.QUOTED_STRING().getSymbol(),
                "The " + entry.colorKey.getText() + " of a class must be a color written \"#rrggbb\".");
        }
        return color.toLowerCase(Locale.ROOT);
    }

    private FontFamily_V001 font(StyleEntryContext entry) throws ClassDiagramParseException {
        FontFamily_V001 font = FontFamily_V001.fromSourceName(entry.word().getText());
        if (font == null) {
            throw problem(entry.word().getStart(), "The font of a class must be one of " + fontNames() + ".");
        }
        return font;
    }

    private Integer fontSize(StyleEntryContext entry) throws ClassDiagramParseException {
        Integer size = Integer.valueOf(entry.NUMBER().getText());
        if (!ClassStyleModel_V001.FONT_SIZES.contains(size)) {
            throw problem(entry.NUMBER().getSymbol(),
                "The font size of a class must be one of " + ClassStyleModel_V001.FONT_SIZES + ".");
        }
        return size;
    }

    private <T> T set(StyleEntryContext entry, T previous, T value) throws ClassDiagramParseException {
        if (previous != null) {
            throw problem(entry.getStart(), "The style has \"" + entry.getStart().getText() + "\" twice.");
        }
        return value;
    }

    private static String fontNames() {
        StringBuilder names = new StringBuilder();
        for (FontFamily_V001 family : FontFamily_V001.values()) {
            names.append(names.length() == 0 ? "" : ", ").append(family.sourceName);
        }
        return names.toString();
    }

    private ClassDiagramParseException problem(Token at, String message) {
        return new ClassDiagramParseException("line " + at.getLine() + ":" + at.getCharPositionInLine() + " " + message);
    }
}

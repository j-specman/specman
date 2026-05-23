package specman;

import org.jetbrains.annotations.Nullable;
import specman.editarea.markups.MarkupType;
import specman.model.v001.EditorContentModel_V001;
import specman.model.v001.Markup_V001;
import specman.model.v001.TextEditAreaModel_V001;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static specman.Aenderungsart.Hinzugefuegt;
import static specman.Aenderungsart.Untracked;
import static specman.styles.Styles.AENDERUNGSFARBE;
import static specman.styles.Styles.BACKGROUND_COLOR_STANDARD;

public class TextInit {

  public static EditorContentModel_V001 initialtext(String text) {
    return initialtext(text, null);
  }

  public static EditorContentModel_V001 initialtext(String text, @Nullable String align) {
    List<Markup_V001> markups = new ArrayList<>();
    if (Specman.instance().aenderungenVerfolgen()) {
      markups.add(new Markup_V001(0, text.length() - 1, MarkupType.Changed));
    }
    String styledText = (align != null)
        ? "<div align='" + align + "'>" + text + "</div>"
        : text;
    TextEditAreaModel_V001 textModel = new TextEditAreaModel_V001(styledText, text, markups, initialArt());
    return new EditorContentModel_V001(textModel);
  }

  public static Color schrittHintergrund() {
    return (Specman.instance() != null && Specman.instance().aenderungenVerfolgen())
        ? AENDERUNGSFARBE.panelColor
        : BACKGROUND_COLOR_STANDARD;
  }

  public static Aenderungsart initialArt() {
    return (Specman.instance() != null && Specman.instance().aenderungenVerfolgen())
        ? Hinzugefuegt
        : Untracked;
  }

}

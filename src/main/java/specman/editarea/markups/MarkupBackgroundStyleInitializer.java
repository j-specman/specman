package specman.editarea.markups;

import specman.editarea.TextEditArea;
import specman.editarea.document.WrappedDocument;
import specman.model.v001.Markup_V001;

import javax.swing.text.AttributeSet;

import java.util.ArrayList;
import java.util.List;

import static specman.graphics.Styles.standardTextBackground;

/** The yellow background of text sections being marked as changed, is <i>not</i> included
 * in the HTML content of a text area. The styling must therefore be initialized from the
 * markup list in the model. The same applies to the background graphics for stepnumber links
 * and the combination of both. Important detail: text without change-style, following a
 * change-styled section must <i>explicitely</i> be "un-styled", otherwise the change-style
 * applies to the succeeding text too. The graphics being used here are "overlays", only focussed
 * on text background. So they do not destroy any foreground styling, font sizing and so on. */
public class MarkupBackgroundStyleInitializer {
  private final WrappedDocument doc;
  private final List<Markup_V001> model;

  public MarkupBackgroundStyleInitializer(TextEditArea textEditArea, List<Markup_V001> model) {
    this(textEditArea.getWrappedDocument(), model);
  }

  public MarkupBackgroundStyleInitializer(WrappedDocument doc, List<Markup_V001> model) {
    this.doc = doc;
    this.model = model;
  }

  private List<StyledSection> model2StyledSections(List<Markup_V001> model) {
    List<StyledSection> stylings = new ArrayList<>();
    for (int i = 0; i < model.size(); i++) {
      Markup_V001 change = model.get(i);
      AttributeSet style = TextMarkup.toBackground(change);
      StyledSection changeSection = new StyledSection(change.getFrom(), change.laenge(), style);
      stylings.add(changeSection);
      StyledSection standardSection = followingStandardSection(model, i);
      if (standardSection != null) {
        stylings.add(standardSection);
      }
    }
    return stylings;
  }

  private StyledSection followingStandardSection(List<Markup_V001> model, int i) {
    Markup_V001 lastChange = model.get(i);
    int resetStart = lastChange.getFrom() + lastChange.laenge() + 1;
    int resetLength;
    if (model.size() > i+1) {
      Markup_V001 nextChange = model.get(i+1);
      resetLength = nextChange.getFrom() - 1 - resetStart;
    }
    else {
      resetLength = doc.getLength() - resetStart;
    }
    return resetLength > 0
      ? new StyledSection(resetStart, resetLength, standardTextBackground)
      : null;
  }

  public void styleChangedTextSections() {
    List<StyledSection> stylings = model2StyledSections(model);
    for (StyledSection styling: stylings) {
      doc.setCharacterAttributes(doc.fromModel(styling.start), styling.length, styling.style, false);
    }
  }

  private static class StyledSection {
    final int start;
    final int length;
    final AttributeSet style;

    public StyledSection(int start, int length, AttributeSet style) {
      this.start = start;
      this.length = length;
      this.style = style;
    }

  }
}

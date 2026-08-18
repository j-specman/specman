package specman.editarea.markups;

import specman.editarea.TextEditArea;
import specman.editarea.document.WrappedDocument;
import specman.model.v002.Markup_V002;

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
  private final List<MarkupEntry> model;

  private record MarkupEntry(int from, int length, String changeset, MarkupType type) {
    static MarkupEntry from(Markup_V002 m) {
      return new MarkupEntry(m.from, m.to - m.from + 1, m.changeset, m.type);
    }
  }

  public MarkupBackgroundStyleInitializer(TextEditArea textEditArea, List<Markup_V002> markups) {
    this.doc = textEditArea.getWrappedDocument();
    this.model = markups.stream().map(MarkupEntry::from).toList();
  }

  private List<StyledSection> model2StyledSections(List<MarkupEntry> model) {
    List<StyledSection> stylings = new ArrayList<>();
    for (int i = 0; i < model.size(); i++) {
      MarkupEntry change = model.get(i);
      AttributeSet style = TextMarkup.toBackground(change.type(), change.changeset());
      StyledSection changeSection = new StyledSection(change.from(), change.length(), style);
      stylings.add(changeSection);
      StyledSection standardSection = followingStandardSection(model, i);
      if (standardSection != null) {
        stylings.add(standardSection);
      }
    }
    return stylings;
  }

  private StyledSection followingStandardSection(List<MarkupEntry> model, int i) {
    MarkupEntry lastChange = model.get(i);
    int resetStart = lastChange.from() + lastChange.length() + 1;
    int resetLength;
    if (model.size() > i+1) {
      MarkupEntry nextChange = model.get(i+1);
      resetLength = nextChange.from() - 1 - resetStart;
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

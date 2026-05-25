package specman.editarea.markups;

import specman.ChangeSet;
import specman.editarea.document.WrappedElement;
import specman.model.v001.Markup_V001;

import javax.swing.text.AttributeSet;
import javax.swing.text.html.CSS;

import static specman.ChangeSet.STEPNUMBER_LINK_COLOR;
import static specman.ChangeSet.changeset;

public class TextMarkup {

  public final MarkupType type;
  public final ChangeSet changeSet;

  public TextMarkup(MarkupType type, ChangeSet changeSet) {
    this.type = type;
    this.changeSet = changeSet;
  }

  public static TextMarkup fromBackground(WrappedElement element) {
    String cssColor = MarkupType.getBackgroundColorFromElement(element);
    return ChangeSet.textMarkupFromBackground(cssColor);
  }

  public static AttributeSet toBackground(Markup_V001 change) {
    ChangeSet changeSet = ChangeSet.fromName(change.getChangeset());
    if (changeSet == null) {
      changeSet = ChangeSet.changeset();
    }
    switch (change.getType()) {
      case Changed:
        return changeSet.textBackground();
      case ChangedSteplink:
        return changeSet.stepnumberLinkBackground();
      case Steplink:
        return STEPNUMBER_LINK_COLOR.background;
    }
    return null;
  }

  public boolean marksChange() { return type.marksChange(); }

  public boolean matches(MarkupSearchPurpose searchPurpose) {
    return type.matches(searchPurpose);
  }
}

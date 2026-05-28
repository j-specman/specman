package specman.editarea.markups;

import org.jetbrains.annotations.Nullable;
import specman.ChangeSet;
import specman.editarea.document.WrappedElement;
import specman.model.v001.Markup_V001;
import specman.util.ObjectUtils;

import javax.swing.text.AttributeSet;
import javax.swing.text.html.CSS;

import java.util.Objects;

import static specman.ChangeSet.STEPNUMBER_LINK_COLOR;
import static specman.ChangeSet.changeset;
import static specman.util.ObjectUtils.nvl;

public class TextMarkup {

  public final MarkupType type;
  public final @Nullable ChangeSet changeSet;

  public TextMarkup(MarkupType type, @Nullable ChangeSet changeSet) {
    this.type = type;
    this.changeSet = changeSet;
  }

  public static TextMarkup fromBackground(WrappedElement element) {
    String cssColor = MarkupType.getBackgroundColorFromElement(element);
    return ChangeSet.textMarkupFromBackground(cssColor);
  }

  public static AttributeSet toBackground(Markup_V001 change) {
    ChangeSet changeSet = nvl(ChangeSet.fromName(change.getChangeset()), changeset());
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

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    TextMarkup markup = (TextMarkup) o;
    return type == markup.type && changeSet == markup.changeSet;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, changeSet);
  }

  public String changeSetName() {
    return changeSet != null ? changeSet.name : null;
  }
}

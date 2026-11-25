package specman.cli.sanitize;

import specman.ChangeSet;
import specman.model.v002.AbstractEditAreaModel_V002;
import specman.model.v002.DiagramModel_V002;
import specman.model.v002.Markup_V002;
import specman.model.v002.TextEditAreaModel_V002;
import specman.model.v002.io.ModelSourcePositions;
import specman.model.v002.io.TextAreaTraversal_V002;
import specman.model.v002.io.TextAreaTraversal_V002.Site;

import java.util.List;

/**
 * Checks the markups of the texts and the change sets they refer to.
 * <p>
 * The markups have to be checked against the plain texts as the sanitizer regenerates them, because their offsets refer
 * to the visible text, which the plain text is the rendition of.
 */
final class MarkupValidator extends AbstractModelValidator {

  private static final String KNOWN_CHANGESETS = String.join(", ", ChangeSet.ALL.keySet());

  private final DiagramModel_V002 model;
  private final ModelSourcePositions positions;

  MarkupValidator(DiagramModel_V002 model, ModelSourcePositions positions) {
    super("Invalid markups");
    this.model = model;
    this.positions = positions;
  }

  @Override
  protected void collectErrors(List<String> errors) {
    TextAreaTraversal_V002.forEachContent(model, (content, site) -> {
      for (AbstractEditAreaModel_V002 area : content.areas) {
        if (area instanceof TextEditAreaModel_V002 textArea) {
          checkMarkupsOfText(textArea, site, errors);
        }
      }
    });
  }

  private void checkMarkupsOfText(TextEditAreaModel_V002 area, Site site, List<String> errors) {
    if (area.markups == null) {
      return;
    }
    String plain = area.plainText != null ? area.plainText : "";
    Markup_V002 previous = null;
    for (Markup_V002 markup : area.markups) {
      checkMarkup(markup, previous, plain, site, errors);
      previous = markup;
    }
  }

  private void checkMarkup(Markup_V002 markup, Markup_V002 previous, String plain, Site site, List<String> errors) {
    String where = located(positions.of(markup), describe(site) + ": markup " + range(markup));
    if (endsBeforeItStarts(markup)) {
      errors.add(where + " ends before it starts");
    }
    else if (liesOutsideOf(plain, markup)) {
      errors.add(where + " lies outside of the " + describePlain(plain));
    }
    if (overlapsOrPrecedes(previous, markup)) {
      errors.add(where + " overlaps or precedes the previous markup " + range(previous)
        + ", markups must be in ascending order without overlap. Offsets refer to the " + describePlain(plain));
    }
    if (marksChangeWithoutChangeset(markup)) {
      errors.add(where + " is of type " + markup.type + " but names no changeset. Known changesets: " + KNOWN_CHANGESETS);
    }
    if (namesUnknownChangeset(markup)) {
      errors.add(where + " names the unknown changeset '" + markup.changeset + "'. Known changesets: " + KNOWN_CHANGESETS);
    }
  }

  private static boolean endsBeforeItStarts(Markup_V002 markup) {
    return markup.from > markup.to;
  }

  private static boolean liesOutsideOf(String plain, Markup_V002 markup) {
    return markup.from < 0 || markup.to >= plain.length();
  }

  /** @param previous the markup before the specified one, null for the first */
  private static boolean overlapsOrPrecedes(Markup_V002 previous, Markup_V002 markup) {
    return previous != null && markup.from <= previous.to;
  }

  private static boolean marksChangeWithoutChangeset(Markup_V002 markup) {
    return markup.changeset == null && markup.type != null && markup.type.marksChange();
  }

  private static boolean namesUnknownChangeset(Markup_V002 markup) {
    return markup.changeset != null && ChangeSet.fromName(markup.changeset) == null;
  }

  /** The plain text is part of each message, because the sanitizer doesn't write the file if it finds errors, so the
   * plain text in the file may be outdated. */
  private static String describePlain(String plain) {
    String lastIndex = plain.isEmpty() ? "" : ", last index " + (plain.length() - 1);
    return "plain text (length " + plain.length() + lastIndex + "): `" + plain + "`";
  }

  private static String range(Markup_V002 markup) {
    return "(" + markup.from + "," + markup.to + ")";
  }

  private static String describe(Site site) {
    return site.step() != null ? describe(site.step()) + " " + site.part() : site.part();
  }
}

package builder;

import specman.Aenderungsart;
import specman.ChangeInfo;
import specman.ChangeSet;
import specman.editarea.markups.MarkupType;
import specman.model.v002.AbstractStepModel_V002;
import specman.model.v002.ChangeInfoModel_V002;
import specman.model.v002.ClassmanImageEditAreaModel_V002;
import specman.model.v002.LinkedClassmanImageEditAreaModel_V002;
import specman.model.v002.EditorContentModel_V002;
import specman.model.v002.ListItemEditAreaModel_V002;
import specman.model.v002.Markup_V002;
import specman.model.v002.MetaTagModel_V002;
import specman.model.v002.TableEditAreaModel_V002;
import specman.model.v002.TextEditAreaModel_V002;

import java.util.ArrayList;
import java.util.List;

/**
 * Base of all step builders. The second type parameter is the concrete builder type, so the
 * with-methods defined here return it and can be freely chained with the with-methods of
 * the concrete builders, e.g. {@code simple().withText("a").withId("12345678")}.
 */
abstract public class AbstractStepBuilder<S extends AbstractStepModel_V002, B extends AbstractStepBuilder<S, B>> {
  private static final int TABLE_WIDTH_PERCENT = 100;

  protected S step;

  protected AbstractStepBuilder(S step) {
    this.step = step;
    withDefaults();
  }

  @SuppressWarnings("unchecked")
  protected B self() {
    return (B) this;
  }

  /** Called from the constructor, so overrides must not rely on fields of the concrete builder
   *  which are initialized by an initializer: they are set only after this method has run. */
  protected B withDefaults() {
    return withId(AbstractStepModel_V002.generateId())
      .withText("");
  }

  public B withId(String id) {
    step.id = id;
    return self();
  }

  public B withStepNumber(String stepNumber) {
    step.stepNumber = stepNumber;
    return self();
  }

  public B withText(String text) {
    step.content = new EditorContentModel_V002(text, null);
    return self();
  }

  /** Marks the first occurrence of the specified fragment of the primary text, e.g. as step link.
   *  Must be called after {@link #withText(String)}, which resets the markups. */
  public B withMarkup(String fragment, MarkupType type, String changeset) {
    TextEditAreaModel_V002 textArea = primaryTextArea();
    int from = textArea.text.indexOf(fragment);
    if (from < 0) {
      throw new IllegalArgumentException("'" + fragment + "' not found in '" + textArea.text + "'");
    }
    int to = from + fragment.length() - 1;
    textArea.markups.add(new Markup_V002(from, to, type, changeset));
    return self();
  }

  /** Adds a markup with exactly the specified offsets, valid or not. Must be called after {@link #withText(String)}. */
  public B withMarkupAt(int from, int to, MarkupType type, String changeset) {
    primaryTextArea().markups.add(new Markup_V002(from, to, type, changeset));
    return self();
  }

  /** Adds a list item to the step, whose text has the specified markup. */
  public B withListItemMarkup(String text, int from, int to, MarkupType type, String changeset) {
    step.content.areas.add(new ListItemEditAreaModel_V002(contentWithMarkup(text, from, to, type, changeset), false, null));
    return self();
  }

  /** Adds a table with one cell to the step, whose text has the specified markup. */
  public B withTableCellMarkup(String text, int from, int to, MarkupType type, String changeset) {
    List<List<EditorContentModel_V002>> cells = List.of(List.of(contentWithMarkup(text, from, to, type, changeset)));
    step.content.areas.add(new TableEditAreaModel_V002(cells, TABLE_WIDTH_PERCENT, List.of(TABLE_WIDTH_PERCENT), null));
    return self();
  }

  private static EditorContentModel_V002 contentWithMarkup(String text, int from, int to, MarkupType type, String changeset) {
    List<Markup_V002> markups = new ArrayList<>(List.of(new Markup_V002(from, to, type, changeset)));
    return new EditorContentModel_V002(new TextEditAreaModel_V002(text, text, markups, (ChangeInfo) null));
  }

  /** Tracks the step as changed in the specified change set, e.g. "yellow". */
  public B withChange(Aenderungsart type, String changesetName) {
    step.changeInfo = ChangeInfoModel_V002.from(new ChangeInfo(type, ChangeSet.fromName(changesetName)));
    return self();
  }

  /** The ID of the counterpart of a moved step: the source step of a target step, and the other way round. */
  public B withSourceStepId(String id) {
    step.sourceStepId = id;
    return self();
  }

  public B withSteplink(String fragment) {
    return withMarkup(fragment, MarkupType.Steplink, null);
  }


  /** Assigns the meta tag of the specified configuration to the step. Without freetext if null. */
  public B withTag(String configName, String freetext) {
    step.tags.add(new MetaTagModel_V002(configName, freetext));
    return self();
  }

  public B withTag(String configName) {
    return withTag(configName, null);
  }

  /** Adds a Classman diagram with the given source behind the text of the step. */
  public B withClassmanDiagram(String source) {
    step.content.areas.add(new ClassmanImageEditAreaModel_V002(source, 1f, null));
    return self();
  }

  /** Adds a Classman diagram which is linked to a file, behind the text of the step. */
  public B withLinkedClassmanDiagram(String link) {
    step.content.areas.add(new LinkedClassmanImageEditAreaModel_V002(link, 1f, null));
    return self();
  }

  /** Adds a table with one cell, which holds a text and a Classman diagram with the given source, behind the text of the
   * step. */
  public B withClassmanDiagramInTableCell(String source) {
    EditorContentModel_V002 cell = new EditorContentModel_V002("cell", null);
    cell.addArea(new ClassmanImageEditAreaModel_V002(source, 1f, null));
    step.content.areas.add(new TableEditAreaModel_V002(List.of(List.of(cell)), TABLE_WIDTH_PERCENT,
      List.of(TABLE_WIDTH_PERCENT), null));
    return self();
  }

  /** Overrides the plain text rendition of the primary text. By default it equals the text itself.
   *  Must be called after {@link #withText(String)}, which resets it. */
  public B withPlainText(String plainText) {
    primaryTextArea().plainText = plainText;
    return self();
  }

  /** The primary text of the step as it was specified by {@link #withText(String)}. */
  public String text() {
    return primaryTextArea().text;
  }

  private TextEditAreaModel_V002 primaryTextArea() {
    return (TextEditAreaModel_V002) step.content.areas.get(0);
  }

  public S build() {
    return step;
  }
}

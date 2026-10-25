package builder;

import specman.editarea.markups.MarkupType;
import specman.model.v002.AbstractStepModel_V002;
import specman.model.v002.EditorContentModel_V002;
import specman.model.v002.Markup_V002;
import specman.model.v002.TextEditAreaModel_V002;

/**
 * Base of all step builders. The second type parameter is the concrete builder type, so the
 * with-methods defined here return it and can be freely chained with the with-methods of
 * the concrete builders, e.g. {@code simple().withText("a").withId("12345678")}.
 */
abstract public class AbstractStepBuilder<S extends AbstractStepModel_V002, B extends AbstractStepBuilder<S, B>> {
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

  public B withSteplink(String fragment) {
    return withMarkup(fragment, MarkupType.Steplink, null);
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

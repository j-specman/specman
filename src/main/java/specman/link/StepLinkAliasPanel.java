package specman.link;

import specman.view.AbstractSchrittView;

import java.io.File;
import java.io.Serial;
import java.util.function.Consumer;

/** Asks for the alias of the file when the user copies a link to a step. The link shown is the one to the step. */
public class StepLinkAliasPanel extends AliasPanel {
  @Serial
  private static final long serialVersionUID = 1L;

  public StepLinkAliasPanel(File file, AbstractSchrittView step, Consumer<String> aliasAccepted) {
    super(file, "dialog.link.alias.title", "dialog.link.alias.description", alias -> SpecmanLink.of(file, alias, step),
      aliasAccepted);
  }
}

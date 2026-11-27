package specman.link;

import specman.view.AbstractSchrittView;

import java.io.File;
import java.io.Serial;
import java.util.function.Consumer;

/** Asks how the link names the file when the user copies a link to a step. All the ways to name a file are on offer.
 * The relative path is the one to a file in the same directory as the document the link is pasted into, which is not
 * known yet, so it is the name of the file. */
public class StepLinkAliasPanel extends AliasPanel {
  @Serial
  private static final long serialVersionUID = 1L;

  public StepLinkAliasPanel(File file, AbstractSchrittView step, Consumer<LinkLocation> locationAccepted) {
    super(file, "dialog.link.alias.title", "dialog.link.alias.description",
      location -> SpecmanLink.of(location, step), LinkLocation.relative(file, LinkLocation.baseDirectoryOf(file)), true,
      locationAccepted);
  }
}

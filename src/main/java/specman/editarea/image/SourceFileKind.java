package specman.editarea.image;

/** What the dialog to choose a source file says and filters for, which differs for the kinds of diagrams that are
 * shown from a source. The message keys are those of the dialog's title, the button to start without a file and
 * its tooltip. The filter is only a preselection, the user can still choose any file. */
public class SourceFileKind {
  public final String[] extensions;
  public final String filterMessageKey;
  public final String chooseTitleKey;
  public final String importTitleKey;
  public final String emptyButtonKey;
  public final String emptyButtonTooltipKey;

  public SourceFileKind(String[] extensions, String filterMessageKey, String chooseTitleKey, String importTitleKey,
                        String emptyButtonKey, String emptyButtonTooltipKey) {
    this.extensions = extensions.clone();
    this.filterMessageKey = filterMessageKey;
    this.chooseTitleKey = chooseTitleKey;
    this.importTitleKey = importTitleKey;
    this.emptyButtonKey = emptyButtonKey;
    this.emptyButtonTooltipKey = emptyButtonTooltipKey;
  }
}

package classman.model.v001.change;


/** Accepts, rejects and renames the change marks, change(...) and was(...), in the source text of a Classman diagram. */
public class SourceMarks_V001 {
  private final SourceRewriter_V001 rewriter = new SourceRewriter_V001();
  private final ChangeResolver_V001 resolver = new ChangeResolver_V001();
  private final ChangeSetRenamer_V001 renamer = new ChangeSetRenamer_V001();

  /** @return the source with the marks of the change set accepted, the given source itself if there is nothing to accept */
  public String accepted(String source, String changeSet) {
    return rewriter.rewritten(source, model -> resolver.accept(model, changeSet));
  }

  /** @return the source with the marks of the change set rejected, the given source itself if there is nothing to reject */
  public String rejected(String source, String changeSet) {
    return rewriter.rewritten(source, model -> resolver.reject(model, changeSet));
  }

  /** @return the source with the marks of one change set given to another, the given source itself if there are none */
  public String renamed(String source, String from, String to) {
    return rewriter.rewritten(source, model -> renamer.rename(model, from, to));
  }
}

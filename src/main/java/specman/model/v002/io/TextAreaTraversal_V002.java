package specman.model.v002.io;

import specman.StepNumber;
import specman.model.v002.AbstractEditAreaModel_V002;
import specman.model.v002.AbstractStepModel_V002;
import specman.model.v002.BranchSequenceModel_V002;
import specman.model.v002.CatchSequenceModel_V002;
import specman.model.v002.CoCatchModel_V002;
import specman.model.v002.DiagramModel_V002;
import specman.model.v002.EditorContentModel_V002;
import specman.model.v002.ListItemEditAreaModel_V002;
import specman.model.v002.StepSequenceModel_V002;
import specman.model.v002.TableEditAreaModel_V002;

import java.util.List;

/** Visits every editor content in a diagram model: the intro and outro, the contents of all steps, the headings of
 * branches and catches, and the contents nested in tables and lists. It is the single place knowing where texts
 * can be in the model, for all checks and corrections which have to look at each of them. */
public final class TextAreaTraversal_V002 {

  /** Where a visited content is, as far as a message for the user needs to tell. */
  public record Site(AbstractStepModel_V002 step, String part) {
    private Site nested(String nestedPart) {
      return new Site(step, part + " " + nestedPart);
    }
  }

  @FunctionalInterface
  public interface ContentVisitor {
    void visit(EditorContentModel_V002 content, Site site);
  }

  private final ContentVisitor visitor;

  private TextAreaTraversal_V002(ContentVisitor visitor) {
    this.visitor = visitor;
  }

  public static void forEachContent(DiagramModel_V002 model, ContentVisitor visitor) {
    TextAreaTraversal_V002 traversal = new TextAreaTraversal_V002(visitor);
    traversal.visitContent(model.intro, new Site(null, "intro"));
    traversal.visitContent(model.outro, new Site(null, "outro"));
    traversal.visitSequence(model.mainSequence);
  }

  private void visitSequence(StepSequenceModel_V002 seq) {
    if (seq == null || seq.steps == null) {
      return;
    }
    for (AbstractStepModel_V002 step : seq.steps) {
      visitContent(step.content, new Site(step, "text"));
      for (NumberedSubSequence_V002 sub : step.subSequencesFor(StepNumber.EMPTY)) {
        if (sub.sequence instanceof BranchSequenceModel_V002 branch) {
          visitContent(branch.heading, new Site(step, "branch heading"));
        }
        visitSequence(sub.sequence);
      }
    }
    visitCatchArea(seq);
  }

  private void visitCatchArea(StepSequenceModel_V002 seq) {
    if (seq.catchArea == null || seq.catchArea.catchSequences == null) {
      return;
    }
    for (CatchSequenceModel_V002 catchSeq : seq.catchArea.catchSequences) {
      visitContent(catchSeq.heading, new Site(null, "catch heading"));
      if (catchSeq.coCatches != null) {
        for (CoCatchModel_V002 coCatch : catchSeq.coCatches) {
          visitContent(coCatch.heading, new Site(null, "coCatch heading"));
        }
      }
      visitSequence(catchSeq);
    }
  }

  private void visitContent(EditorContentModel_V002 content, Site site) {
    if (content == null || content.areas == null) {
      return;
    }
    visitor.visit(content, site);
    for (AbstractEditAreaModel_V002 area : List.copyOf(content.areas)) {
      if (area instanceof TableEditAreaModel_V002 table) {
        for (List<EditorContentModel_V002> row : table.cells) {
          for (EditorContentModel_V002 cell : row) {
            visitContent(cell, site.nested("table cell"));
          }
        }
      }
      else if (area instanceof ListItemEditAreaModel_V002 listItem) {
        visitContent(listItem.content, site.nested("list item"));
      }
    }
  }
}

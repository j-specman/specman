package specman.pdf.navigation;

import specman.editarea.document.WrappedDocument;
import specman.editarea.document.WrappedDocumentUtil;
import specman.editarea.document.WrappedElement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Read-only adapter for the existing UI's normal and change-marked link styles. */
public final class PdfTextReferences {
  private PdfTextReferences() {}

  public record Reference(int start, int end, String text) {}

  public static List<Reference> read(WrappedDocument document) {
    List<Reference> fragments = new ArrayList<>();
    for (WrappedElement root : document.getRootElements()) collect(root, document, fragments);
    fragments.sort(Comparator.comparingInt(Reference::start));
    List<Reference> references = new ArrayList<>();
    for (Reference fragment : fragments) {
      // Never resolve separate font runs of one marked number as independent step numbers.
      if (!references.isEmpty() && references.getLast().end() == fragment.start()) {
        Reference previous = references.removeLast();
        references.add(new Reference(previous.start(), fragment.end(), previous.text() + fragment.text()));
      }
      else references.add(fragment);
    }
    return List.copyOf(references);
  }

  private static void collect(WrappedElement element, WrappedDocument document, List<Reference> result) {
    if (element.isLeaf()) {
      if (!WrappedDocumentUtil.stepnumberLinkStyleSet(element)) return;
      int start = Math.max(0, element.getStartOffset().toModel());
      int end = Math.min(document.getCore().getLength() - document.getVisibleTextStart(),
        element.getEndOffset().toModel());
      if (end > start) {
        result.add(new Reference(start, end, document.getText(document.fromModel(start), end - start)));
      }
    }
    else {
      for (int i = 0; i < element.getElementCount(); i++) collect(element.getElement(i), document, result);
    }
  }
}

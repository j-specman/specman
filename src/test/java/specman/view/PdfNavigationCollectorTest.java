package specman.view;

import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.util.IdentityHashMap;
import java.util.Map;
import specman.pdf.Shape;
import specman.pdf.navigation.PdfNavigationPlan.TargetId;

import static org.junit.jupiter.api.Assertions.*;

class PdfNavigationCollectorTest {
  @Test
  void bindsStepAndCatchLabelsToDifferentSourceKeysEvenForTheSameDisplayedNumber() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      JPanel step = new JPanel(), heading = new JPanel(), stepLabel = new JPanel(), catchLabel = new JPanel();
      TargetId stepId = TargetId.step("one"), catchId = TargetId.catchHeading("one");
      Shape stepLabelShape = new Shape(stepLabel), catchLabelShape = new Shape(catchLabel);
      Shape stepShape = new Shape(step).add(new Shape(7, 0).add(stepLabelShape));
      Shape catchShape = new Shape(heading).add(new Shape(11, 0).add(catchLabelShape));
      Map<Object, TargetId> targets = new IdentityHashMap<>(), labels = new IdentityHashMap<>();
      targets.put(step, stepId);
      targets.put(heading, catchId);
      labels.put(stepLabel, stepId);
      labels.put(catchLabel, catchId);
      PdfNavigationCollector.bindShapes(new Shape(0, 0).add(stepShape).add(catchShape), targets,
        new IdentityHashMap<>(), labels);
      assertEquals(stepId, stepShape.getNavigationTarget());
      assertEquals(catchId, catchShape.getNavigationTarget());
      assertEquals(specman.pdf.navigation.PdfNavigationPlan.Source.label(stepId, true).key(),
        stepLabelShape.getNavigationTextKey());
      assertEquals(specman.pdf.navigation.PdfNavigationPlan.Source.label(catchId, true).key(),
        catchLabelShape.getNavigationTextKey());
      assertNotEquals(stepLabelShape.getNavigationTextKey(), catchLabelShape.getNavigationTextKey());
    });
  }

  @Test
  void bindsNestedHeaderAndTextShapesUsingTheirActualComponentIdentity() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      JPanel header = new JPanel();
      JPanel text = new JPanel();
      JPanel other = new JPanel();
      TargetId id = TargetId.step("one");
      Shape textShape = new Shape(text);
      Shape headerShape = new Shape(header).add(textShape);
      Shape unrelated = new Shape(other);
      Shape root = new Shape(0, 0).add(headerShape).add(unrelated);
      Map<Object, TargetId> targets = new IdentityHashMap<>();
      targets.put(header, id);
      Map<Object, String> texts = new IdentityHashMap<>();
      texts.put(text, "source");
      PdfNavigationCollector.bindShapes(root, targets, texts);
      assertEquals(id, headerShape.getNavigationTarget());
      assertEquals("source", textShape.getNavigationTextKey());
      assertNull(textShape.getNavigationTarget());
      assertNull(unrelated.getNavigationTarget());
      assertNull(unrelated.getNavigationTextKey());
    });
  }

  @Test
  void refusesToReadLiveViewsOutsideTheEdt() {
    assertThrows(IllegalStateException.class, () -> PdfNavigationCollector.collect(null, null, null));
  }

  @Test
  void aHiddenCatchContainerHidesLocallyVisibleChildrenWithoutNeedingAWindow() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      JPanel root = new JPanel();
      JPanel catchArea = new JPanel();
      JPanel sequences = new JPanel();
      JPanel heading = new JPanel();
      root.add(catchArea);
      catchArea.add(sequences);
      sequences.add(heading);
      assertTrue(PdfNavigationCollector.visibleWithin(heading));
      assertFalse(heading.isShowing());
      sequences.setVisible(false);
      assertTrue(heading.isVisible());
      assertFalse(PdfNavigationCollector.visibleWithin(heading));
      assertTrue(PdfNavigationCollector.visibleWithin(catchArea));
    });
  }
}

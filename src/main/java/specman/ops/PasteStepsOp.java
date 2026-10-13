package specman.ops;

import specman.EditException;
import specman.clipboard.SpecmanClipboardContent;
import specman.clipboard.SpecmanStepsTransferable;
import specman.clipboard.ExternalPasteChangemarksAdjuster;
import specman.clipboard.InternalPasteChangemarksAdjuster;
import specman.clipboard.PasteChangemarksAdjusterI;
import specman.model.v002.AbstractStepModel_V002;
import specman.model.v002.MetaTagModel_V002;
import specman.model.v002.io.ModelParser_V002;
import specman.model.v002.io.ModelParseException;
import specman.model.v002.io.StepIdRemapper_V002;
import specman.view.AbstractSchrittView;
import specman.view.SchrittSequenzView;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static specman.Specman.editor;
import static specman.view.RelativeStepPosition.After;

public class PasteStepsOp extends AbstractADBLSpecmanOp {

  private final AbstractSchrittView referenceStep;

  public PasteStepsOp(SpecmanOpContext context, AbstractSchrittView referenceStep) {
    super(context);
    this.referenceStep = referenceStep;
  }

  @Override
  void execute() throws EditException {
    try {
      SpecmanClipboardContent content = readClipboard();
      if (content != null) {
        List<AbstractStepModel_V002> stepModels = new ModelParser_V002().parseSteps(content.serializedSteps);
        ensureUniqueStepIds(stepModels);
        boolean sameInstance = editor().instanceId().equals(content.instanceId);
        PasteChangemarksAdjusterI adjuster = sameInstance
            ? new InternalPasteChangemarksAdjuster()
            : new ExternalPasteChangemarksAdjuster();
        stepModels = adjuster.adjust(stepModels, aenderungenVerfolgen());
        addSteps(stepModels);
        toastSkippedMetaTags(stepModels);
      }
    }
    catch (ModelParseException ex) {
      throw new EditException("Clipboard content cannot be pasted as a Specman step:\n" + ex.getMessage());
    }
  }

  private void addSteps(List<AbstractStepModel_V002> stepModels) {
    SchrittSequenzView parent = referenceStep.getParent();
    AbstractSchrittView reference = referenceStep;
    for (AbstractStepModel_V002 stepModel : stepModels) {
      AbstractSchrittView newStep = AbstractSchrittView.baueSchrittViewFrom(parent, stepModel);
      parent.insertStep(newStep, After, reference);
      parent.renumberFollowingSteps(reference);
      newStep.viewsNachinitialisieren();
      newStepPostInit(newStep);
      reference = newStep;
    }
  }

  /** Tags whose meta tag configuration doesn't exist in this diagram are not pasted. Tells the user
   * which configurations were missing, nested steps included. */
  private void toastSkippedMetaTags(List<AbstractStepModel_V002> stepModels) {
    Set<String> unknownConfigNames = new LinkedHashSet<>();
    List<AbstractStepModel_V002> allSteps = new ArrayList<>();
    for (AbstractStepModel_V002 stepModel : stepModels) {
      stepModel.addStepRecursively(allSteps);
    }
    for (AbstractStepModel_V002 step : allSteps) {
      for (MetaTagModel_V002 tag : step.tags) {
        if (editor().findMetaTagConfig(tag.configName) == null) {
          unknownConfigNames.add(tag.configName);
        }
      }
    }
    if (!unknownConfigNames.isEmpty()) {
      showToast("Meta tags skipped",
        "The following meta tags have been skipped\nas they are not configured in this diagram:\n• "
          + String.join("\n• ", unknownConfigNames));
    }
  }

  private void ensureUniqueStepIds(List<AbstractStepModel_V002> models) {
    Set<String> existingIds = editor().listAllSteps().stream()
      .map(AbstractSchrittView::getId)
      .collect(Collectors.toSet());
    StepIdRemapper_V002.remapIfNeeded(models, existingIds);
  }

  private SpecmanClipboardContent readClipboard() {
    try {
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      if (clipboard.isDataFlavorAvailable(SpecmanStepsTransferable.SPECMAN_STEPS_FLAVOR)) {
        return (SpecmanClipboardContent) clipboard.getData(SpecmanStepsTransferable.SPECMAN_STEPS_FLAVOR);
      }
      String text = (String) clipboard.getData(DataFlavor.stringFlavor);
      return (text != null && !text.isBlank()) ? new SpecmanClipboardContent(null, text) : null;
    }
    catch (Exception ex) {
      return null;
    }
  }
}


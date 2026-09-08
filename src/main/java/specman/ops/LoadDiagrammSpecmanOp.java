package specman.ops;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import specman.ChangeSet;
import specman.EditException;
import specman.SpecmanVersion;
import specman.model.ModelEnvelope;
import specman.model.ModelConverterV001V002;
import specman.model.v001.AbstractSchrittModel_V001;
import specman.model.v001.StruktogrammModel_V001;
import specman.model.v002.DiagramModel_V002;
import specman.model.v002.io.ModelRenumberer_V002;
import specman.view.KlappButton;
import specman.view.QuellSchrittView;
import specman.view.SchrittSequenzView;
import specman.view.AbstractSchrittView;

import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import specman.editarea.StepnumberLink;
import specman.editarea.TextEditArea;
import specman.editarea.document.WrappedElement;

public class LoadDiagrammSpecmanOp extends AbstractInitSpecmanOp {

  public LoadDiagrammSpecmanOp(SpecmanOpContext context) {
    super(context);
  }

  public void load() {
    File verzeichnis = (getDiagrammDatei() != null) ? getDiagrammDatei().getParentFile() : null;
    JFileChooser fileChooser = new JFileChooser(verzeichnis);
    fileChooser.setFileFilter(new FileNameExtensionFilter("Nassi Diagramme", "nsd"));
    if (fileChooser.showOpenDialog(getScrollPane()) == JFileChooser.APPROVE_OPTION) {
      loadFromDiagrammOrWorkingCopy(fileChooser.getSelectedFile());
      resetPdfExportChooser();
    }
  }

  public void loadFromDiagrammOrWorkingCopy(File diagramFile) {
    if (!confirmDiscardUnsavedChanges()) {
      return;
    }
    AutoSaveOp.deleteWorkingCopyFor(getDiagrammDatei());
    if (AutoSaveOp.workingCopyExistsFor(diagramFile)) {
      File workingCopy = AutoSaveOp.workingCopyFor(diagramFile);
      int choice = showConfirmDialog(
          "A working copy for '" + diagramFile.getName() + "' is present.\n" +
          "Specman may not have been closed properly in the last session.\n\n" +
          "Model file: " + formatTimestamp(diagramFile) + "\n" +
          "Working copy: " + formatTimestamp(workingCopy) + "\n\n" +
          "Restore from working copy?",
          "Working copy found", JOptionPane.YES_NO_OPTION);
      if (choice == JOptionPane.YES_OPTION) {
        load(workingCopy);
        setDiagrammDatei(diagramFile);
        markAsUnsavedWorkingCopy();
      } else {
        load(diagramFile);
      }
      AutoSaveOp.deleteWorkingCopyFor(diagramFile);
    } else {
      load(diagramFile);
    }
    addRecentFile(diagramFile);
    if (!hasUnsavedChanges()) {
      long wcTimestamp = AutoSaveOp.createWorkingCopyFor(diagramFile);
      context().notifyWorkingCopyInitialized(wcTimestamp);
    }
  }

  private static String formatTimestamp(File file) {
    LocalDateTime dt = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(file.lastModified()), ZoneId.systemDefault());
    return dt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
  }

  private void load(File diagramFile) {
    try {
      loadOrThrow(diagramFile);
    }
    catch (EditException | IOException e) {
      displayException(e);
    }
  }

  void loadOrThrow(File diagramFile) throws EditException, IOException {
    clearFocusHistory();
    setChangeModeEnabled(false);
    dropWelcomeMessage();
    setDiagrammDatei(diagramFile);
    ModelEnvelope meta = readMeta(diagramFile);
    verifyModelTypeAndSpecmanVersion(meta);
    ModelEnvelope envelope = readFull(diagramFile, meta.modelType);
    applyEnvelope(envelope);
  }

  void loadOrThrow(byte[] snapshot) throws EditException, IOException {
    clearFocusHistory();
    setChangeModeEnabled(false);
    dropWelcomeMessage();
    // setDiagrammDatei intentionally omitted — caller manages the file reference
    ModelEnvelope meta = readMeta(snapshot);
    verifyModelTypeAndSpecmanVersion(meta);
    ModelEnvelope envelope = readFull(snapshot, meta.modelType);
    applyEnvelope(envelope);
  }

  private void applyEnvelope(ModelEnvelope envelope) throws EditException {
    DiagramModel_V002 model = resolveModel(envelope);

    ChangeSet changeSet = ChangeSet.fromName(model.changeSetName);
    if (changeSet != null) {
      context.updateChangeSet(changeSet);
    }
    setZoomFaktor(model.zoomFactor);
    zoomFaktorAnzeigeAktualisieren(model.zoomFactor);
    KlappButton.scaleIcons(model.zoomFactor, 0);
    setDiagrammbreite(model.width);
    getIntro().setEditorContent(model.intro);
    getOutro().setEditorContent(model.outro);
    setPdfExportOptions(model.pdfExportOptions);
    setDiagrammName(model.name);
    setHauptSequenz(new SchrittSequenzView(null, model.mainSequence));

    hauptSequenzInitialisieren();
    getHauptSequenz().renummerieren();
    rewriteStaleStepNumberLinks(model);
    // quellZielZuweisung: step references handled via UUID in a future step
    getHauptSequenz().viewsNachinitialisieren();
    getIntro().viewsNachinitialisieren();
    getIntro().registerAllExistingStepnumbers();
    getOutro().viewsNachinitialisieren();
    getOutro().registerAllExistingStepnumbers();
    setChangeModeEnabled(model.changeModeEnabled);
    discardAllUndoEdits();
  }

  private DiagramModel_V002 resolveModel(ModelEnvelope envelope) {
    if (envelope.model instanceof DiagramModel_V002) {
      return (DiagramModel_V002) envelope.model;
    }
    return ModelConverterV001V002.convert((StruktogrammModel_V001) envelope.model);
  }

  private void verifyModelTypeAndSpecmanVersion(ModelEnvelope envelope) throws EditException {
    boolean isV1 = StruktogrammModel_V001.class.getName().equals(envelope.modelType);
    boolean isV2 = DiagramModel_V002.class.getName().equals(envelope.modelType);
    if (!isV1 && !isV2) {
      throw new EditException("The selected file does not contain an actogramm model or a model of an unsupported Specman version " + envelope.specmanVersion);
    }
    String compatibilityVersionPrefix = SpecmanVersion.getCompatibilityVersionPrefix();
    if (!envelope.specmanVersion.startsWith(compatibilityVersionPrefix)) {
      showMessage("The selected file was created from Specman version " + envelope.specmanVersion + ". " +
        "The current version is " + SpecmanVersion.getVersion() + ". The file format is compatible. However, " +
        "files being edited with a newer version should not be edited with older versions afterwards. " +
        "This may cause the loss of meta information");
    }
  }

  /** Reads only the meta information from the envelope without making any
   * assumptions about the structure of the actual nested model representation.
   * This is important because model type and Specman version may have an impact
   * about how exactly to read the rest. */
  private ModelEnvelope readMeta(File diagramFile) throws IOException {
    ObjectMapper metaMapper = new ObjectMapper();
    JsonNode root = metaMapper.readTree(diagramFile);
    return extractMeta(root);
  }

  private ModelEnvelope readMeta(byte[] data) throws IOException {
    ObjectMapper metaMapper = new ObjectMapper();
    JsonNode root = metaMapper.readTree(data);
    return extractMeta(root);
  }

  private ModelEnvelope extractMeta(JsonNode root) {
    ModelEnvelope meta = new ModelEnvelope();
    meta.modelType = root.path("modelType").asText(null);
    meta.specmanVersion = root.path("specmanVersion").asText(null);
    return meta;
  }

  private ModelEnvelope readFull(File diagramFile, String modelType) throws IOException {
    ObjectMapper mapper = buildMapper(modelType);
    return mapper.readValue(diagramFile, ModelEnvelope.class);
  }

  private ModelEnvelope readFull(byte[] data, String modelType) throws IOException {
    ObjectMapper mapper = buildMapper(modelType);
    return mapper.readValue(data, ModelEnvelope.class);
  }

  private ObjectMapper buildMapper(String modelType) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    if (StruktogrammModel_V001.class.getName().equals(modelType)) {
      mapper.enableDefaultTyping();
    }
    return mapper;
  }

  /** Rewrites stale step-number cross-references after loading a V2 model that was edited outside
   * Specman, especially by an AI agent. After an agent reorders or inserts steps, the step numbers
   * embedded as Steplink-styled text runs in the diagram content may no longer match the numbers
   * that {@code renummerieren()} just assigned. This method compares the saved step numbers (stored
   * in each step's {@code stepNumber} field) against the freshly computed numbers from the view and
   * rewrites every affected Steplink run in place, before {@code viewsNachinitialisieren()} rebuilds
   * the reference graph via {@code registerAllExistingStepnumbers()}. */
  private void rewriteStaleStepNumberLinks(DiagramModel_V002 model) {
    Map<String, String> savedNumbers = ModelRenumberer_V002.collectNumbers(model.mainSequence);
    if (!savedNumbers.isEmpty()) {
      Map<String, String> computedIndex = getHauptSequenz().buildStepNumberIndex();
      Map<String, String> changedNumbers = queryChangedNumbers(savedNumbers, computedIndex);
      if (!changedNumbers.isEmpty()) {
        List<TextEditArea> allAreas = collectAllTextAreas();
        for (TextEditArea area : allAreas) {
          remapStepNumberLinksInArea(area, changedNumbers);
        }
      }
    }
  }

  private List<TextEditArea> collectAllTextAreas() {
    List<TextEditArea> allAreas = new ArrayList<>();
    allAreas.addAll(getIntro().getTextAreas());
    allAreas.addAll(getHauptSequenz().getTextAreas());
    allAreas.addAll(getOutro().getTextAreas());
    return allAreas;
  }

  private Map<String, String> queryChangedNumbers(Map<String, String> savedNumbers, Map<String, String> computedIndex) {
    Map<String, String> changedNumbers = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : computedIndex.entrySet()) {
      String savedNumber = savedNumbers.get(entry.getKey());
      if (savedNumber != null && !savedNumber.equals(entry.getValue())) {
        changedNumbers.put(savedNumber, entry.getValue());
      }
    }
    return changedNumbers;
  }

  private void remapStepNumberLinksInArea(TextEditArea area, Map<String, String> changedNumbers) {
    List<WrappedElement> links = area.findStepnumberLinks();
    List<WrappedElement> elementsToUpdate = new ArrayList<>();
    List<String> newIDs = new ArrayList<>();
    for (WrappedElement link : links) {
      String currentID = area.getStepnumberLinkIDFromElement(link.getStartOffset(), link.getEndOffset());
      if (!StepnumberLink.isStepnumberLinkDefect(currentID)) {
        String newID = changedNumbers.get(currentID);
        if (newID != null) {
          elementsToUpdate.add(link);
          newIDs.add(newID);
        }
      }
    }
    // Apply back-to-front so earlier offsets are not shifted by later replacements
    for (int i = elementsToUpdate.size() - 1; i >= 0; i--) {
      area.replaceStepnumberLinkElement(elementsToUpdate.get(i), newIDs.get(i));
    }
  }

  private void quellZielZuweisung(List<AbstractSchrittModel_V001> allModelSteps) {
    for (AbstractSchrittModel_V001 modelStep : allModelSteps) {
      if (modelStep.quellschrittID != null) {
        AbstractSchrittView zielschritt = getHauptSequenz().findeSchrittZuId(modelStep.id);
        if (zielschritt instanceof QuellSchrittView) {
          continue;
        }
        QuellSchrittView quellSchritt = (QuellSchrittView) getHauptSequenz().findeSchrittZuId(modelStep.quellschrittID);
        zielschritt.setQuellschrittUDBL(quellSchritt);
        quellSchritt.setZielschritt(zielschritt);
      }
    }
  }

}

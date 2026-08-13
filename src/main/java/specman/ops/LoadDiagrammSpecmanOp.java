package specman.ops;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import specman.ChangeSet;
import specman.EditException;
import specman.SpecmanVersion;
import specman.model.ModelEnvelope;
import specman.model.v001.AbstractSchrittModel_V001;
import specman.model.v001.StruktogrammModel_V001;
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
import java.util.List;

public class LoadDiagrammSpecmanOp extends AbstractSpecmanOp {

  public LoadDiagrammSpecmanOp(SpecmanOpContext context) {
    super(context);
  }

  public void load() {
    File verzeichnis = (getDiagrammDatei() != null) ? getDiagrammDatei().getParentFile() : null;
    JFileChooser fileChooser = new JFileChooser(verzeichnis);
    fileChooser.setFileFilter(new FileNameExtensionFilter("Nassi Diagramme", "nsd"));
    if (fileChooser.showOpenDialog(getScrollPane()) == JFileChooser.APPROVE_OPTION) {
      loadFromDiagrammOrBackup(fileChooser.getSelectedFile());
      resetPdfExportChooser();
    }
  }

  public void loadFromDiagrammOrBackup(File diagramFile) {
    if (AutoSave.backupExistsFor(diagramFile)) {
      File backupFile = AutoSave.backupFileFor(diagramFile);
      int choice = showConfirmDialog(
          "A backup for '" + diagramFile.getName() + "' is present.\n" +
          "Specman may not have been closed properly in the last session.\n\n" +
          "Model file: " + formatTimestamp(diagramFile) + "\n" +
          "Backup file: " + formatTimestamp(backupFile) + "\n\n" +
          "Restore from backup?",
          "Backup found", JOptionPane.YES_NO_OPTION);
      if (choice == JOptionPane.YES_OPTION) {
        load(backupFile);
        setDiagrammDatei(diagramFile);
        markAsUnsavedBackup();
      } else {
        load(diagramFile);
      }
      AutoSave.deleteBackupFor(diagramFile);
    } else {
      load(diagramFile);
    }
    addRecentFile(diagramFile);
  }

  private static String formatTimestamp(File file) {
    LocalDateTime dt = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(file.lastModified()), ZoneId.systemDefault());
    return dt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
  }

  private void load(File diagramFile) {
    try {
      clearFocusHistory();
      setChangeModeEnabled(false);
      dropWelcomeMessage();
      setDiagrammDatei(diagramFile);

      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
      objectMapper.enableDefaultTyping();
      ModelEnvelope envelope = objectMapper.readValue(getDiagrammDatei(), ModelEnvelope.class);
      verifyModelTypeAndSpecmanVersion(envelope);
      StruktogrammModel_V001 model = (StruktogrammModel_V001) envelope.model;

      ChangeSet changeSet = ChangeSet.fromName(model.changeSetName);
      if (changeSet != null) {
        context.updateChangeSet(changeSet);
      }
      setZoomFaktor(model.zoomFaktor);
      zoomFaktorAnzeigeAktualisieren(model.zoomFaktor);
      KlappButton.scaleIcons(model.zoomFaktor, 0);
      setDiagrammbreite(model.breite);
      getIntro().setEditorContent(model.intro);
      getOutro().setEditorContent(model.outro);
      setPdfExportOptions(model.pdfExportOptions);
      setDiagrammName(model.name);
      setHauptSequenz(new SchrittSequenzView(null, model.hauptSequenz));

      hauptSequenzInitialisieren();
      quellZielZuweisung(model.queryAllSteps());
      getHauptSequenz().viewsNachinitialisieren();
      getIntro().viewsNachinitialisieren();
      getIntro().registerAllExistingStepnumbers();
      getOutro().viewsNachinitialisieren();
      getOutro().registerAllExistingStepnumbers();
      setChangeModeEnabled(model.changeModeenabled);
      discardAllUndoEdits();
    }
    catch (EditException | IOException e) {
      displayException(e);
    }
  }

  private void verifyModelTypeAndSpecmanVersion(ModelEnvelope envelope) throws EditException {
    if (!StruktogrammModel_V001.class.getName().equals(envelope.modelType)) {
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

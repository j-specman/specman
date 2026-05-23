package specman;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;

class LoadDiagrammSpecmanOp extends AbstractSpecmanOp {

  LoadDiagrammSpecmanOp(Specman specman) {
    super(specman);
  }

  void laden() {
    File verzeichnis = (specman.diagrammDatei != null) ? specman.diagrammDatei.getParentFile() : null;
    JFileChooser fileChooser = new JFileChooser(verzeichnis);
    fileChooser.setFileFilter(new FileNameExtensionFilter("Nassi Diagramme", "nsd"));
    if (fileChooser.showOpenDialog(specman) == JFileChooser.APPROVE_OPTION) {
      laden(fileChooser.getSelectedFile());
      specman.pdfExportChooser = null;
    }
  }

  void laden(File diagramFile) {
    try {
      specman.focusHistory.clear();
      specman.diagramToolBar.setChangeModeEnabled(false);
      specman.dropWelcomeMessage();
      specman.setDiagrammDatei(diagramFile);

      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.enableDefaultTyping();
      ModelEnvelope envelope = objectMapper.readValue(specman.diagrammDatei, ModelEnvelope.class);
      StruktogrammModel_V001 model = (StruktogrammModel_V001) envelope.model;

      specman.zoomFaktor = model.zoomFaktor;
      specman.zoomFaktorAnzeigeAktualisieren(specman.zoomFaktor);
      KlappButton.scaleIcons(specman.zoomFaktor, 0);
      specman.diagrammbreite = model.breite;
      specman.intro.setEditorContent(model.intro);
      specman.outro.setEditorContent(model.outro);
      specman.pdfExportOptions = model.pdfExportOptions;
      specman.setName(model.name);
      specman.hauptSequenz = new SchrittSequenzView(specman, null, model.hauptSequenz);

      specman.hauptSequenzInitialisieren();
      quellZielZuweisung(model.queryAllSteps());
      specman.hauptSequenz.viewsNachinitialisieren();
      specman.intro.viewsNachinitialisieren();
      specman.intro.registerAllExistingStepnumbers();
      specman.outro.viewsNachinitialisieren();
      specman.outro.registerAllExistingStepnumbers();
      specman.diagramToolBar.setChangeModeEnabled(model.changeModeenabled);
      specman.recentFiles.add(diagramFile);
      specman.undoManager.discardAllEdits();
    }
    catch (IOException e) {
      specman.displayException(e);
    }
  }

  private void quellZielZuweisung(List<AbstractSchrittModel_V001> allModelSteps) {
    for (AbstractSchrittModel_V001 modelStep : allModelSteps) {
      if (modelStep.quellschrittID != null) {
        AbstractSchrittView zielschritt = specman.hauptSequenz.findeSchrittZuId(modelStep.id);
        if (zielschritt instanceof QuellSchrittView) {
          continue;
        }
        QuellSchrittView quellSchritt = (QuellSchrittView) specman.hauptSequenz.findeSchrittZuId(modelStep.quellschrittID);
        zielschritt.setQuellschrittUDBL(quellSchritt);
        quellSchritt.setZielschritt(zielschritt);
      }
    }
  }

}

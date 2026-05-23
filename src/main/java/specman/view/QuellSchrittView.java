package specman.view;

import specman.Aenderungsart;
import specman.EditorI;
import specman.SchrittID;
import specman.model.v001.EditorContentModel_V001;
import specman.model.v001.QuellSchrittModel_V001;
import specman.undo.props.UDBL;

import javax.swing.*;
import java.awt.*;

import static specman.graphics.Styles.AENDERUNGSFARBE;

public class QuellSchrittView extends AbstractSchrittView {

    protected AbstractSchrittView zielschritt;

    public QuellSchrittView(EditorI editor, SchrittSequenzView parent, SchrittID id) {
        //TODO JL: der "." sorgt für eine Mindesthöhe des Quellschritts. Muss noch gesäubert werden.
        //Die Höhe des Schrittnummer-Labels sollte die Höhe bestimmen.
        super(editor, parent, new EditorContentModel_V001(".", Aenderungsart.Quellschritt), id, Aenderungsart.Quellschritt);
        setQuellStil();
        setBackgroundUDBL(AENDERUNGSFARBE.panelColor);
    }

    @Override
    public JComponent getDecoratedComponent() { return decorated(editContainer); }

    public QuellSchrittView(EditorI editor, SchrittSequenzView parent, QuellSchrittModel_V001 model) {
        super(editor, parent, model.inhalt, model.id, model.aenderungsart);
        setBackgroundUDBL(new Color(model.farbe));
    }

    @Override
    public QuellSchrittModel_V001 generiereModel(boolean formatierterText) {
        QuellSchrittModel_V001 model = new QuellSchrittModel_V001(
            id,
            getEditorContent(formatierterText),
            getBackground().getRGB(),
            aenderungsart,
            getZielschrittID(),
            getDecorated()
        );
        return model;
    }

    @Override
    public JComponent getPanel() { return editContainer; }

    public SchrittID getZielschrittID() {
      return zielschritt != null ? zielschritt.getId() : null;
    }

    public void setQuellStil() {
      editContainer.setQuellStil(getZielschrittID());
      setAenderungsart(Aenderungsart.Quellschritt);
    }

    @Override
    public void setId(SchrittID id) {
      SchrittID oldId = getId();
      super.setId(id);
      if (zielschritt != null && !oldId.equals(id)) {
        zielschritt.resyncStepnumberStyleUDBL();
      }
    }

    public void setZielschrittUDBL(AbstractSchrittView zielschritt) { UDBL.setZielschrittUDBL(this, zielschritt); }
    public void setZielschritt(AbstractSchrittView zielschritt) { this.zielschritt = zielschritt; }
    public AbstractSchrittView getZielschritt() { return zielschritt; }
}

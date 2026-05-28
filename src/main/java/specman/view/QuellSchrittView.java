package specman.view;

import specman.Aenderungsart;
import specman.ChangeInfo;
import specman.EditorI;
import static specman.ChangeSet.changeset;
import static specman.util.ObjectUtils.nvl;
import specman.SchrittID;
import specman.model.v001.EditorContentModel_V001;
import specman.model.v001.QuellSchrittModel_V001;
import specman.undo.props.UDBL;

import javax.swing.*;
import java.awt.*;

public class QuellSchrittView extends AbstractSchrittView {

    protected AbstractSchrittView zielschritt;

    public QuellSchrittView(SchrittSequenzView parent, SchrittID id) {
        //TODO JL: der "." sorgt für eine Mindesthöhe des Quellschritts. Muss noch gesäubert werden.
        //Die Höhe des Schrittnummer-Labels sollte die Höhe bestimmen.
        super(parent, new EditorContentModel_V001(".", new ChangeInfo(Aenderungsart.Quellschritt, changeset())), id, new ChangeInfo(Aenderungsart.Quellschritt, changeset()));
        setQuellStil();
        setBackgroundUDBL(changeset().panelColor());
    }

    public QuellSchrittView(SchrittSequenzView parent, QuellSchrittModel_V001 model) {
      super(parent, model.inhalt, model.id, ChangeInfo.fromModel(model.changeInfo, model.aenderungsart));
      setBackgroundUDBL(new Color(model.farbe));
    }

    @Override
    public JComponent getDecoratedComponent() { return decorated(editContainer); }

    @Override
    public QuellSchrittModel_V001 generiereModel(boolean formatierterText) {
        QuellSchrittModel_V001 model = new QuellSchrittModel_V001(
            id,
            getEditorContent(formatierterText),
            getBackground().getRGB(),
            changeInfo,
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
      setChangeInfo(changeInfo.toQuellschritt());
      editContainer.setQuellStil(getZielschrittID(), changeInfo.changeSet());
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

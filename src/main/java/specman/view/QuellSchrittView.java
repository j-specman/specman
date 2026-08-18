package specman.view;

import specman.Aenderungsart;
import specman.ChangeInfo;

import static specman.ChangeSet.changeset;
import static specman.util.ObjectUtils.nvl;
import specman.StepNumber;
import specman.model.v002.EditorContentModel_V002;
import specman.model.v002.SourceStepModel_V002;
import specman.undo.props.UDBL;

import javax.swing.*;
import java.awt.*;

public class QuellSchrittView extends AbstractSchrittView {

    protected AbstractSchrittView zielschritt;

    public QuellSchrittView(SchrittSequenzView parent, StepNumber id) {
        //TODO JL: der "." sorgt für eine Mindesthöhe des Quellschritts. Muss noch gesäubert werden.
        //Die Höhe des Schrittnummer-Labels sollte die Höhe bestimmen.
        super(parent, new EditorContentModel_V002(".", new ChangeInfo(Aenderungsart.Quellschritt, changeset())), id, new ChangeInfo(Aenderungsart.Quellschritt, changeset()));
        setQuellStil();
        setBackgroundUDBL(changeset().panelColor());
    }

    public QuellSchrittView(SchrittSequenzView parent, SourceStepModel_V002 model) {
      super(parent, model.content, model.id, model.changeInfo != null ? model.changeInfo.toChangeInfo() : ChangeInfo.UNTRACKED);
      setBackgroundUDBL(new Color(model.color));
      this.id = model.id;
    }

    @Override
    public JComponent getDecoratedComponent() { return decorated(editContainer); }

    @Override
    public SourceStepModel_V002 generiereModel(boolean formatierterText) {
        return new SourceStepModel_V002(
          id,
            getEditorContent(formatierterText),
            getBackground().getRGB(),
            changeInfo,
            null,
            getDecorated()
        );
    }

    @Override
    public JComponent getPanel() { return editContainer; }

    public StepNumber getZielschrittID() {
      return zielschritt != null ? zielschritt.getNumber() : null;
    }

    public void setQuellStil() {
      setChangeInfo(changeInfo.toQuellschritt());
      editContainer.setQuellStil(getZielschrittID(), changeInfo.changeSet());
    }

    @Override
    public void setNumber(StepNumber number) {
      StepNumber oldId = getNumber();
      super.setNumber(number);
      if (zielschritt != null && !oldId.equals(number)) {
        zielschritt.resyncStepnumberStyleUDBL();
      }
    }

    public void setZielschrittUDBL(AbstractSchrittView zielschritt) { UDBL.setZielschrittUDBL(this, zielschritt); }
    public void setZielschritt(AbstractSchrittView zielschritt) { this.zielschritt = zielschritt; }
    public AbstractSchrittView getZielschritt() { return zielschritt; }
}

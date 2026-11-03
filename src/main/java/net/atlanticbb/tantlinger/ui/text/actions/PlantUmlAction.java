package net.atlanticbb.tantlinger.ui.text.actions;

import static specman.i18n.Messages.i18n;
import static specman.Specman.editor;

import javax.swing.JEditorPane;
import java.awt.event.ActionEvent;
import java.io.Serial;

public class PlantUmlAction extends BasicEditAction {
    @Serial
    private static final long serialVersionUID = 1L;

    public PlantUmlAction() {
        super(i18n("plantuml.insert"));
        this.putValue("ShortDescription", this.getValue("Name"));
    }

    @Override
    protected void doEdit(ActionEvent actionEvent, JEditorPane jEditorPane) {
        editor().addPlantUmlImage();
    }
}

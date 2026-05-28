package net.atlanticbb.tantlinger.ui.text.actions;

import net.atlanticbb.tantlinger.ui.UIUtils;
import specman.Specman;
import static specman.Specman.editor;

import javax.swing.JEditorPane;
import java.awt.event.ActionEvent;
import java.io.Serial;

public class ImageAction extends BasicEditAction {
    @Serial
    private static final long serialVersionUID = 1L;

    public ImageAction() {
        super(i18n.str("image_"));
        this.putValue("SmallIcon", UIUtils.getIcon("resources/images/x16/", "image.png"));
        this.putValue("ShortDescription", this.getValue("Name"));
    }

    @Override
    protected void doEdit(ActionEvent actionEvent, JEditorPane jEditorPane) {
        editor().addImageViaFileChooser();
    }
}
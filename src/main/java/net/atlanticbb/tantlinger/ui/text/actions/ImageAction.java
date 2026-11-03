package net.atlanticbb.tantlinger.ui.text.actions;

import specman.graphics.IconReader;
import specman.graphics.SpecmanIcons;

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
        this.putValue("SmallIcon", IconReader.readSvgIcon(SpecmanIcons.IMAGE, SpecmanIcons.SIZE_SMALL));
        this.putValue("ShortDescription", this.getValue("Name"));
    }

    @Override
    protected void doEdit(ActionEvent actionEvent, JEditorPane jEditorPane) {
        editor().addImageViaFileChooser();
    }
}
package specman.view;

import specman.ChangeInfo;
import specman.StepNumber;
import specman.model.v002.EditorContentModel_V002;
import specman.model.v002.DoWhileStepModel_V002;

/**
 * Spezielle Anzeige einer While-Schleife mit einem abschließenden unteren Balken.
 * Im Nassi-Shneiderman-Standard steht in dieser Form unten noch einmal die Prüfbedingung
 * drin, aber das lassen wir mal weg. Das sieht komisch aus.
 *
 * @author less02
 */
public class WhileWhileSchrittView extends SchleifenSchrittView {

	public WhileWhileSchrittView(SchrittSequenzView parent, EditorContentModel_V002 initialerText, StepNumber id, ChangeInfo changeInfo, boolean withDefaultContent) {
		super(parent, initialerText, id, changeInfo, true);
		if (withDefaultContent) {
			initWiederholsequenz(einschrittigeInitialsequenz(id.naechsteEbene(), changeInfo));
		}
	}

	public WhileWhileSchrittView(SchrittSequenzView parent, EditorContentModel_V002 initialerText, StepNumber id, ChangeInfo changeInfo) {
		this(parent, initialerText, id, changeInfo, true);
	}

	public WhileWhileSchrittView(SchrittSequenzView parent, DoWhileStepModel_V002 model) {
		super(parent, model, true);
	}

	/** The bottom bar visually occupies the ascent-to-parent drop zone, so it handles that drop itself. */
	@Override public boolean dropTargetSuppressesAscentToParent() { return true; }

	@Override
	public DoWhileStepModel_V002 generiereModel(boolean formatierterText) {
		return new DoWhileStepModel_V002(
			id,
			getEditorContent(formatierterText),
			getBackground().getRGB(),
			changeInfo,
			klappen.isSelected(),
			wiederholSequenz.generiereSchrittSequenzModel(formatierterText),
			linkerBalken.getWidth(),
			null,
			getDecorated());
	}

}
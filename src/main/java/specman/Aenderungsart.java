package specman;

import java.awt.*;

import static specman.ChangeSet.changeset;
import static specman.graphics.Styles.BACKGROUND_COLOR_STANDARD;

public enum Aenderungsart {
	Untracked, Hinzugefuegt, Geloescht, Quellschritt, Zielschritt;

	public Color toBackgroundColor() {
		return (this == Hinzugefuegt || this == Geloescht)
			? changeset().panelColor()
			: BACKGROUND_COLOR_STANDARD;
	}

	public boolean istAenderung() { return this != Untracked; }

	public int asNumChanges() { return istAenderung() ? 1 : 0; }
}

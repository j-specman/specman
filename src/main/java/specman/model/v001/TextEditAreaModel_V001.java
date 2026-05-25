package specman.model.v001;

import com.fasterxml.jackson.annotation.JsonIgnore;
import specman.Aenderungsart;
import specman.ChangeInfo;

import java.util.ArrayList;
import java.util.List;

public class TextEditAreaModel_V001 extends AbstractEditAreaModel_V001 {
	public final String text;
	public final String plainText;
	public final List<Markup_V001> markups;
	public final Aenderungsart aenderungsart; // kept for backwards compatibility
	public final ChangeInfo_V001 changeInfo;

	@Deprecated public TextEditAreaModel_V001() { // For Jackson only
		text = null;
		plainText = null;
		markups = null;
		aenderungsart = null;
		changeInfo = null;
	}

	public TextEditAreaModel_V001(String text) { this(text, text, new ArrayList<>(), (ChangeInfo_V001) null); }

	public TextEditAreaModel_V001(String text, String plainText, List<Markup_V001> markups, ChangeInfo changeInfo) {
		this.text = text;
		this.plainText = plainText;
		this.markups = markups;
		this.aenderungsart = null;
		this.changeInfo = new ChangeInfo_V001(changeInfo);
	}

	public TextEditAreaModel_V001(String text, String plainText, List<Markup_V001> markups, ChangeInfo_V001 changeInfo) {
		this.text = text;
		this.plainText = plainText;
		this.markups = markups;
		this.aenderungsart = null;
		this.changeInfo = changeInfo;
	}

  @JsonIgnore
  public boolean isEmpty() { return text.isEmpty(); }
}

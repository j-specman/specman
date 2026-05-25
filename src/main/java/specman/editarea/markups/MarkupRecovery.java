package specman.editarea.markups;

import specman.editarea.document.WrappedDocumentI;
import specman.editarea.document.WrappedPosition;
import specman.model.v001.Markup_V001;

import java.util.List;
import java.util.Objects;

import static specman.editarea.markups.CharType.NonWhitespace;

public class MarkupRecovery {
  final WrappedDocumentI target;
  final MarkedCharSequence source;
  final TextMarkup[] marksPerChar;
  WrappedPosition targetProgress;
  Integer sourceProgress;

  public MarkupRecovery(WrappedDocumentI target, MarkedCharSequence source) {
    this.target = target;
    this.source = source;
    this.marksPerChar = new TextMarkup[target.getLength()];
    this.targetProgress = target.fromModel(0);
    this.sourceProgress = 0;
  }

  public List<Markup_V001> recover() {
    WrappedPosition nextTargetVisibleCharSeqStart;
    Integer nextSourceVisibleCharSeqStart;
    do {
      nextTargetVisibleCharSeqStart = findRight(targetProgress, NonWhitespace);
      nextSourceVisibleCharSeqStart = source.findRight(sourceProgress, NonWhitespace);
      recoverSkippedWhitespaceMarks(nextTargetVisibleCharSeqStart, nextSourceVisibleCharSeqStart);
      recoverVisibleCharMarks(nextTargetVisibleCharSeqStart, nextSourceVisibleCharSeqStart);
    }
    while(nextTargetVisibleCharSeqStart != null);

    return assembleMarkupsFromMarkupsPerChar();
  }

  private void recoverVisibleCharMarks(WrappedPosition targetPos, Integer sourcePos) {
    if (targetPos == null) {
      return;
    }
    do {
      marksPerChar[targetPos.toModel()] = source.type(sourcePos);
      targetPos = targetPos.inc();
      sourcePos++;
    }
    while(targetPos.exists() && sourcePos < source.size() && source.isVisibleChar(sourcePos));
    targetProgress = targetPos;
    sourceProgress = sourcePos;
  }

  /** TODO: Skipped whitespaces might also have changemarks, which need to be recovered. */
  private void recoverSkippedWhitespaceMarks(WrappedPosition targetVisibleCharSeqStart, Integer sourceVisibleCharSeqStart) {
    if (targetVisibleCharSeqStart == null || targetVisibleCharSeqStart.equals(targetProgress)) {
      return;
    }
    int targetWhitespaceLen = targetWhitespaceLen(targetVisibleCharSeqStart);
    int sourceWhitespaceLen = sourceWhitespaceLen(sourceVisibleCharSeqStart);
    if (targetWhitespaceLen == sourceWhitespaceLen) {
      WrappedPosition targetWhitespacePos = targetProgress;
      int sourceWhitespacePos = sourceProgress;
      for (int i = 0; i < targetWhitespaceLen; i++) {
        marksPerChar[targetWhitespacePos.toModel()] = source.type(sourceWhitespacePos);
        targetWhitespacePos = targetWhitespacePos.inc();
        sourceWhitespacePos++;
      }
    }

  }

  private int sourceWhitespaceLen(Integer sourceVisibleCharSeqStart) {
    return sourceVisibleCharSeqStart - sourceProgress;
  }

  private int targetWhitespaceLen(WrappedPosition targetVisibleCharSeqStart) {
    return targetVisibleCharSeqStart.distance(targetProgress);
  }

  private WrappedPosition findRight(WrappedPosition pos, CharType charType) {
    while (pos.exists()) {
      if (charType.at(pos)) {
        return pos;
      }
      pos = pos.inc();
    }
    return null;
  }

  private List<Markup_V001> assembleMarkupsFromMarkupsPerChar() {
    List<Markup_V001> changemarks = new java.util.ArrayList<>();
    Integer markStart = null;
    TextMarkup lastMarkup = null;
    for (int i = 0; i < marksPerChar.length; i++) {
      TextMarkup currentType = marksPerChar[i];
      if (!Objects.equals(currentType, lastMarkup)) {
        if (markStart != null) {
          changemarks.add(new Markup_V001(markStart, i - 1, lastMarkup));
          markStart = null;
        }
        if (currentType != null) {
          markStart = i;
        }
        lastMarkup = currentType;
      }
    }
    if (markStart != null) {
      changemarks.add(new Markup_V001(markStart, marksPerChar.length - 1, lastMarkup));
    }
    return changemarks;
  }
}

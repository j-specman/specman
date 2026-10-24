package net.atlanticbb.tantlinger.shef;

import specman.graphics.IconReader;
import specman.graphics.SpecmanIcons;

import net.atlanticbb.tantlinger.i18n.I18n;
import net.atlanticbb.tantlinger.ui.DefaultAction;
import net.atlanticbb.tantlinger.ui.ShefIconInstaller;
import net.atlanticbb.tantlinger.ui.text.CompoundUndoManager;
import net.atlanticbb.tantlinger.ui.text.HTMLUtils;
import net.atlanticbb.tantlinger.ui.text.WysiwygHTMLEditorKit;
import net.atlanticbb.tantlinger.ui.text.actions.*;
import net.atlanticbb.tantlinger.ui.text.dialogs.SpecmanHTMLFontDialog.FontFamily;
import org.bushe.swing.action.ActionList;
import org.bushe.swing.action.ActionManager;
import org.bushe.swing.action.ActionUIFactory;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.Document;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.undo.UndoManager;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serial;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

/**
 * This class was extracted from the SHEF library and modified / extended for Specman.
 * Unfortunately it was not really designed for modification, and we didn't find an elegant
 * object-oriented approach for the following modifications:
 * <ul>
 *   <li>Additional toolbar icon for the creation of step links</li>
 *   <li>Differing functionality for the creation of tables, list items and images</li>
 *   <li>Recovery of markups on change of paragraph type</li>
 * </ul>
 * The following parts of the original SHEF class have been removed on purpose:
 * <ul>
 *   <li>The HTML source editor tab (including syntax gutter, source popup menu and the
 *   wysiwyg/source synchronization with its getText/setText), and the pane's own embedded
 *   wysiwyg editor and popup menu. Specman never offers editing the HTML source, and the embedded
 *   editors were never displayed, because Specman only uses this class for its menus, its format
 *   toolbar and for {@link #instrumentWysEditor(JEditorPane, String, Integer)}.</li>
 * </ul>
 * The removal is important for headless testing: The source editor's {@code SyntaxTextPane} creates
 * a custom cursor in its constructor, which throws a {@code HeadlessException}. With the source
 * editor gone, this class can be constructed without a display.
 * The SHEF library in the project's local repository does not contain the class any more
 * to avoid selection of the wrong implementation in a Specman distribution jar.
 */
public class HTMLEditorPane extends JPanel
{
  @Serial private static final long serialVersionUID = 1L;

  private static final I18n i18n = I18n.getInstance("net.atlanticbb.tantlinger.shef");

  private JEditorPane focusedEditor;
  private JComboBox fontFamilyCombo;
  private JComboBox paragraphCombo;
  private final List<Component> textGroupComponents = new ArrayList<>();
  private final List<Component> insertGroupComponents = new ArrayList<>();

  private JMenu editMenu;
  private JMenu formatMenu;
  private JMenu insertMenu;

  private ActionList actionList;
  /** Insert buttons that need a focused text area. The state lives on the buttons, because the SHEF actions
   * reset their own enabled state on every caret or focus event.
   */
  private final List<AbstractButton> textAreaButtons = new ArrayList<>();
  private boolean textAreaActionsEnabled = true;

  private FocusListener focusHandler = new FocusHandler();
  private ActionListener fontChangeHandler = new FontChangeHandler();
  private ActionListener paragraphComboHandler = new ParagraphComboHandler();
  private CaretListener caretHandler = new CaretHandler();

  private UndoManager centralUndoManager;

  public HTMLEditorPane(UndoManager centralUndoManager)
  {
    this.centralUndoManager = centralUndoManager;
    CompoundUndoManager.setCentralUndoManager(centralUndoManager);
    initUI();
  }

  private void initUI()
  {
    createEditorActions();
  }

  /** Controls of the toolbar group "Text": paragraph, font, size, color, inline styles, alignment.
   * These components are part of the SHEF-Editor.
   */
  public List<Component> getTextGroupComponents() {
    return List.copyOf(textGroupComponents);
  }

  /** Controls of the toolbar group "Insert": lists, link, image, table, step number link.
   * These components are part of the SHEF-Editor.
   */
  public List<Component> getInsertGroupComponents() {
    return List.copyOf(insertGroupComponents);
  }

  public JMenu getEditMenu()
  {
    return editMenu;
  }

  public JMenu getFormatMenu()
  {
    return formatMenu;
  }

  public JMenu getInsertMenu()
  {
    return insertMenu;
  }


  private void createEditorActions()
  {
    actionList = new ActionList("editor-actions");

    ActionList paraActions = new ActionList("paraActions");
    ActionList fontSizeActions = new ActionList("fontSizeActions");
    ActionList editActions = HTMLEditorActionFactory.createEditActionList();
    Action objectPropertiesAction = new HTMLElementPropertiesAction();

    // create file menu
    JMenu fileMenu = new JMenu(i18n.str("file"));

    // create edit menu
    ActionList lst = new ActionList("edits");
    lst.addAll(editActions);
    lst.add(null);
    lst.add(new FindReplaceAction(false));
    actionList.addAll(lst);
    editMenu = ActionUIFactory.getInstance().createMenu(lst);
    editMenu.setText(i18n.str("edit"));


    //create format menu
    formatMenu = new JMenu(i18n.str("format"));
    lst = SpecmanFontSizeActionFactory.createFontSizeActionList();
    actionList.addAll(lst);
    formatMenu.add(createMenu(lst, i18n.str("size")));
    fontSizeActions.addAll(lst);

    lst = HTMLEditorActionFactory.createInlineActionList();
    // Strikethrough conflicts with the deletion markup in change mode, so it is not offered as a user style
    lst.removeIf(a -> a instanceof HTMLInlineAction && ((HTMLInlineAction) a).getTag() == HTML.Tag.STRIKE);
    actionList.addAll(lst);
    formatMenu.add(createMenu(lst, i18n.str("style")));

    Action act = new HTMLFontColorAction();
    actionList.add(act);
    formatMenu.add(act);

    act = new SpecmanHTMLFontAction();
    actionList.add(act);
    formatMenu.add(act);

    act = new ClearStylesAction();
    actionList.add(act);
    formatMenu.add(act);
    formatMenu.addSeparator();

    lst = HTMLEditorActionFactory.createBlockElementActionList();
    lst = HTMLBlockActionWithMarkupRecovery.wrap(lst);
    actionList.addAll(lst);
    formatMenu.add(createMenu(lst, i18n.str("paragraph")));
    paraActions.addAll(lst);

    lst = new ActionList("list");
    lst.add(new UnorderedListItemAction());
    lst.add(new OrderedListItemAction());
    actionList.addAll(lst);
    formatMenu.add(createMenu(lst, i18n.str("list")));
    formatMenu.addSeparator();
    paraActions.addAll(lst);

    lst = HTMLEditorActionFactory.createAlignActionList();
    actionList.addAll(lst);
    formatMenu.add(createMenu(lst, i18n.str("align")));

    JMenu tableMenu = new JMenu(i18n.str("table"));
    lst = HTMLEditorActionFactory.createInsertTableElementActionList();
    actionList.addAll(lst);
    tableMenu.add(createMenu(lst, i18n.str("insert")));

    lst = HTMLEditorActionFactory.createDeleteTableElementActionList();
    actionList.addAll(lst);
    tableMenu.add(createMenu(lst, i18n.str("delete")));
    formatMenu.add(tableMenu);
    formatMenu.addSeparator();

    actionList.add(objectPropertiesAction);
    formatMenu.add(objectPropertiesAction);



    //create insert menu
    insertMenu = new JMenu(i18n.str("insert"));
    act = new HTMLLinkAction();
    actionList.add(act);
    insertMenu.add(act);

    act = new ImageAction();
    actionList.add(act);
    insertMenu.add(act);

    act = new TableAction();
    actionList.add(act);
    insertMenu.add(act);
    insertMenu.addSeparator();

    act = new HTMLLineBreakAction();
    actionList.add(act);
    insertMenu.add(act);

    act = new HTMLHorizontalRuleAction();
    actionList.add(act);
    insertMenu.add(act);

    act = new SpecialCharAction();
    actionList.add(act);
    insertMenu.add(act);

    act = new StepnumberLinkAction();
    actionList.add(act);
    insertMenu.add(act);

    createToolbarComponents(paraActions, fontSizeActions);
    List<AbstractButton> toolbarButtons = textGroupComponents.stream()
            .filter(AbstractButton.class::isInstance)
            .map(AbstractButton.class::cast)
            .collect(Collectors.toCollection(java.util.ArrayList::new));
    insertGroupComponents.stream()
            .filter(AbstractButton.class::isInstance)
            .map(AbstractButton.class::cast)
            .forEach(toolbarButtons::add);
    ShefIconInstaller.install(actionList, toolbarButtons);
  }

  private void createToolbarComponents(ActionList blockActs, ActionList fontSizeActs)
  {
    Font comboFont = new Font("Dialog", Font.PLAIN, 12);
    PropertyChangeListener propLst = new PropertyChangeListener()
    {
      public void propertyChange(PropertyChangeEvent evt)
      {
        if(evt.getPropertyName().equals("selected"))
        {
          if(evt.getNewValue().equals(Boolean.TRUE))
          {
            paragraphCombo.removeActionListener(paragraphComboHandler);
            paragraphCombo.setSelectedItem(evt.getSource());
            paragraphCombo.addActionListener(paragraphComboHandler);
          }
        }
      }
    };
    for (Object o : blockActs) {
      if (o instanceof DefaultAction)
        ((DefaultAction) o).addPropertyChangeListener(propLst);
    }
    paragraphCombo = new JComboBox(toArray(blockActs));
    paragraphCombo.setPreferredSize(new Dimension(120, 22));
    paragraphCombo.setMinimumSize(new Dimension(120, 22));
    paragraphCombo.setMaximumSize(new Dimension(120, 22));
    paragraphCombo.setFont(comboFont);
    paragraphCombo.addActionListener(paragraphComboHandler);
    paragraphCombo.setRenderer(new ParagraphComboRenderer());
    textGroupComponents.add(paragraphCombo);

    fontFamilyCombo = new JComboBox(FontFamily.ALL_FONTS);
    fontFamilyCombo.setPreferredSize(new Dimension(150, 22));
    fontFamilyCombo.setMinimumSize(new Dimension(150, 22));
    fontFamilyCombo.setMaximumSize(new Dimension(150, 22));
    fontFamilyCombo.setFont(comboFont);
    fontFamilyCombo.addActionListener(fontChangeHandler);
    textGroupComponents.add(fontFamilyCombo);

    final JButton fontSizeButton = new JButton(IconReader.readSvgIcon(SpecmanIcons.FONT_SIZE, SpecmanIcons.SIZE_TOOLBAR));
    final JPopupMenu sizePopup = ActionUIFactory.getInstance().createPopupMenu(fontSizeActs);
    ActionListener al = e -> sizePopup.show(fontSizeButton, 0, fontSizeButton.getHeight());
    fontSizeButton.addActionListener(al);
    configToolbarButton(fontSizeButton);
    textGroupComponents.add(fontSizeButton);

    Action act = new HTMLFontColorAction();
    actionList.add(act);
    addToToolBar(textGroupComponents, act);

    act = new HTMLInlineAction(HTMLInlineAction.BOLD);
    act.putValue(ActionManager.BUTTON_TYPE, ActionManager.BUTTON_TYPE_VALUE_TOGGLE);
    actionList.add(act);
    addToToolBar(textGroupComponents, act);

    act = new HTMLInlineAction(HTMLInlineAction.ITALIC);
    act.putValue(ActionManager.BUTTON_TYPE, ActionManager.BUTTON_TYPE_VALUE_TOGGLE);
    actionList.add(act);
    addToToolBar(textGroupComponents, act);

    act = new HTMLInlineAction(HTMLInlineAction.UNDERLINE);
    act.putValue(ActionManager.BUTTON_TYPE, ActionManager.BUTTON_TYPE_VALUE_TOGGLE);
    actionList.add(act);
    addToToolBar(textGroupComponents, act);

    List alst = HTMLEditorActionFactory.createAlignActionList();
    for(Iterator it = alst.iterator(); it.hasNext();)
    {
      act = (Action)it.next();
      act.putValue(ActionManager.BUTTON_TYPE, ActionManager.BUTTON_TYPE_VALUE_TOGGLE);
      actionList.add(act);
      addToToolBar(textGroupComponents, act);
    }

    act = new UnorderedListItemAction();
    actionList.add(act);
    textAreaButtons.add(addToToolBar(insertGroupComponents, act));

    act = new OrderedListItemAction();
    actionList.add(act);
    textAreaButtons.add(addToToolBar(insertGroupComponents, act));

    act = new HTMLLinkAction();
    actionList.add(act);
    addToToolBar(insertGroupComponents, act);

    act = new ImageAction();
    actionList.add(act);
    textAreaButtons.add(addToToolBar(insertGroupComponents, act));

    act = new TableAction();
    actionList.add(act);
    textAreaButtons.add(addToToolBar(insertGroupComponents, act));

    act = new StepnumberLinkAction();
    actionList.add(act);
    textAreaButtons.add(addToToolBar(insertGroupComponents, act));
  }

  private AbstractButton addToToolBar(List<Component> group, Action act)
  {
    AbstractButton button = ActionUIFactory.getInstance().createButton(act);
    configToolbarButton(button);
    group.add(button);
    return button;
  }

  /**
   * Converts an action list to an array.
   * Any of the null "separators" or sub ActionLists are ommited from the array.
   * @param lst
   * @return
   */
  private Action[] toArray(ActionList lst)
  {
    List acts = new ArrayList();
    for(Iterator it = lst.iterator(); it.hasNext();)
    {
      Object v = it.next();
      if(v != null && v instanceof Action)
        acts.add(v);
    }

    return (Action[])acts.toArray(new Action[acts.size()]);
  }

  private void configToolbarButton(AbstractButton button)
  {
    button.setText(null);
    button.setMnemonic(0);
    // same sizing as the DiagramToolBar buttons: no margin, size derived from the icon
    button.setMargin(new Insets(0, 0, 0, 0));
    button.setFocusable(false);
    button.setFocusPainted(false);
    //button.setBorder(plainBorder);
    Action a = button.getAction();
    if(a != null)
      button.setToolTipText(a.getValue(Action.NAME).toString());
  }

  private JMenu createMenu(ActionList lst, String menuName)
  {
    JMenu m = ActionUIFactory.getInstance().createMenu(lst);
    m.setText(menuName);
    return m;
  }

  public void instrumentWysEditor(JEditorPane ed, String initialText, Integer orientation) {
    ed.setEditorKit(new WysiwygHTMLEditorKit());
    //styleSheet.addRule("body { background-color: #ccc; font-family: Times, serif; }");
    //styleSheet.addRule("html { background-color: #ccc; font-family: Times, serif; }");
    //styleSheet.addRule("@font-face { font-family: \"Default\"; src: local(\"Times New Roman\"); }");

    //styleSheet.addRule("h1 {color: blue; font-family:helvetica; }");
    ed.setContentType("text/html");

    initialText = this.formatInitialText(initialText, orientation);
    this.insertHTML(ed, initialText, 0);
    ed.addCaretListener(this.caretHandler);
    ed.addFocusListener(this.focusHandler);
    HTMLDocument document = (HTMLDocument)ed.getDocument();
    CompoundUndoManager cuh = new CompoundUndoManager(document, this.centralUndoManager != null ? this.centralUndoManager : new UndoManager());
    document.addUndoableEditListener(cuh);
  }

  private String formatInitialText(String initialText, int orientation) {
    if (initialText == null) {
      return "<p></p>";
    } else {
      switch(orientation) {
        case 1:
          return "<div align='center'>" + initialText + "</div>";
        case 2:
          return "<div align='right'>" + initialText + "</div>";
        default:
          return initialText;
      }
    }
  }

  //  inserts html into the wysiwyg editor TODO remove JEditorPane parameter
  private void insertHTML(JEditorPane editor, String html, int location)
  {
    try
    {
      HTMLEditorKit kit = (HTMLEditorKit) editor.getEditorKit();
      Document doc = editor.getDocument();
      StringReader reader = new StringReader(HTMLUtils.jEditorPaneizeHTML(html));
      kit.read(reader, doc, location);
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }

  private void updateState()
  {
    actionList.putContextValueForAll(HTMLTextEditAction.EDITOR, focusedEditor);
    actionList.updateEnabledForAll();
    applyTextAreaActionsEnabled();
  }

  /** Enables or disables the insert buttons that need a focused text area; survives SHEF's own state updates. */
  public void setTextAreaActionsEnabled(boolean enabled)
  {
    textAreaActionsEnabled = enabled;
    applyTextAreaActionsEnabled();
  }

  private void applyTextAreaActionsEnabled()
  {
    for (AbstractButton button : textAreaButtons)
    {
      Action action = button.getAction();
      button.setEnabled(textAreaActionsEnabled && (action == null || action.isEnabled()));
    }
  }


  private class CaretHandler implements CaretListener
  {
    /* (non-Javadoc)
     * @see javax.swing.event.CaretListener#caretUpdate(javax.swing.event.CaretEvent)
     */
    public void caretUpdate(CaretEvent e)
    {
      updateState();
    }
  }

  private class FocusHandler implements FocusListener
  {
    public void focusGained(FocusEvent e)
    {
      if(e.getComponent() instanceof JEditorPane)
      {
        JEditorPane ed = (JEditorPane)e.getComponent();
        CompoundUndoManager.updateUndo(ed.getDocument());
        focusedEditor = ed;

        updateState();
        // updateEnabledStates();
      }
    }

    public void focusLost(FocusEvent e)
    {

      if(e.getComponent() instanceof JEditorPane)
      {
        //focusedEditor = null;
        //wysiwygUpdated();
      }
    }
  }

  private class ParagraphComboHandler implements ActionListener
  {
    public void actionPerformed(ActionEvent e) {
      if(e.getSource() == paragraphCombo)
      {
        Action a = (Action)(paragraphCombo.getSelectedItem());
        a.actionPerformed(e);
      }
    }
  }

  private class ParagraphComboRenderer extends DefaultListCellRenderer
  {
    /**
     *
     */
    @Serial private static final long serialVersionUID = 1L;

    public Component getListCellRendererComponent(JList list, Object value, int index,
        boolean isSelected, boolean cellHasFocus)
    {
      if(value instanceof Action)
      {
        value = ((Action)value).getValue(Action.NAME);
      }

      return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    }
  }

  private class FontChangeHandler implements ActionListener
  {
    public void actionPerformed(ActionEvent e)
    {
      if(e.getSource() == fontFamilyCombo && focusedEditor != null)
      {
        //MutableAttributeSet tagAttrs = new SimpleAttributeSet();
        HTMLDocument document = (HTMLDocument)focusedEditor.getDocument();
        CompoundUndoManager.beginCompoundEdit(document);

        if(fontFamilyCombo.getSelectedIndex() != 0)
        {
          HTMLUtils.setFontFamily(focusedEditor, ((FontFamily)fontFamilyCombo.getSelectedItem()).toFamiliyName());
        }
        else
        {
          HTMLUtils.setFontFamily(focusedEditor, null);
        }
        CompoundUndoManager.endCompoundEdit(document);
      }
    }

    /* (non-Javadoc)
     * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
     */
    public void itemStateChanged(ItemEvent e)
    {


    }
  }
}
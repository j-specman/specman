package specman.metatag.generic.config;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import specman.i18n.IButton;
import specman.undo.UndoableMetaTagConfigAdded;
import specman.undo.UndoableMetaTagConfigEdited;
import specman.undo.UndoableMetaTagConfigRemoved;
import specman.undo.manager.UndoRecording;
import specman.view.AbstractSchrittView;
import specman.visitor.MetaTagConfigUseLister;
import specman.visitor.MetaTagConfigUseRemover;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import static specman.Specman.editor;
import static specman.i18n.Messages.i18n;

/** The content of the {@link MetaTagConfigsDialog}: lists the meta tag configurations of the diagram and lets the user
 * create, edit and delete them. Every confirmed creation, edit or deletion is one undoable step. Being a plain panel,
 * it can be created and operated without displaying a dialog.
 * <p>
 * Meta tags are the level above the content, so none of the changes is a change of the content: they leave no change
 * marks, even if what depends on a configuration changes with it. */
public class MetaTagConfigsEditor extends JPanel {
  @Serial
  private static final long serialVersionUID = 1L;

  public static final String LIST = "metatag.configs.list";
  public static final String NEW_BUTTON = "metatag.configs.new";
  public static final String EDIT_BUTTON = "metatag.configs.edit";
  public static final String DELETE_BUTTON = "metatag.configs.delete";
  public static final String CLOSE_BUTTON = "metatag.configs.close";

  private static final Dimension LIST_SIZE = new Dimension(280, 200);
  private static final int MAX_LISTED_USES = 10;

  private final MetaTagConfigRegistry registry;
  private final Set<String> namesInUse;
  private final DefaultListModel<MetaTagConfig> listModel = new DefaultListModel<>();
  private final JList<MetaTagConfig> list = new JList<>(listModel);
  private final JButton newButton = new IButton("button.new");
  private final JButton editButton = new IButton("button.edit");
  private final JButton deleteButton = new IButton("button.delete");
  private final JButton closeButton = new IButton("button.close");
  private Runnable closeAction = () -> { };

  /** @param namesInUse names of the configurations which tags at steps and diagrams refer to; these must not be
   * renamed */
  public MetaTagConfigsEditor(MetaTagConfigRegistry registry, Set<String> namesInUse) {
    super(new BorderLayout());
    this.registry = registry;
    this.namesInUse = new HashSet<>(namesInUse);
    initComponents();
    refreshList(null);
  }

  /** @param closeAction what to do when the user is done, e.g. closing the dialog around the panel */
  public void onClose(Runnable closeAction) {
    this.closeAction = closeAction;
  }

  private void initComponents() {
    list.setName(LIST);
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setCellRenderer(new MetaTagConfigListRenderer());
    list.addListSelectionListener(e -> {
      boolean selected = list.getSelectedValue() != null;
      editButton.setEnabled(selected);
      deleteButton.setEnabled(selected);
    });
    list.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2 && list.getSelectedValue() != null) {
          editSelected();
        }
      }
    });
    JScrollPane scrollPane = new JScrollPane(list);
    scrollPane.setPreferredSize(LIST_SIZE);

    JPanel center = new JPanel(new FormLayout("10px, fill:default:grow, 10px", "10px, fill:default:grow, 10px"));
    center.add(scrollPane, CC.xy(2, 2));

    JPanel south = new JPanel(new BorderLayout());
    south.add(new JSeparator(), BorderLayout.NORTH);
    south.add(buildButtonPanel(), BorderLayout.CENTER);

    add(center, BorderLayout.CENTER);
    add(south, BorderLayout.SOUTH);
  }

  private JPanel buildButtonPanel() {
    newButton.setName(NEW_BUTTON);
    newButton.addActionListener(e -> createNew());
    editButton.setName(EDIT_BUTTON);
    editButton.addActionListener(e -> editSelected());
    editButton.setEnabled(false);
    deleteButton.setName(DELETE_BUTTON);
    deleteButton.addActionListener(e -> deleteSelected());
    deleteButton.setEnabled(false);
    closeButton.setName(CLOSE_BUTTON);
    closeButton.addActionListener(e -> closeAction.run());
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    panel.add(newButton);
    panel.add(editButton);
    panel.add(deleteButton);
    panel.add(closeButton);
    return panel;
  }

  private void createNew() {
    MetaTagConfig created = editor().editMetaTagConfig(null, true, registry::contains);
    if (created != null) {
      registry.add(created);
      editor().addEdit(new UndoableMetaTagConfigAdded(registry, created));
      refreshList(created);
    }
  }

  /** The name of a configuration which is used can't be changed: tags at steps and stereotypes in diagrams refer to it. */
  private void editSelected() {
    MetaTagConfig old = list.getSelectedValue();
    MetaTagConfig edited = editor().editMetaTagConfig(old, !namesInUse.contains(old.getName()),
      name -> !name.equals(old.getName()) && registry.contains(name));
    if (edited != null && !edited.equals(old)) {
      registry.replace(old.getName(), edited);
      editor().addEdit(new UndoableMetaTagConfigEdited(registry, old, edited));
      refreshList(edited);
    }
  }

  private void refreshList(MetaTagConfig toSelect) {
    listModel.clear();
    registry.all().forEach(listModel::addElement);
    list.setSelectedValue(toSelect, true);
  }

  /** The tags at steps and the stereotypes in diagrams which refer to the configuration are removed together with it,
   * in one undoable step. The undo brings back the configuration first, then what used it. */
  private void deleteSelected() {
    MetaTagConfig config = list.getSelectedValue();
    if (namesInUse.contains(config.getName()) && !confirmDeletionWithUses(config)) {
      return;
    }
    try (UndoRecording ur = editor().composeUndo()) {
      for (AbstractSchrittView step : editor().listAllSteps()) {
        step.removeTagsOfUDBL(config);
      }
      editor().traverseDiagram(new MetaTagConfigUseRemover(config.getName()));
      int index = registry.remove(config.getName());
      editor().addEdit(new UndoableMetaTagConfigRemoved(registry, config, index));
    }
    namesInUse.remove(config.getName());
    refreshList(null);
  }

  private boolean confirmDeletionWithUses(MetaTagConfig config) {
    return editor().showConfirmDialog(i18n("dialog.metatag.configs.delete.confirm", config.getName(), describeUses(config)),
      i18n("dialog.metatag.configs.delete.title"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
  }

  /** @return the first uses of the configuration, one per line, and the number of the others if there are more, so
   * that the warning doesn't get too high for a configuration which is used everywhere */
  private static String describeUses(MetaTagConfig config) {
    MetaTagConfigUseLister lister = new MetaTagConfigUseLister(config.getName());
    editor().traverseDiagram(lister);
    List<String> uses = lister.uses();
    StringBuilder description = new StringBuilder();
    for (String use : uses.subList(0, Math.min(uses.size(), MAX_LISTED_USES))) {
      description.append("• ").append(use).append('\n');
    }
    if (uses.size() > MAX_LISTED_USES) {
      description.append("… ").append(i18n("dialog.metatag.configs.use.more", uses.size() - MAX_LISTED_USES));
    }
    return description.toString().stripTrailing();
  }
}

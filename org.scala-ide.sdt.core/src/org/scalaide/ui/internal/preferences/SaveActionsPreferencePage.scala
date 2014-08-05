package org.scalaide.ui.internal.preferences

import org.eclipse.jface.layout.TableColumnLayout
import org.eclipse.jface.preference.PreferencePage
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.viewers.CheckStateChangedEvent
import org.eclipse.jface.viewers.CheckboxTableViewer
import org.eclipse.jface.viewers.ColumnWeightData
import org.eclipse.jface.viewers.IStructuredContentProvider
import org.eclipse.jface.viewers.SelectionChangedEvent
import org.eclipse.jface.viewers.TableViewerColumn
import org.eclipse.jface.viewers.Viewer
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Table
import org.eclipse.swt.widgets.Text
import org.eclipse.ui.IWorkbench
import org.eclipse.ui.IWorkbenchPreferencePage
import org.scalaide.core.ScalaPlugin
import org.scalaide.core.internal.formatter.FormatterPreferences
import org.scalaide.extensions.SaveActionSetting
import org.scalaide.ui.internal.editor.SaveActionExtensions
import org.scalaide.util.internal.eclipse.SWTUtils

import scalariform.formatter.ScalaFormatter

/** This class is referenced through plugin.xml */
class SaveActionsPreferencePage extends PreferencePage with IWorkbenchPreferencePage {

  private val prefStore = ScalaPlugin.prefStore

  private var textBefore: IDocument = _
  private var textAfter: IDocument = _
  private var descriptionArea: Text = _

  private val settings = SaveActionExtensions.saveActionSettings.toArray

  private var changes = Set[SaveActionSetting]()

  override def createContents(parent: Composite): Control = {
    import SWTUtils._

    val base = new Composite(parent, SWT.NONE)
    base.setLayout(new GridLayout(2, true))

    mkLabel(base, "Save actions are executed for open editors whenever a save event occurs for one of them.", columnSize = 2)

    val tableComposite = new Composite(base, SWT.NONE)
    tableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1))

    val table = new Table(tableComposite, SWT.CHECK | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION | SWT.V_SCROLL)
    table.setHeaderVisible(true)
    table.setLinesVisible(true)

    val tcl = new TableColumnLayout
    tableComposite.setLayout(tcl)

    val viewer = new CheckboxTableViewer(table)
    viewer.setContentProvider(ContentProvider)
    viewer.addSelectionChangedListener { e: SelectionChangedEvent =>
      selectSaveAction(table.getSelection().head.getData().asInstanceOf[SaveActionSetting])
    }
    viewer.addCheckStateListener { e: CheckStateChangedEvent =>
      toggleSaveAction(e.getElement().asInstanceOf[SaveActionSetting])
    }

    val columnEnabled = new TableViewerColumn(viewer, SWT.NONE)
    columnEnabled.getColumn().setText("Name")
    columnEnabled onLabelUpdate { _.asInstanceOf[SaveActionSetting].name }
    tcl.setColumnData(columnEnabled.getColumn(), new ColumnWeightData(1, true))

    viewer.setInput(settings)
    viewer.setAllChecked(false)
    viewer.setCheckedElements(settings.filter(isEnabled).asInstanceOf[Array[AnyRef]])

    mkLabel(base, "Description:", columnSize = 2)

    descriptionArea = mkTextArea(base, lineHeight = 3, columnSize = 2)

    mkLabel(base, "Before:")
    mkLabel(base, "After:")

    val previewTextBefore = createPreviewer(base) {
      textBefore = _
    }
    previewTextBefore.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true))

    val previewTextAfter = createPreviewer(base) {
      textAfter = _
    }
    previewTextAfter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true))

    base
  }

  override def init(workbench: IWorkbench): Unit = ()

  override def performOk(): Boolean = {
    changes foreach { saveAction =>
      val previousValue = prefStore.getBoolean(saveAction.id)
      prefStore.setValue(saveAction.id, !previousValue)
    }
    super.performOk()
  }

  override def performDefaults(): Unit = {
    super.performDefaults
  }

  private def mkTextArea(parent: Composite, lineHeight: Int = 1, initialText: String = "", columnSize: Int = 1): Text = {
    val t = new Text(parent, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.WRAP)
    t.setText(initialText)
    t.setLayoutData({
      val gd = new GridData(SWT.FILL, SWT.FILL, true, false, columnSize, 1)
      gd.heightHint = lineHeight*t.getLineHeight()
      gd
    })
    t
  }

  private def mkLabel(parent: Composite, text: String, columnSize: Int = 1): Label = {
    val lb = new Label(parent, SWT.NONE)
    lb.setText(text)
    lb.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, columnSize, 1))
    lb
  }

  private def isEnabled(saveAction: SaveActionSetting): Boolean =
    prefStore.getBoolean(saveAction.id)

  private def toggleSaveAction(saveAction: SaveActionSetting) = {
    if (changes.contains(saveAction))
      changes -= saveAction
    else
      changes += saveAction
  }

  private def selectSaveAction(saveAction: SaveActionSetting) = {
    textBefore.set(formatPreviewText(saveAction.textBefore))
    textAfter.set(formatPreviewText(saveAction.textAfter))
    descriptionArea.setText(saveAction.description)
  }

  private def createPreviewer(parent: Composite)(f: IDocument => Unit): Control = {
    val previewer = ScalaPreviewerFactory.createPreviewer(parent, prefStore, "")
    f(previewer.getDocument())
    previewer.getControl
  }

  private def formatPreviewText(text: String): String =
    ScalaFormatter.format(text, FormatterPreferences.getPreferences(prefStore))

  private object ContentProvider extends IStructuredContentProvider {

    override def dispose(): Unit = ()

    override def getElements(input: Any): Array[AnyRef] = {
      input.asInstanceOf[Array[AnyRef]]
    }

    override def inputChanged(viewer: Viewer, oldInput: Any, newInput: Any): Unit = ()
  }
}

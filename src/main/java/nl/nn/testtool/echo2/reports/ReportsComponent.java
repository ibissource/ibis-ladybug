package nl.nn.testtool.echo2.reports;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TooManyListenersException;

import nextapp.echo2.app.Border;
import nextapp.echo2.app.Button;
import nextapp.echo2.app.CheckBox;
import nextapp.echo2.app.Color;
import nextapp.echo2.app.Column;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.Extent;
import nextapp.echo2.app.FillImageBorder;
import nextapp.echo2.app.Grid;
import nextapp.echo2.app.Insets;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.ListBox;
import nextapp.echo2.app.Row;
import nextapp.echo2.app.SelectField;
import nextapp.echo2.app.Table;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.WindowPane;
import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import nextapp.echo2.app.filetransfer.UploadSelect;
import nextapp.echo2.app.layout.ColumnLayoutData;
import nextapp.echo2.app.list.DefaultListModel;
import nextapp.echo2.app.list.ListSelectionModel;
import nextapp.echo2.app.table.DefaultTableModel;
import nl.nn.testtool.MetadataExtractor;
import nl.nn.testtool.Report;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.echo2.BeanParent;
import nl.nn.testtool.echo2.ComparePane;
import nl.nn.testtool.echo2.Echo2Application;
import nl.nn.testtool.echo2.TransformationWindow;
import nl.nn.testtool.echo2.util.Download;
import nl.nn.testtool.filter.View;
import nl.nn.testtool.filter.Views;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.transform.ReportXmlTransformer;
import nl.nn.testtool.util.LogUtil;

import org.apache.log4j.Logger;

import echopointng.SelectFieldEx;
import echopointng.table.DefaultSortableTableModel;
import echopointng.table.SortableTable;
import echopointng.tree.DefaultTreeCellRenderer;

/**
 * @author m00f069
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ReportsComponent extends Column implements BeanParent, ActionListener {
	private static final long serialVersionUID = 1L;
	Logger log = LogUtil.getLogger(this);
	private List<String> changeReportGeneratorEnabledRoles;
	// TODO testTool overbodig maken nu we storage van view halen?
	private TestTool testTool;
	private MetadataExtractor metadataExtractor;
	private Views views;
	private boolean addCompareButton = false;
	private boolean addSeparateOptionsRow = false;
	private TreePane treePane;
	private Label nameLabel;
	private IntegerField integerFieldMaxMetadataTableSize;
	private static final int defaultMaxMetadataTableSize = 10;
	private boolean focusMaxMetadataTableSize = true;
	private Label numberOfMetadataRecords;
	private SelectFieldEx viewSelect;
	private Row filterRow;
	private DefaultTableModel metadataTableModel;
	private DefaultSortableTableModel metadataSortableTableModel;
	private MetadataTableHeaderRenderer metadataTableHeaderRenderer;
	private MetadataTableCellRenderer metadataTableCellRenderer;
	private SortableTable metadataTable;
	private Label numberOfReportsInProgressLabel;
	private Label estimatedMemoryUsageReportsInProgressLabel;
	private Label errorLabel;
	private ReportXmlTransformer reportXmlTransformer = null;
	private SelectField reportGeneratorEnabledSelectField;
	private Label reportGeneratorEnabledErrorLabel;
	private Label filterValuesLabel;
	private ListBox filterValuesListBox;
	private CheckBox checkBoxTransformReportXml;
	private IntegerField integerFieldOpenLatest;
	private TextField regexFilterField;
	private CheckBox checkBoxExcludeReportsWithEmptyReportXml;
	private IntegerField integerFieldOpenReportInProgress;
	private ComparePane comparePane;
	private WindowPane optionsWindow;
	private WindowPane filterWindow;
	private TransformationWindow transformationWindow;
	private WindowPane uploadWindow;
	private UploadSelect uploadSelect;
	private Object firstValueOfLastSelectedRow;
	private SelectField downloadSelectField;
	private BeanParent beanParent;
	private Echo2Application echo2Application;
	private boolean initCalled = false;

	public void setChangeReportGeneratorEnabledRoles(List<String> changeReportGeneratorEnabledRoles) {
		this.changeReportGeneratorEnabledRoles = changeReportGeneratorEnabledRoles;
	}

	public void setTestTool(TestTool testTool) {
		this.testTool = testTool;
	}

	public void setMetadataExtractor(MetadataExtractor metadataExtractor) {
		this.metadataExtractor = metadataExtractor;
	}
	
	public void setViews(Views views) {
		this.views = views;
	}
	
	public Views getViews() {
		return views;
	}
	
	public void setAddCompareButton(boolean addCompareButton) {
		this.addCompareButton = addCompareButton;
	}

	public void setAddSeparateOptionsRow(boolean addSeparateOptionsRow) {
		this.addSeparateOptionsRow = addSeparateOptionsRow;
	}

	public void setTransformationWindow(TransformationWindow transformationWindow) {
		this.transformationWindow = transformationWindow;
	}

	public void setFocusMaxMetadataTableSize(boolean focusMaxMetadataTableSize) {
		this.focusMaxMetadataTableSize = focusMaxMetadataTableSize;
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean() {
		setInsets(new Insets(10));

		// Construct

		DefaultListModel viewDefaultListModel = new DefaultListModel();
		Iterator iterator = views.iterator();
		while (iterator.hasNext()) {
			viewDefaultListModel.add(iterator.next());
		}

		viewSelect = new SelectFieldEx(viewDefaultListModel);
		setSelectedView(views.getDefaultView());
		viewSelect.setActionCommand("ViewSelect");
		viewSelect.addActionListener(this);

		Column optionsColumn = new Column();

		// TODO een OptionsWindow class maken en in Echo2Application instantieren zoals TransformationWindow?
		optionsWindow = new WindowPane();
		optionsWindow.setVisible(false);
		optionsWindow.setTitle("Options");
		optionsWindow.setTitleBackground(Echo2Application.getButtonBackgroundColor());
		optionsWindow.setBorder(new FillImageBorder(Echo2Application.getButtonBackgroundColor(), new Insets(0, 0, 0, 0), new Insets(0, 0, 0, 0)));
		optionsWindow.setWidth(new Extent(500));
		optionsWindow.setHeight(new Extent(185));
		optionsWindow.setInsets(new Insets(10, 5, 0, 0));
		optionsWindow.add(optionsColumn);
		optionsWindow.setDefaultCloseOperation(WindowPane.HIDE_ON_CLOSE);
		optionsWindow.init();

		Column filterColumn = new Column();

		// TODO een FilterWindow class maken en in Echo2Application instantieren zoals TransformationWindow?
		filterWindow = new WindowPane();
		filterWindow.setVisible(false);
		filterWindow.setTitle("Filter");
		filterWindow.setTitleBackground(Echo2Application.getButtonBackgroundColor());
		filterWindow.setBorder(new FillImageBorder(Echo2Application.getButtonBackgroundColor(), new Insets(0, 0, 0, 0), new Insets(0, 0, 0, 0)));
		filterWindow.setWidth(new Extent(600));
		filterWindow.setHeight(new Extent(400));
		filterWindow.setInsets(new Insets(10, 5, 0, 0));
		filterWindow.add(filterColumn);
		filterWindow.setDefaultCloseOperation(WindowPane.HIDE_ON_CLOSE);
		filterWindow.init();

		Column uploadColumn = new Column();

		// TODO een UploadWindow class maken en in Echo2Application instantieren zoals TransformationWindow?
		uploadWindow = new WindowPane();
		uploadWindow.setVisible(false);
		uploadWindow.setTitle("Upload");
		uploadWindow.setTitleBackground(Echo2Application.getButtonBackgroundColor());
		uploadWindow.setBorder(new FillImageBorder(Echo2Application.getButtonBackgroundColor(), new Insets(0, 0, 0, 0), new Insets(0, 0, 0, 0)));
		uploadWindow.setWidth(new Extent(350));
		uploadWindow.setHeight(new Extent(110));
		uploadWindow.setInsets(new Insets(10, 0, 10, 0));
		uploadWindow.add(uploadColumn);
		uploadWindow.setDefaultCloseOperation(WindowPane.HIDE_ON_CLOSE);
		uploadWindow.init();

		Row buttonRow = Echo2Application.getNewRow();
		
		Button expandAllButton  = new Button("Expand all");
		expandAllButton.setActionCommand("ExpandAll");
		Echo2Application.decorateButton(expandAllButton);
		expandAllButton.addActionListener(this);

		Button collapseAllButton  = new Button("Collapse all");
		collapseAllButton.setActionCommand("CollapseAll");
		Echo2Application.decorateButton(collapseAllButton);
		collapseAllButton.addActionListener(this);

		Button closeAllButton  = new Button("Close all");
		closeAllButton.setActionCommand("CloseAll");
		Echo2Application.decorateButton(closeAllButton);
		closeAllButton.addActionListener(this);

		Button downloadAllButton = new Button("Download all");
		downloadAllButton.setActionCommand("DownloadAll");
		downloadAllButton.addActionListener(this);
		Echo2Application.decorateButton(downloadAllButton);

		Button compareButton = new Button("Compare all");
		compareButton.setActionCommand("CompareAll");
		Echo2Application.decorateButton(compareButton);
		compareButton.addActionListener(this);

		Button prepareUploadButton = new Button("Upload...");
		prepareUploadButton.setActionCommand("OpenUploadWindow");
		Echo2Application.decorateButton(prepareUploadButton);
		prepareUploadButton.addActionListener(this);

		Button optionsButtonWindow = new Button("Options...");
		optionsButtonWindow.setActionCommand("OpenOptionsWindows");
		Echo2Application.decorateButton(optionsButtonWindow);
		optionsButtonWindow.addActionListener(this);

		Row optionsRow = null;
		if (addSeparateOptionsRow) {
			optionsRow = Echo2Application.getNewRow();
			optionsRow.setInsets(new Insets(0, 5, 0, 0));
		}

		errorLabel = Echo2Application.createErrorLabelWithColumnLayoutData();
		errorLabel.setVisible(false);
		add(errorLabel);

		Row uploadSelectRow = new Row();

		ReportUploadListener reportUploadListener = new ReportUploadListener();
		reportUploadListener.setReportsComponent(this);

		uploadSelect = new UploadSelect();
		uploadSelect.setEnabledSendButtonText("Upload");
		uploadSelect.setDisabledSendButtonText("Upload");
		try {
			uploadSelect.addUploadListener(reportUploadListener);
		} catch (TooManyListenersException e) {
			String message = "TooManyListenersException: " + e.getMessage();
			displayError(message, e);
		}

		Button buttonRefresh  = new Button("Refresh");
		buttonRefresh.setActionCommand("Refresh");
		Echo2Application.decorateButton(buttonRefresh);
		buttonRefresh.addActionListener(this);

		downloadSelectField = new SelectField(new String[]{"Both", "Report", "Message"});
		downloadSelectField.setSelectedIndex(0);

		integerFieldMaxMetadataTableSize = new IntegerField();
		integerFieldMaxMetadataTableSize.setActionCommand("Refresh");
		integerFieldMaxMetadataTableSize.setWidth(new Extent(25));
		integerFieldMaxMetadataTableSize.setDefaultValue(defaultMaxMetadataTableSize);
		integerFieldMaxMetadataTableSize.addActionListener(this);

		numberOfMetadataRecords = new Label();

		filterRow = Echo2Application.getNewRow();
		filterRow.setInsets(new Insets(0, 5, 0, 0));

		ColumnLayoutData columnLayoutDataForTable;
		metadataTableModel = new DefaultTableModel();
		metadataSortableTableModel = new DefaultSortableTableModel(metadataTableModel);
		metadataTableHeaderRenderer = new MetadataTableHeaderRenderer();
		metadataTableCellRenderer = new MetadataTableCellRenderer();
		columnLayoutDataForTable = new ColumnLayoutData();
		metadataTable = new SortableTable(metadataSortableTableModel);
		metadataTable.setDefaultHeaderRenderer(metadataTableHeaderRenderer);
		metadataTable.setDefaultRenderer(metadataTableCellRenderer);
		metadataTable.setLayoutData(columnLayoutDataForTable);
		metadataTable.setBorder(new Border(1, Color.BLACK, Border.STYLE_DOTTED));
		metadataTable.setFont(DefaultTreeCellRenderer.DEFAULT_FONT);
		metadataTable.setRolloverBackground(Echo2Application.getButtonRolloverBackgroundColor());
		metadataTable.setRolloverEnabled(true);
		metadataTable.setSelectionBackground(Echo2Application.getButtonBackgroundColor());
		metadataTable.setSelectionEnabled(true);
		metadataTable.addActionListener(this);
		metadataTable.setActionCommand("OpenReport");
		columnLayoutDataForTable.setInsets(new Insets(0, 5, 0, 0));
		metadataTableCellRenderer.setMetadataExtractor(metadataExtractor);
		metadataTableHeaderRenderer.getLayoutData().setInsets(new Insets(0, 0, 0, 0));
		metadataTableHeaderRenderer.getLayoutData().setBackground(Echo2Application.getPaneBackgroundColor());
		metadataTableHeaderRenderer.setMetadataExtractor(metadataExtractor);

		nameLabel = Echo2Application.createInfoLabelWithColumnLayoutData();

		numberOfReportsInProgressLabel = Echo2Application.createInfoLabelWithColumnLayoutData();

		estimatedMemoryUsageReportsInProgressLabel = Echo2Application.createInfoLabelWithColumnLayoutData();

		reportGeneratorEnabledSelectField = new SelectField(new String[]{"Yes", "No"});
		reportGeneratorEnabledSelectField.setActionCommand("UpdateGeneratorEnabled");
		reportGeneratorEnabledSelectField.addActionListener(this);

		reportGeneratorEnabledErrorLabel = Echo2Application.createErrorLabelWithRowLayoutData();
		reportGeneratorEnabledErrorLabel.setVisible(false);

		filterValuesLabel = Echo2Application.createInfoLabelWithRowLayoutData();

		filterValuesListBox = new ListBox();
        filterValuesListBox.setSelectionMode(ListSelectionModel.MULTIPLE_SELECTION);
		filterValuesListBox.setActionCommand("UpdateFilterValues");
		filterValuesListBox.addActionListener(this);
		filterValuesListBox.setHeight(new Extent(300));

		Row reportGeneratorEnabledRow = Echo2Application.getNewRow();
		reportGeneratorEnabledRow.setInsets(new Insets(0, 5, 0, 0));
		reportGeneratorEnabledRow.add(new Label("Report generator enabled:"));
		reportGeneratorEnabledRow.add(reportGeneratorEnabledSelectField);
		reportGeneratorEnabledRow.add(reportGeneratorEnabledErrorLabel);
		
		// Testtool - Options - RegexFilterField
		regexFilterField = new TextField();
		regexFilterField.setWidth(new Extent(200));
		
		Button buttonRegexFilterField  = new Button("Apply");
		buttonRegexFilterField.setActionCommand("UpdateRegexValues");
		Echo2Application.decorateButton(buttonRegexFilterField);
		buttonRegexFilterField.addActionListener(this);
				
		Row reportFilterRegexRow = Echo2Application.getNewRow();
		reportFilterRegexRow.setInsets(new Insets(0, 5, 0, 0));
		reportFilterRegexRow.add(new Label("Report filter (regex):"));
		reportFilterRegexRow.add(regexFilterField);
		reportFilterRegexRow.add(buttonRegexFilterField);
					
		// End Testtool - Options - RegexFilterField
		
		Row filterValuesLabelRow = Echo2Application.getNewRow();
		filterValuesLabelRow.setInsets(new Insets(0, 5, 0, 0));
		filterValuesLabelRow.add(filterValuesLabel);

		Row filterValuesSelectRow = Echo2Application.getNewRow();
		filterValuesSelectRow.setInsets(new Insets(0, 5, 0, 0));
		filterValuesSelectRow.add(new Label("Select one or more of:"));
		filterValuesSelectRow.add(filterValuesListBox);
		
		checkBoxTransformReportXml = new CheckBox("Transform report xml");
		checkBoxTransformReportXml.setInsets(new Insets(0, 5, 0, 0));
		checkBoxTransformReportXml.setSelected(true);

		Button buttonOpenTransformationWindow  = new Button("Transformation...");
		buttonOpenTransformationWindow.setActionCommand("OpenTransformationWindow");
		Echo2Application.decorateButton(buttonOpenTransformationWindow);
		buttonOpenTransformationWindow.addActionListener(this);

		Row transformationRow = Echo2Application.getNewRow();
		transformationRow.setInsets(new Insets(0, 5, 0, 0));
		transformationRow.add(checkBoxTransformReportXml);
		transformationRow.add(buttonOpenTransformationWindow);

		Row openLatestReportsRow = Echo2Application.getNewRow();
		openLatestReportsRow.setInsets(new Insets(0, 5, 0, 0));

		Button buttonOpen  = new Button("Open");
		buttonOpen.setActionCommand("OpenLatestReports");
		Echo2Application.decorateButton(buttonOpen);
		buttonOpen.addActionListener(this);

		integerFieldOpenLatest = new IntegerField();
		integerFieldOpenLatest.setWidth(new Extent(25));
		integerFieldOpenLatest.setDefaultValue(10);

		checkBoxExcludeReportsWithEmptyReportXml = new CheckBox("Exclude reports with empty report xml");
		checkBoxExcludeReportsWithEmptyReportXml.setSelected(true);

		Button buttonOpenReportInProgress  = new Button("Open");
		buttonOpenReportInProgress.setActionCommand("OpenReportInProgress");
		Echo2Application.decorateButton(buttonOpenReportInProgress);
		buttonOpenReportInProgress.addActionListener(this);
		
		integerFieldOpenReportInProgress = new IntegerField();
		integerFieldOpenReportInProgress.setWidth(new Extent(25));
		integerFieldOpenReportInProgress.setDefaultValue(1);

		Row openReportInProgressRow = Echo2Application.getNewRow();
		openReportInProgressRow.setInsets(new Insets(0, 5, 0, 0));
		openReportInProgressRow.add(buttonOpenReportInProgress);
		openReportInProgressRow.add(new Label("report in progress number"));
		openReportInProgressRow.add(integerFieldOpenReportInProgress);

		// Wire

		buttonRow.add(buttonRefresh);
		buttonRow.add(optionsButtonWindow);
		if (addCompareButton) {
			buttonRow.add(compareButton);
		}
		buttonRow.add(prepareUploadButton);
		buttonRow.add(downloadAllButton);		
		buttonRow.add(expandAllButton);
		buttonRow.add(collapseAllButton);
		buttonRow.add(closeAllButton);

		uploadSelectRow.add(new Label("Upload"));
		uploadSelectRow.add(uploadSelect);
		uploadColumn.add(uploadSelectRow);

		openLatestReportsRow.add(buttonOpen);
		openLatestReportsRow.add(integerFieldOpenLatest);
		openLatestReportsRow.add(new Label("latest reports"));
		openLatestReportsRow.add(checkBoxExcludeReportsWithEmptyReportXml);
		
		Row rowForAdditinalOptions;
		if (addSeparateOptionsRow) {
			rowForAdditinalOptions = optionsRow;
		} else {
			rowForAdditinalOptions = buttonRow;
		}
		rowForAdditinalOptions.add(new Label("Download:"));
		rowForAdditinalOptions.add(downloadSelectField);
		rowForAdditinalOptions.add(new Label("View:"));
		rowForAdditinalOptions.add(viewSelect);
		rowForAdditinalOptions.add(integerFieldMaxMetadataTableSize);
		rowForAdditinalOptions.add(numberOfMetadataRecords);

		optionsColumn.add(reportGeneratorEnabledRow);
		optionsColumn.add(reportFilterRegexRow);
		optionsColumn.add(transformationRow);
		optionsColumn.add(openLatestReportsRow);
		optionsColumn.add(openReportInProgressRow);

		filterColumn.add(filterValuesLabelRow);
		filterColumn.add(filterValuesSelectRow);

		add(buttonRow);
		if (addSeparateOptionsRow) {
			add(optionsRow);
		}
		add(filterRow);
		add(metadataTable);
		add(nameLabel);
		add(numberOfReportsInProgressLabel);
		add(estimatedMemoryUsageReportsInProgressLabel);
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void initBean(BeanParent beanParent) {
		this.beanParent = beanParent;
		this.echo2Application = Echo2Application.getEcho2Application(beanParent, this);
		echo2Application.getContentPane().add(optionsWindow);
		echo2Application.getContentPane().add(filterWindow);
		echo2Application.getContentPane().add(uploadWindow);
		if (focusMaxMetadataTableSize) {
			echo2Application.setFocusedComponent(getIntegerFieldMaxMetadataTableSize());
		}
		views.initBean(this);
	}

	public BeanParent getBeanParent() {
		return beanParent;
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void init() {
		super.init();
		// Init is (also) called when clicking on "Reports" in tree
		if (!initCalled) {
			initCalled = true;
			displayReports(true);
		}
	}

	public void setTreePane(TreePane treePane) {
		this.treePane = treePane;
	}

	public void setComparePane(ComparePane comparePane) {
		this.comparePane = comparePane;
	}

	public void setReportXmlTransformer(ReportXmlTransformer reportXmlTransformer) {
		this.reportXmlTransformer = reportXmlTransformer;
	}

	public TextField getIntegerFieldMaxMetadataTableSize() {
		return integerFieldMaxMetadataTableSize;
	}

	/**
	 * @see nextapp.echo2.app.event.ActionListener#actionPerformed(nextapp.echo2.app.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		hideErrorMessage();
		if (e.getActionCommand().equals("ExpandAll")) {
			treePane.expandAll();
		} else if (e.getActionCommand().equals("CollapseAll")) {
			treePane.collapseAll();
		} else if (e.getActionCommand().equals("CloseAll")) {
			treePane.closeAllReports();
		} else if (e.getActionCommand().equals("DownloadAll")) {
			try {
				String errorMessage = null;
				Storage storage = treePane.getStorage();
				List storageIds = storage.getStorageIds();
				if (storageIds.size() > 0) {
					String filename = storage.getReport((Integer)storageIds.get(0)).getName();
					if (storageIds.size() > 1) {
						filename = filename  + " and " + (storageIds.size() - 1) + " more";
					}
					if ("Both".equals(downloadSelectField.getSelectedItem())) {
						errorMessage = Download.download(storage, filename, true, true);
					} else if ("Report".equals(downloadSelectField.getSelectedItem())) {
						errorMessage = Download.download(storage, filename);
					} else if ("Message".equals(downloadSelectField.getSelectedItem())) {
						errorMessage = Download.download(storage, filename, false, true);
					} else {
						errorMessage = "No download type selected";
					}
				} else {
					errorMessage = "No open reports to download";
				}
				if (errorMessage != null) {
					displayError(errorMessage);
				}
			} catch(StorageException storageException) {
				displayError(storageException);
			}
		} else if (e.getActionCommand().equals("Refresh")) {
			displayReports(false);
		} else if (e.getActionCommand().equals("OpenReport")) {
			View view = getSelectedView();
			Table table = (Table)e.getSource();
			int selectedIndex = table.getSelectionModel().getMinSelectedIndex();
			firstValueOfLastSelectedRow = metadataTableModel.getValueAt(0, selectedIndex);
			String isOpenReportAllowed = view.isOpenReportAllowed(firstValueOfLastSelectedRow);
			if ("Allowed".equals(isOpenReportAllowed)) {
				Storage storage = view.getStorage();
				openReport(storage, (Integer)firstValueOfLastSelectedRow, true);
			} else {
				displayError(isOpenReportAllowed);
			}
		} else if (e.getActionCommand().equals("ViewSelect")) {
			treePane.redisplayReports(getSelectedView());
			displayReports(true);
		} else if (e.getActionCommand().equals("CompareAll")) {
			comparePane.compare();
		} else if (e.getActionCommand().equals("OpenOptionsWindows")) {
			reportGeneratorEnabledErrorLabel.setVisible(false);
			if (testTool.getReportGeneratorEnabled()) {
				reportGeneratorEnabledSelectField.setSelectedItem("Yes");
			} else {
				reportGeneratorEnabledSelectField.setSelectedItem("No");
			}
			regexFilterField.setText(testTool.getRegexFilter());
			optionsWindow.setVisible(true);
		} else if (e.getActionCommand().equals("UpdateGeneratorEnabled")) {
			if (echo2Application.isUserInRoles(changeReportGeneratorEnabledRoles)) {
				if ("Yes".equals(reportGeneratorEnabledSelectField.getSelectedItem())) {
					testTool.setReportGeneratorEnabled(true);
				} else if ("No".equals(reportGeneratorEnabledSelectField.getSelectedItem())) {
					testTool.setReportGeneratorEnabled(false);
				}
			} else {
				reportGeneratorEnabledErrorLabel.setText("Not allowed");
				reportGeneratorEnabledErrorLabel.setVisible(true);
			}
		// Update the value according to Regexfield	
			
		} else if (e.getActionCommand().equals("UpdateRegexValues")) {
			if (echo2Application.isUserInRoles(changeReportGeneratorEnabledRoles)) {
					testTool.setRegexFilter(regexFilterField.getText());
				}
			else {
				reportGeneratorEnabledErrorLabel.setText("Not allowed");
			}
		// End Update the value according to Regexfield		
		
		} else if (e.getActionCommand().equals("OpenTransformationWindow")) {
			transformationWindow.setVisible(true);
		} else if (e.getActionCommand().equals("OpenLatestReports")) {
			Storage storage = getSelectedView().getStorage();
			List storageIds = null;
			try {
				storageIds = storage.getStorageIds();
			} catch(StorageException storageException) {
				displayError(storageException);
			}
			if (storageIds != null) {
				int max = integerFieldOpenLatest.getValue();
				int size = storageIds.size();
				if (size < max) {
					max = size;
				}
				for (int i = max - 1; i > -1; i--) {
					openReport(storage, (Integer)storageIds.get(i), false);
				}
			}
		} else if (e.getActionCommand().equals("OpenReportInProgress")) {
			Report report = (Report)testTool.getReportInProgress(integerFieldOpenReportInProgress.getValue() - 1);
			if (report != null) {
				openReport(report, true);
			}
		} else if (e.getActionCommand().equals("OpenUploadWindow")) {
			uploadWindow.setVisible(true);
		} else if (e.getActionCommand().startsWith("Reset filter ")) {
			String metadataName = e.getActionCommand().substring(13);
			String filterValue = "";
			Map metadataFilter = getSelectedView().getMetadataFilter();
			if (metadataFilter != null) {
				filterValue = (String)metadataFilter.get(metadataName);
			}
			Button button = (Button)e.getSource();
			Component component = button.getParent();
			Column column;
			if (component instanceof Grid) {
				Grid grid = (Grid)button.getParent();
				column = (Column)grid.getParent();
			} else {
				column = (Column)button.getParent();
			}
			TextField textField = (TextField)column.getComponent(1);
			textField.setText(filterValue);
			displayReports(false);
		} else if (e.getActionCommand().startsWith("Select filter ")) {
			String metadataName = e.getActionCommand().substring(14);
            Storage storage = getSelectedView().getStorage();
			List filterValues = null;
			try {
				filterValues = storage.getFilterValues(metadataName);
			} catch(StorageException storageException) {
				displayError(storageException);
			}
			if (filterValues != null) {
				filterValuesLabel.setText(metadataName);
				DefaultListModel filterListModel = (DefaultListModel) filterValuesListBox.getModel();
	            filterListModel.removeAll();
	            for (int i = 0; i < filterValues.size(); i++) {
					Object o = filterValues.get(i);
					if (o != null) {
						String fv = (String) o;
						if (fv.trim().length() > 0) {
							filterListModel.add(fv);
						}
					}
				}
				Button button = (Button)e.getSource();
				Component component = button.getParent();
				Column column;
				if (component instanceof Grid) {
					Grid grid = (Grid)button.getParent();
					column = (Column)grid.getParent();
				} else {
					column = (Column)button.getParent();
				}
				TextField textField = (TextField)column.getComponent(1);
				String currentFilterValue = textField.getText();
				String[] cfvs = currentFilterValue.split(",");
				int[] indices = new int[cfvs.length];
				for (int i = 0; i < cfvs.length; i++) {
					String cfv = (String) cfvs[i];
					int selectedIndex = filterListModel.indexOf(cfv);
					if (selectedIndex>=0) {
						indices[i] = selectedIndex;
					}
				}
	            filterValuesListBox.setSelectedIndices(indices);
	            filterWindow.setVisible(true);
			}
		} else if (e.getActionCommand().equals("UpdateFilterValues")) {
			String columnId = (String) filterValuesLabel.getText();
			Column column = (Column) filterRow.getComponent(columnId);
			TextField textField = (TextField)column.getComponent(1);
			String listBoxValues = "";
			for (int i = 0; i < filterValuesListBox.getSelectedValues().length; i++) {
				if (i == 0) {
					listBoxValues = (String) filterValuesListBox.getSelectedValues()[i];
				} else {
					listBoxValues = listBoxValues + "," + (String) filterValuesListBox.getSelectedValues()[i];
				}
			}
			textField.setText(listBoxValues);
			displayReports(false);
		}
	}

	private void openReport(Storage storage, Integer storageId,
			boolean ignoreExcludeReportsWithEmptyReportXml) {
		Report report = null;
		try {
			report = storage.getReport(storageId);
			if (report == null) {
				displayError("Report with storage id '" + storageId + "' not found");
			}
		} catch(StorageException storageException) {
			displayError(storageException);
		}
		if (report != null) {
			report.setTestTool(testTool);
			openReport(report, ignoreExcludeReportsWithEmptyReportXml);
		}
	}
	
	public void openReport(Report report, boolean ignoreExcludeReportsWithEmptyReportXml) {
		if (checkBoxTransformReportXml.isSelected()) {
			report.setReportXmlTransformer(reportXmlTransformer);
		}
		boolean exclude = false;
		if (!ignoreExcludeReportsWithEmptyReportXml
				&& checkBoxExcludeReportsWithEmptyReportXml.isSelected()) {
			if (report.toXml().length() < 1) {
				exclude = true;
			}
		}
		if (!exclude) {
			treePane.addReport(report, getSelectedView());
		}
	}

	public void displayReports(boolean metadataNamesChanged) {
		Storage storage = getSelectedView().getStorage();
		try {
			numberOfMetadataRecords.setText("/ " + storage.getSize());
		} catch(StorageException storageException) {
			displayError(storageException);
		}
		// Update filter table and metadata table layout when metadata names have changed
		List metadataNames = getSelectedView().getMetadataNames();
		if (metadataNamesChanged) {
			filterRow.removeAll();
			Map metadataFilter = getSelectedView().getMetadataFilter();
			Iterator iterator = getSelectedView().getMetadataNames().iterator();
			while (iterator.hasNext()) {
				String metadataName = (String)iterator.next();
				int filterType = storage.getFilterType(metadataName);
				Button button = new Button(metadataExtractor.getShortLabel(metadataName));
				Column column = new Column();
				column.setId(metadataName);
				if (filterType == Storage.FILTER_SELECT) {
					Grid grid = new Grid();
					column.add(grid);
					grid.add(button);
					Button selectButton = new Button("...");
					grid.add(selectButton);
					grid.setWidth(new Extent(100, Extent.PERCENT));
					Echo2Application.decorateButton(selectButton);
					selectButton.setFont(DefaultTreeCellRenderer.DEFAULT_FONT);
					selectButton.setWidth(new Extent(35, Extent.PERCENT));
					String actionCommand = "Select filter ";
					selectButton.setToolTipText(actionCommand + "value for " + metadataExtractor.getLabel(metadataName));
					selectButton.setActionCommand(actionCommand + metadataName);
					selectButton.addActionListener(this);
					button.setWidth(new Extent(150, Extent.PERCENT));
				} else {
					column.add(button);
					button.setWidth(new Extent(100, Extent.PERCENT));
				}
				TextField textField = new TextField();
				column.add(textField);
				textField.setFont(DefaultTreeCellRenderer.DEFAULT_FONT);
				textField.setWidth(new Extent(104, Extent.PERCENT));
				if (metadataFilter != null) {
					textField.setText((String)metadataFilter.get(metadataName));
				}
				textField.setToolTipText(storage.getUserHelp(metadataName));
				if (filterType == Storage.FILTER_SELECT) {
					textField.setBackground(Echo2Application.getPaneBackgroundColor());
					textField.setEnabled(false);
				}
				textField.setActionCommand("Refresh");
				textField.addActionListener(this);
				Echo2Application.decorateButton(button);
				button.setFont(DefaultTreeCellRenderer.DEFAULT_FONT);
				String actionCommand = "Reset filter ";
				button.setToolTipText(actionCommand + "value for " + metadataExtractor.getLabel(metadataName));
				button.setActionCommand(actionCommand + metadataName);
				button.addActionListener(this);
				filterRow.add(column);
			}
			metadataTableHeaderRenderer.setMetadataNames(metadataNames);
			metadataTableCellRenderer.setMetadataNames(metadataNames);
			metadataTableModel.setColumnCount(metadataNames.size());
			for (int i = 0; i < metadataNames.size(); i++) {
				String metadataName = (String)metadataNames.get(i);
				metadataTableModel.setColumnName(i, metadataExtractor.getShortLabel(metadataName));
			}
		}
		// Remove old metadata
		while (metadataTableModel.getRowCount() > 0) {
			metadataTableModel.deleteRow(0);
		}
		int numberOfRecords = integerFieldMaxMetadataTableSize.getValue();
		if (numberOfRecords < 0) {
			numberOfRecords = defaultMaxMetadataTableSize;
			integerFieldMaxMetadataTableSize.setText("" + defaultMaxMetadataTableSize);
		}
		// Search/get new metadata
		List searchValues = new ArrayList();
		for (int i = 0; i < filterRow.getComponentCount(); i++) {
			Column column = (Column)filterRow.getComponent(i);
			TextField textField = (TextField)column.getComponent(1);
			searchValues.add(textField.getText());
		}
		List metadata = null;
		try {
			metadata = storage.getMetadata(numberOfRecords, metadataNames,
					searchValues, MetadataExtractor.VALUE_TYPE_GUI);
		} catch(StorageException storageException) {
			displayError(storageException);
		}
		// Display new metadata
		if (metadata != null) {
			Iterator metadataIterator = metadata.iterator();
			while (metadataIterator.hasNext()) {
				List metadataRecord = (List)metadataIterator.next();
				Object[] rowData = new Object[metadataNames.size()];
				for (int i = 0; i < metadataRecord.size(); i++) {
					rowData[i] = metadataRecord.get(i);
				}
				metadataTableModel.addRow(rowData);
			}
		}
		// The last selected report might not be available anymore or on a
		// different row in the new table
		metadataTable.getSelectionModel().clearSelection();
		for (int i = 0; i < metadataTableModel.getRowCount(); i++) {
			if (metadataTableModel.getValueAt(0, i).equals(firstValueOfLastSelectedRow)) {
				metadataTable.getSelectionModel().setSelectedIndex(i, true);
			}
		}
		// Update labels
		nameLabel.setText("Name: " + storage.getName());
		numberOfReportsInProgressLabel.setText("Number of reports in progress: " + testTool.getNumberOfReportsInProgress());
		estimatedMemoryUsageReportsInProgressLabel.setText("Estimated memory usage reports in progress: " + testTool.getReportsInProgressEstimatedMemoryUsage() + " bytes");
	}

	protected void displayError(Throwable t) {
		displayError(t.getMessage(), t);
	}

	protected void displayError(String message, Throwable t) {
		log.error(message, t);
		displayError(t.getMessage());
	}

	protected void displayError(String message) {
		log.error(message);
		if (errorLabel.isVisible()) {
			errorLabel.setText(errorLabel.getText() + " [" + message + "]");
		} else {
			errorLabel.setText("[" + message + "]");
			errorLabel.setVisible(true);
		}
	}

	protected void hideErrorMessage() {
		errorLabel.setVisible(false);
	}

	public WindowPane getUploadOptionsWindow() {
		return uploadWindow;
	}

	public void setSelectedView(View view) {
		viewSelect.setSelectedItem(view);
	}

	public View getSelectedView() {
		return (View)viewSelect.getSelectedItem();
	}
}

class IntegerField extends TextField {
	int defaultValue = 0;

	public void setDefaultValue(int defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	public void init() {
		setText("" + defaultValue);
	}

	public int getValue() {
		int result;
		try {
			result = Integer.parseInt(getText());
		} catch(NumberFormatException numberFormatException) {
			init();
			result = defaultValue;
		}
		return result;
	}
}
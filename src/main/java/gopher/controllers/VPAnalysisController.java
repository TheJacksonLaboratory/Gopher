package gopher.controllers;

import gopher.gui.factories.PopupFactory;
import gopher.service.GopherService;
import gopher.service.model.Approach;
import gopher.service.model.Design;
import gopher.service.model.viewpoint.ViewPoint;
import gopher.util.Utils;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is the Tab that shows a table with all of the viewpoints created together with a sumnmary of the overall
 * quality of the results
 *
 * @author Peter Robinson
 * @version 0.1.3 (2018-06-07).
 */
@Component
public class VPAnalysisController implements Initializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(VPAnalysisController.class.getName());

    /**
     * A map used to keep track of the open tabs. The Key is a reference to a viewpoint object, and the value is a
     * reference to a Tab that has been opened for it.
     */
    private final Map<ViewPoint, Tab> openTabs = new ConcurrentHashMap<>();

    @FXML
    private ScrollPane VpAnalysisScrollPane;

    @FXML
    private HBox listviewHbox;
    @FXML
    private ListView<String> lviewKey;
    @FXML
    private ListView<String> lviewValue;

    @FXML
    private TableView<ViewPoint> viewPointTableView;
    @FXML
    private TableColumn<ViewPoint, Button> actionTableColumn;
    @FXML
    private TableColumn<ViewPoint, String> targetTableColumn;
    @FXML
    private TableColumn<ViewPoint, String> genomicLocationColumn;
    @FXML
    private TableColumn<ViewPoint, String> nSelectedTableColumn;
    @FXML
    private TableColumn<ViewPoint, String> viewpointScoreColumn;
    @FXML
    private TableColumn<ViewPoint, String> viewpointTotalLengthOfActiveSegments;
    @FXML
    private TableColumn<ViewPoint, String> viewpointTotalLength;
    @FXML
    private TableColumn<ViewPoint, String> fragmentOverlappingTSSColumn;
    @FXML
    private TableColumn<ViewPoint, Button> deleteTableColumn;
    @FXML
    private TableColumn<ViewPoint, Button> resetTableColumn;

    @FXML
    private TableColumn<ViewPoint, String> manuallyRevisedColumn;

    private final ObservableList<ViewPoint> observableViewPointList = FXCollections.observableArrayList();
    private final ObservableList<String> summaryTableKeys = FXCollections.observableArrayList();
    private final ObservableList<String> summaryTablevalues = FXCollections.observableArrayList();


    /**
     * A reference to the main TabPane of the GUI. We will add new tabs to this that will show viewpoints in the
     * UCSC browser.
     */
    private TabPane tabpane;
    @Autowired
    private GopherService gopherService;

    @Autowired
    public VPAnalysisController() {

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.setProperty("jsse.enableSNIExtension", "false");
        HBox.setHgrow(lviewValue, Priority.ALWAYS);
        initTable();
        this.lviewKey.setItems(summaryTableKeys);
        this.lviewValue.setItems(summaryTablevalues);
    }

    /**
     * Set up the table that will show the ViewPoints.
     */
    private void initTable() {
        actionTableColumn.setSortable(false);
        actionTableColumn.setCellValueFactory(cdf -> {
            ViewPoint vp = cdf.getValue();
            Button btn = new Button("Show");
            btn.setOnAction(e -> {
                LOGGER.trace(String.format("Adding tab for row with Target: %s, Chromosome: %s, Genomic pos: %d n selected %d ",
                        vp.getTargetName(), vp.getReferenceID(), vp.getGenomicPos(), vp.getActiveSegments().size()));
                openViewPointInTab(vp);
            });
            return new ReadOnlyObjectWrapper<>(btn);
        });

        deleteTableColumn.setSortable(false);
        deleteTableColumn.setCellValueFactory(cdf -> {
            ViewPoint vp = cdf.getValue();
            Button btn = new Button("Delete");
            btn.setOnAction(e -> {
                LOGGER.trace(String.format("Deleting viewpoint: %s, Chromosome: %s, Genomic pos: %d n selected %d ",
                        vp.getTargetName(), vp.getReferenceID(), vp.getGenomicPos(), vp.getActiveSegments().size()));
                gopherService.deleteViewpoint(vp);
                if (this.openTabs.containsKey(vp)) { // If the tab is open, remove it from the GUI.
                    Tab tab = openTabs.get(vp);
                    tab.setDisable(true);
                    tab.getTabPane().getTabs().remove(tab);
                    openTabs.remove(vp);
                }
                refreshVPTable();
            });
            return new ReadOnlyObjectWrapper<>(btn);
        });

        resetTableColumn.setSortable(false);
        resetTableColumn.setCellValueFactory(cdf -> {
            ViewPoint vp = cdf.getValue();
            if (vp.wasModified()) {
                gopherService.setClean(false);
            }
            Button btn = new Button("Reset");
            btn.setOnAction(e -> {
                vp.resetSegmentsToOriginalState();
                refreshVPTable();
                updateViewPointInTab(vp);
            });
            return new ReadOnlyObjectWrapper<>(btn);
        });

        // the third column
        targetTableColumn.setSortable(true);
        targetTableColumn.setEditable(false);
        // The following shows the gene name with an astrerix if the center segment is selected.
        targetTableColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getTargetName()));
        // fourth column--position, e.g.,chr4:622712
        genomicLocationColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getGenomicLocationString()));
        genomicLocationColumn.setComparator(new GenomicLocationComparator());
        //  fifth column--number of selected fragments
        nSelectedTableColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(String.valueOf(cdf.getValue().getActiveSegments().size())));
        nSelectedTableColumn.setComparator(new IntegerComparator());
        // sixth column--score of fragments.
        viewpointScoreColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(String.valueOf(cdf.getValue().getScoreAsPercentString())));
        viewpointScoreColumn.setComparator(new PercentComparator());
        // seventh column--total length of active segments
        viewpointTotalLengthOfActiveSegments.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(String.valueOf(cdf.getValue().getTotalLengthOfActiveSegments())));
        viewpointTotalLengthOfActiveSegments.setComparator(new IntegerComparator());
        // eight column--total length viewpoint
        viewpointTotalLength.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(String.valueOf(((int) cdf.getValue().getTotalLengthOfViewpoint()))));
        viewpointTotalLength.setComparator(new IntegerComparator());
        // ninth column -- is central digest with TSS selected?
        fragmentOverlappingTSSColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().isTSSfragmentChosen() ? "yes" : "no"));

        manuallyRevisedColumn.setCellValueFactory(cdf -> new ReadOnlyStringWrapper(cdf.getValue().getManuallyRevised()));
        // allow titles of all table columns to be broken into multiple lines
        viewPointTableView.getColumns().forEach(Utils::makeHeaderWrappable);
        viewPointTableView.setItems(observableViewPointList);
    }


    private void updateViewPointInTab(ViewPoint vp) {
        if (openTabs.containsKey(vp)) {
            Tab tab = openTabs.get(vp);
            tabpane.getTabs().remove(tab);
            openTabs.remove(vp);
        }
        openViewPointInTab(vp);
    }


    public void removeViewPointTab(ViewPoint vp) {
        if (this.openTabs.containsKey(vp)) {
            Tab tab = this.openTabs.get(vp);
            tab.setDisable(true);
            tab.getTabPane().getTabs().remove(tab);
            this.openTabs.remove(vp);
        } else {
            LOGGER.error("Could not find opened viewpoint in openTabs list: " + vp.getTargetName());
        }
    }

    /** Add a reference to the TabPane that is managed in {@link GopherMainController} as an FXML field.
     * We use the same TabPane here to add new panes for selected ViewPoints.
     * @param tpane a TabPane reference
     */
    public void setTabPane(TabPane tpane) {
        this.tabpane = tpane;
    }

    /**
     * This method creates a new {@link Tab} populated with a viewpoint!
     *
     * @param vp This {@link ViewPoint} object will be opened into a new Tab.
     */
    private void openViewPointInTab(ViewPoint vp) {
        LOGGER.trace("Opening viewpoint {}", vp.getTargetName());
        if (openTabs.containsKey(vp)) {
            Tab tab = openTabs.get(vp);
            LOGGER.trace("openTabs containsKey " + vp.getTargetName());

            if (tab == null || tab.isDisabled()) {
                LOGGER.trace("openTabs REMOVING " + vp.getTargetName());
                openTabs.remove(vp);
            } else {
                LOGGER.trace("openTabs SELECTING " + vp.getTargetName());
                this.tabpane.getSelectionModel().select(tab);
                return;
            }
        }
        LOGGER.trace("openTabs does not contain Key: {}", vp.getTargetName());
        final Tab tab = new Tab("Viewpoint: " + vp.getTargetName());
        tab.setId(vp.getTargetName());
        tab.setClosable(true);
        tab.setOnClosed(event -> {
            if (tabpane.getTabs()
                    .size() == 2) {
                event.consume();
            }
        });

        tab.setOnCloseRequest((e) -> {
            for (ViewPoint vpnt : this.openTabs.keySet()) {
                Tab t = this.openTabs.get(vpnt);
                if (t.equals(tab)) {
                    this.openTabs.remove(vpnt);
                }
            }
        });

        ViewpointScrollPane vpsp = new ViewpointScrollPane(vp, this);
        tab.setContent(vpsp);
        this.tabpane.getTabs().add(tab);
        this.tabpane.getSelectionModel().select(tab);
        openTabs.put(vp, tab);

    }

    public void setTabPaneRef(TabPane tabp) {
        this.tabpane = tabp;
    }


    /**
     * This method gets called right after the user has created a set of Viewpoints. It should
     * show the table and also present a summary of the quality of the set of viewpoints in the
     * upper half of the tab.
     */
    public void showVPTable() {
        if (!this.gopherService.viewpointsInitialized()) {
            LOGGER.warn("[View Points not initialized");
            PopupFactory.displayError("Could not confirmDialog viewpoints", "No initialized viewpoints were found");
            return;
        }
        refreshVPTable();
    }


    public void updateListView() {
        Map<String, String> summaryMap = createListViewContent();
        summaryTableKeys.setAll(summaryMap.keySet());
        summaryTablevalues.setAll(summaryMap.values());
    }

    /**
     * Creates a map with the information that we will display in the two ListView objects of this Tab.
     *
     * @return Map with info about the panel design
     */
    private Map<String, String> createListViewContent() {
        Design design = new Design(this.gopherService);
        design.calculateDesignParameters();
        return design.getDesignStatisticsList();
    }


    /**
     * This method is called to refresh the values of the ViewPoint in the table of the analysis tab.
     */
    public void refreshVPTable() {
        if (gopherService == null) {
            LOGGER.error("GOPHER Service is null--should never happen");
            return;
        }
        LOGGER.trace("Refreshing VP Table, gopher service has {} items", gopherService.getViewPointList().size());
        observableViewPointList.clear();
        observableViewPointList.setAll(this.gopherService.getViewPointList());
        LOGGER.trace("Size of observableViewPointList after adding viewpoints {}", observableViewPointList.size());
    }

    /**
     * Class for sorting items like 100 and 1000
     */
    static class IntegerComparator implements Comparator<String> {
        @Override
        public int compare(String s1, String s2) {
            try {
                Integer d1 = Integer.parseInt(s1);
                Integer d2 = Integer.parseInt(s2);
                return d1.compareTo(d2);
            } catch (Exception e) {
                LOGGER.error("Error encounted while sorting integer values {} and {}: {}", s1, s2, e.getMessage());
                return 0;
            }
        }
    }

    /**
     * Class for sorting items like 2.3% and 34.5%
     */
    static class PercentComparator implements Comparator<String> {

        @Override
        public int compare(String s1, String s2) {
            int i = s1.indexOf("%");
            if (i > 0)
                s1 = s1.substring(0, i);
            i = s2.indexOf("%");
            if (i > 0)
                s2 = s2.substring(0, i);
            try {
                Double d1 = Double.parseDouble(s1);
                Double d2 = Double.parseDouble(s2);
                return d1.compareTo(d2);
            } catch (Exception e) {
                LOGGER.error(String.format("Error encounted while sorting percentage values %s and %s", s1, s2));
                LOGGER.error("Error: {}", e.getMessage());
                return 0;
            }
        }
    }

    /**
     * Class for sorting items like chr3:4325 and chrY:762
     */
    static class GenomicLocationComparator implements Comparator<String> {
        @Override
        public int compare(String s1, String s2) {
            if (!s1.startsWith("chr"))
                return 0;
            if (!s2.startsWith("chr"))
                return 0; /* should never happen */
            s1 = s1.substring(3);
            s2 = s2.substring(3);
            s1 = s1.replaceAll(",", "");
            s2 = s2.replaceAll(",", "");
            String chromStr1, chromStr2;
            int chr1, chr2;
            int pos1, pos2;
            try {
                int i = s1.indexOf(":");
                if (i > 0) {
                    chromStr1 = s1.substring(0, i);
                    pos1 = Integer.parseInt(s1.substring(1 + i));
                    if (chromStr1.equals("X")) {
                        chr1 = 100;
                    } else if (chromStr1.equals("Y")) {
                        chr1 = 101;
                    } else if (chromStr1.startsWith("M")) {
                        chr1 = 102;
                    } else {
                        chr1 = Integer.parseInt(chromStr1);
                    }
                } else {
                    return 0;
                }
                i = s2.indexOf(":");
                if (i > 0) {
                    chromStr2 = s2.substring(0, i);
                    pos2 = Integer.parseInt(s2.substring(1 + i));
                    if (chromStr2.equals("X")) {
                        chr2 = 100;
                    } else if (chromStr2.equals("Y")) {
                        chr2 = 101;
                    } else if (chromStr2.startsWith("M")) {
                        chr2 = 102;
                    } else {
                        chr2 = Integer.parseInt(chromStr2);
                    }
                } else {
                    return 0;
                }
                if (chr1 != chr2) {
                    return Integer.compare(chr1, chr2);
                } else {
                    return Integer.compare(pos1, pos2);
                }
            } catch (Exception e) {
                LOGGER.error(String.format("Error encounted while sorting chromosome locations %s and %s", s1, s2));
                LOGGER.error("Error: {}", e.getMessage());
                return 0;
            }
        }
    }

    public GopherService getGopherService() {
        return this.gopherService;
    }

}

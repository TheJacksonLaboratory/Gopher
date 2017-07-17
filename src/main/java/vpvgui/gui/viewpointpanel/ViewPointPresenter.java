package vpvgui.gui.viewpointpanel;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import vpvgui.gui.analysisPane.VPRow;
import vpvgui.model.Model;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by peter on 16.07.17.
 */
public class ViewPointPresenter implements Initializable {

    @FXML
    public AnchorPane pane;
    @FXML
    private WebView wview;
    @FXML
    private TableView tview;

    private Model model;

    private VPRow vprow;
    /** A reference to the main TabPane of the GUI. We will add new tabs to this that will show viewpoints in the
     * UCSC browser.
     */

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setInitialWebView();
        /* The following line is needed to avoid a SSL handshake alert
         * when opening the UCSC Browser.
         */
        System.setProperty("jsse.enableSNIExtension", "false");
    }


    public ViewPointPresenter(){
    }


    public void setModel(Model m) { this.model=m; }

   // public void setTabPaneRef(TabPane tabp) {
    //    this.tabpane=tabp;
    //}

    public void setVPRow(VPRow row) { this.vprow=row; }

    public void setInitialWebView() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h3>View Point Viewer</h3>");
        sb.append("<p><i>Connecting to UCSC Browser to visualized view point...</i></p>");
        sb.append("</body></html>");
        setData(sb.toString());
    }

    /* TODO
     public void showVPTable() {--see here, to construct table but in this case with Fragment rows.
     */

    public void setData(String html) {
        WebEngine engine = wview.getEngine();
        engine.loadContent(html);
    }


    public void setURL(String url) {
        WebEngine engine = wview.getEngine();
        engine.load(url);
    }

    public AnchorPane getPane() { return this.pane; }

   /* private void addTabPane(VPRow row) {
        final Tab tab = new Tab("Tab " + row.getTargetName());
        tab.setClosable(true);
        tab.setOnClosed(new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
                if (tabpane.getTabs()
                        .size() == 2) {
                    event.consume();
                }
            }
        });
        WebView  browser = new WebView();
        WebEngine engine = browser.getEngine();
        String url = "https://genome.ucsc.edu/cgi-bin/hgTracks?db=mm9&lastVirtModeType=default&lastVirtModeExtraState=&virtModeType=default&virtMode=0&nonVirtPosition=&position=chr4%3A151709259%2D151714759&hgsid=599799979_TMuPBovtFYR9grIdzARnJ2XDq9NE";
        engine.load(url);
        VBox vb = new VBox();
        vb.setPadding(new Insets(30, 50, 50, 50));
        vb.setSpacing(10);
        vb.setAlignment(Pos.CENTER);
        vb.getChildren().addAll(browser);
        tab.setContent(vb);
        this.tabpane.getTabs().add(tab);
        this.tabpane.getSelectionModel().select(tab);
    }*/

    public void sendToUCSC() {
        String url = this.vprow.getURL(); //"https://genome.ucsc.edu/cgi-bin/hgTracks?db=mm9&lastVirtModeType=default&lastVirtModeExtraState=&virtModeType=default&virtMode=0&nonVirtPosition=&position=chr4%3A151709259%2D151714759&hgsid=599799979_TMuPBovtFYR9grIdzARnJ2XDq9NE";
        setURL(url);
       /* VBox vb = new VBox();
        vb.setPadding(new Insets(30, 50, 50, 50));
        vb.setSpacing(10);
        vb.setAlignment(Pos.CENTER);
        vb.getChildren().addAll(browser);
        tab.setContent(vb);*/
    }
}

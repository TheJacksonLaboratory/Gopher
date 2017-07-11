package vpvgui.gui.analysisPane;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javax.swing.text.TableView;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by peterrobinson on 7/6/17.
 */
public class VPAnalysisPresenter implements Initializable {

    @FXML
    private WebView wview;

    @FXML
    private TableView tview;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setInitialWebView();
    }

    public void setInitialWebView() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h3>View Point Viewer");
        sb.append("<p>Please set up and initialize analysis using the first Tab.</p>");
        sb.append("</body></html>");
        setData(sb.toString());
    }

    public void setData(String html) {
        WebEngine engine = wview.getEngine();
        engine.loadContent(html);
    }



}

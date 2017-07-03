package vpvgui.gui.help;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import vpvgui.gui.settings.SettingsPresenter;
import vpvgui.gui.settings.SettingsView;
import vpvgui.model.Settings;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by peterrobinson on 7/3/17.
 */
public class HelpPresenter implements Initializable {

    @FXML
    WebView wview;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // no op
    }

    public void setData(String html) {
        WebEngine engine = wview.getEngine();
        engine.loadContent(html);
    }
}

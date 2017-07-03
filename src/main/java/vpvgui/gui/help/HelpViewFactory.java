package vpvgui.gui.help;

import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.stage.Stage;
import vpvgui.gui.settings.SettingsPresenter;
import vpvgui.gui.settings.SettingsView;
import vpvgui.model.Settings;

/**
 * Created by peterrobinson on 7/3/17.
 */
public class HelpViewFactory {




    public static void display() {
        Stage window;
        String windowTitle = "VPV Settings";
        window = new Stage();
        window.setOnCloseRequest( event -> {window.close();} );
        window.setTitle(windowTitle);

        HelpView view = new HelpView();
        HelpPresenter presenter = (HelpPresenter) view.getPresenter();

        String html=getHTML();
        presenter.setData(html);

        window.setScene(new Scene(view.getView()));
        window.showAndWait();
    }

    /*
        * Formats the list properties in human-readable format, checking for an empty list.
        */
    private static String toStringHelper(String listName, ObservableList<String> lst) {
        if (lst==null || lst.isEmpty()) {
            return (String.format(""));
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s: (%d)\n", listName, lst.size()));
        for (String s : lst) {
            sb.append(String.format("\t%s\n", s));
        }
        return sb.toString();
    }

    private static String getHTML() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><h3>VPV Help</h3>");

        sb.append("</body></html>");
        return sb.toString();

    }

}

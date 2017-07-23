package vpvgui.gui.settings;

import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.stage.Stage;


import java.util.Properties;


/**
 * Created by peter on 01.07.17.
 */
public class SettingsViewFactory {


    public static void showSettings(Properties properties) {
        Stage window;
        String windowTitle = "VPV Settings";
        window = new Stage();
        window.setOnCloseRequest( event -> {window.close();} );
        window.setTitle(windowTitle);

        SettingsView view = new SettingsView();
        SettingsPresenter presenter = (SettingsPresenter) view.getPresenter();
        presenter.setSignal(signal -> {
            switch (signal) {
                case DONE:
                    window.close();
                    break;
                case CANCEL:
                case FAILED:
                    throw new IllegalArgumentException(String.format("Illegal signal %s received.", signal));
            }

        });
        String html=getHTML(properties);
        presenter.setData(html);

        window.setScene(new Scene(view.getView()));
        window.showAndWait();
    }

    /*
        * Formats the list properties in human-readable format, checking for an empty list.
        */
    private static String joinEnzymes(ObservableList<String> lst) {
        if (lst==null || lst.isEmpty()) {
            return (String.format("not initialized"));
        }
        StringBuilder sb = new StringBuilder();
        sb.append(lst.get(0));
        for (int i=0; i<lst.size();i++) {
            sb.append(String.format("; %s", lst.get(i)));
        }
        return sb.toString();
    }

    /**
     * TODO make this nicer in terms of order, e.g., put project name at top etc.
     * @param properties
     * @return
     */
    private static String getHTML(Properties properties) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><h3>VPV Settings</h3>");
        sb.append("<p><ul>");
        for (String prop: properties.stringPropertyNames()) {
            sb.append(String.format("<li>%s: %s</li>",prop,properties.getProperty(prop)));
        }
        sb.append("</ul></p>");
        sb.append("</body></html>");
        return sb.toString();

    }


}

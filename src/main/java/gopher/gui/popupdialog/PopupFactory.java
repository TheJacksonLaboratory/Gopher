package gopher.gui.popupdialog;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;

public class PopupFactory {
    /** Indicates if the entry made by the user is valid and should be transmitted to the main controller.*/
    private boolean valid=false;

    private boolean wasCancelled=false;

    private Integer integerValue=null;

    private String stringValue=null;

    private static final String HTML_HEADER = "<html><head>%s</head><body>";
    private static final String HTML_FOOTER = "</body></html>";




    private boolean showDialogToGetStringFromUser(String windowTitle, String html, String labeltext, String previousValue, String defaultValue){
        Stage window;
        window = new Stage();
        window.setOnCloseRequest( event -> window.close() );
        window.setTitle(windowTitle);

        PopupView view = new PopupView();
        PopupPresenter presenter = (PopupPresenter) view.getPresenter();
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
        presenter.setData(html);
        presenter.setLabelText(labeltext);
        if (previousValue!=null) {
            presenter.setPreviousValue(String.valueOf(previousValue));
        } else {
            presenter.setPromptValue(defaultValue);
        }


        window.setScene(new Scene(view.getView()));
        window.showAndWait();
        if (presenter.wasCanceled()) {
            wasCancelled=true;
            return false; // do nothing, the user canceled the entry
        }
        String value = presenter.getValue();
        if (value!=null && value.length()>0 ) {
            this.stringValue=value;
            valid=true;
        } else {
            valid=false;
        }

        return (! presenter.wasCanceled());
    }


    private static String getProjectNameHTML() {
        return  "<h1>GOPHER Projects</h1>\n"+
                "<p>Enter a name for a new GOPHER project. Names should start with letters, numbers, or an underscore." +
               " By default, Gopher stores the projects in a hidden .gopher directory in the user's home directory." +
                " Projects can also be exported to other locations on the file system using the File|Export... menu item." +
                " Projects can be imported with Project|Import.</p>";
    }


    /** Open up a dialog where the user can enter a new project name. */
    public String getProjectName() {
        String title="Enter New Project Name";
        String labelText="Enter project name:";
        String defaultProjectName="new project";
        String html=getProjectNameHTML();
        boolean  OK = showDialogToGetStringFromUser(title,html,labelText,null,defaultProjectName);
        if (OK) {
            return stringValue;
        } else {
            valid = false;
            return null;
        }

    }

    /**
     * Request a String from user.
     *
     * @param windowTitle - Title of PopUp window
     * @param promptText  - Prompt of Text field (suggestion for user)
     * @param labelText   - Text of your request
     * @return String with user input
     */
    @Deprecated
    public static String getStringFromUser(String windowTitle, String promptText, String labelText) {
        TextInputDialog dialog = new TextInputDialog(promptText);
        dialog.setTitle(windowTitle);
        dialog.setHeaderText(null);
        dialog.setContentText(labelText);
        Optional<String> result = dialog.showAndWait();

        return result.orElse(null);
    }


    public static void displayError(String title, String message) {

        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle(title);

        window.setMinWidth(250);
        Label label = new Label();
        label.setText(message);
        label.setStyle(
                "-fx-border-color: lightblue; "
                        + "-fx-font-size: 14;"
                        + "-fx-border-insets: -5; "
                        + "-fx-border-radius: 5;"
                        + "-fx-border-style: dotted;"
                        + "-fx-border-width: 2;"
                        + "-fx-alignment: top-left;"
                        + "-fx-text-fill: red;"
        );

        Button button = new Button("OK");
        button.setOnAction(e -> window.close());

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10, 50, 50, 50));

        layout.getChildren().addAll(label, button);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout);
        window.setScene(scene);
        window.showAndWait();
    }


    public static void displayMessage(String title, String message) {

        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle(title);

        window.setMinWidth(250);
        Label label = new Label();
        label.setText(message);
        label.setStyle(
                "-fx-border-color: lightblue; "
                        + "-fx-font-size: 14;"
                        + "-fx-border-insets: -5; "
                        + "-fx-border-radius: 5;"
                        + "-fx-border-style: dotted;"
                        + "-fx-border-width: 2;"
                        + "-fx-alignment: top-left;"
                        + "-fx-text-fill: blue;"
        );

        Button button = new Button("OK");
        button.setOnAction(e -> window.close() );


        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10, 50, 50, 50));

        layout.getChildren().addAll(label, button);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout);
        window.setScene(scene);
        window.showAndWait();
    }



    public static void displayException(String title, String message, Exception e) {
        TextArea textArea = new TextArea(e.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        Label label = new Label("The exception stacktrace was:");


        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Exception Dialog");
        alert.setHeaderText(title);
        alert.setContentText(message);

        // Set expandable Exception into the dialog pane.
        alert.getDialogPane().setExpandableContent(expContent);

        alert.showAndWait();
    }




    public static void showAbout(String versionString, String dateString) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("GOPHER");
        alert.setHeaderText(null);
        alert.setContentText(String.format("Version %s\nLast changed: %s",versionString,dateString ));

        alert.showAndWait();
    }

    public static boolean confirmDialog(String title, String message) {
        final BooleanProperty answer = new SimpleBooleanProperty();
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle(title);
        window.setMinWidth(250);
        Label label = new Label();
        label.setText(message);
        label.setPadding(new Insets(5));

        Button yesButton = new Button("Yes");
        yesButton.setDefaultButton(true);
        yesButton.setMinWidth(80);
        Button noButton = new Button("No");
        noButton.setCancelButton(true);
        noButton.setMinWidth(80);

        yesButton.setOnAction(e -> {
            answer.setValue(true);
            window.close();
        });
        noButton.setOnAction(e -> {
            answer.setValue(false);
            window.close();
        });

        VBox layout = new VBox(10);
        HBox buttonLayout = new HBox(20);
        buttonLayout.setAlignment(Pos.CENTER);
        buttonLayout.getChildren().addAll(noButton, yesButton);
        buttonLayout.setPadding(new Insets(5));

        layout.getChildren().addAll(label, buttonLayout);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout);
        window.setScene(scene);
        window.showAndWait();

        return answer.getValue();
    }


    public boolean isValid() { return valid;}
    public boolean wasCancelled() { return wasCancelled; }


    private static String getPreHTML(String text) {
       return String.format("<html><body><h1>GOPHER Report</h1><pre>%s</pre></body></html>",text);
    }

    public static  void showSummaryDialog(String text) {
        Stage window;
        String windowTitle = "GOPHER Report";
        window = new Stage();
        window.setOnCloseRequest( event -> window.close() );
        window.setTitle(windowTitle);

        PopupView view = new PopupView();
        PopupPresenter presenter = (PopupPresenter) view.getPresenter();
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
        presenter.setData(getPreHTML(text));
        presenter.hideButtons();


        window.setScene(new Scene(view.getView()));
        window.showAndWait();
    }


    public static  void showReportListDialog(List<String> reportlist) {
        Stage window;
        String windowTitle = "GOPHER Report";
        window = new Stage();
        window.setOnCloseRequest( event -> window.close() );
        window.setTitle(windowTitle);

        ListView<String> list = new ListView<>();

        ObservableList<String> items =FXCollections.observableArrayList (reportlist);
        list.setItems(items);
        list.setPrefWidth(450);
        list.setPrefHeight(350);
        list.setOrientation(Orientation.VERTICAL);

        StackPane root = new StackPane();
        root.getChildren().add(list);
        window.setScene(new Scene(root, 450, 350));
        window.showAndWait();
    }

    /**
     * Show information to user.
     *
     * @param text        - message text
     * @param windowTitle - Title of PopUp window
     */
    public static void showInfoMessage(String text, String windowTitle) {
        Alert al = new Alert(Alert.AlertType.INFORMATION);
        DialogPane dialogPane = al.getDialogPane();
        dialogPane.getChildren().stream().filter(node -> node instanceof Label).forEach(node -> ((Label)node).setMinHeight(Region.USE_PREF_SIZE));
        al.setTitle(windowTitle);
        al.setHeaderText(null);
        al.setContentText(text);
        al.showAndWait();
    }



    public static void showException(String windowTitle, String header, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(windowTitle);
        alert.setHeaderText(header);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("The exception stacktrace was:");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);
        // Set expandable Exception into the dialog pane.
        alert.getDialogPane().setExpandableContent(textArea);
        alert.getDialogPane().setMinWidth(Region.USE_PREF_SIZE);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }


}

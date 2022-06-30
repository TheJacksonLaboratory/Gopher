package gopher.gui.webpopup;

import javafx.stage.Stage;


import java.util.Map;

public class SettingsPopup extends WebViewerPopup {

    private final String NOT_INITIALIZED = "not initialized";

    private final String html;

    private final Map<String, String> data;


    public SettingsPopup(Map<String, String> data,
                         Stage stage) {
        super(stage);
        this.data = data;
        this.html = getHTML();
    }

    private Map<String, String> getSettingsItems() {
        return data;
    }

    private String getLiRow(Map.Entry<String,String> e) {
        return String.format("<li>%s: %s</li>\n", e.getKey(), e.getValue());
    }

    private String getHTML() {
        Map<String, String> items = getSettingsItems();
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head>");
        sb.append(inlineCSS());
        sb.append("""
                </head>
                <body><h2>Gopher Settings</h2>
                <p>These parameters must be set (via the Setup menu) before annotating.</p>
                <p><ul>
                """);
        for (var e : items.entrySet()) {
            sb.append(getLiRow(e));
        }
        sb.append("""
                </ul></p>
                </body></html>
                                
                """);
        return sb.toString();
    }

    protected String inlineCSS() {
        return "<style>\n" +
                "  html { margin: 20; padding: 20; }" +
                "body { font: 100% georgia, sans-serif; line-height: 1.88889;color: #001f3f; margin: 0; padding: 0; }"+
                "p { margin-top: 10;text-align: justify;}"+
                "h2 {font-family: 'serif';font-size: 1.4em;font-style: normal;font-weight: bold;"+
                "letter-spacing: 1px; margin-bottom: 0; color: #001f3f;}"+
                "  </style>";
    }

    @Override
    public void popup() {
        showHtmlContent("Settings", html);
    }
}

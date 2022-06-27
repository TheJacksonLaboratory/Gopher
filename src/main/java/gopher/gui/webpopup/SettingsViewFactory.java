package gopher.gui.webpopup;

import gopher.service.GopherService;
import gopher.service.model.Approach;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Class to confirmDialog the current user-chosen settings for analysis.
 * @author Peter Robinson
 * @version 0.0.3 (2017-10-20).
 */
public class SettingsViewFactory {



    public static void showSettings(GopherService service) {
        Stage window;
        String windowTitle = "GOPHER Settings";
        window = new Stage();
        window.setOnCloseRequest( event -> window.close());
        window.setTitle(windowTitle);
        window.setHeight(900);
        Map<String,String> settingsMap=getSettingsMap(service);
        SettingsPopup popup = new SettingsPopup(settingsMap, window);
        popup.popup();
    }

    private static Map<String,String> getSettingsMap(GopherService service) {
        Map<String,String> orderedmap = new LinkedHashMap<>();
        if (service == null) return orderedmap; // not initialized yet
        orderedmap.put("Genome build", service.getGenomeBuild());
        orderedmap.put("Path to genome directory", service.getGenomeDirectoryPath());
        orderedmap.put("Genome unpacked?", service.isGenomeUnpacked() ? "yes":"no");
        orderedmap.put("Genome indexed?", service.isGenomeIndexed() ? "yes":"no");
        orderedmap.put("Transcript file", service.getTranscriptsBasename());
        orderedmap.put("Targets path", service.getTargetGenesPath());
        orderedmap.put("RefGene path", service.getRefGenePath());
        orderedmap.put("Alignability map", service.getAlignabilityMapPathIncludingFileNameGz());
        orderedmap.put("Approach" , service.getApproach().toString());
        if (service.getApproach().equals(Approach.SIMPLE)) {
            orderedmap.put("Allow patching?", service.getAllowPatching()? "yes":"no");
        }
        orderedmap.put("Upstream size", String.format("%s bp", service.getSizeUp()));
        orderedmap.put("Downstream size", String.format("%s bp", service.getSizeDown()));
        orderedmap.put("Minimum fragment size", String.format("%s bp", service.getMinFragSize()));
        orderedmap.put("Minimum probe/fragment count", String.valueOf(service.getMinBaitCount()));
        orderedmap.put("Max. k-mer alignability", String.valueOf(service.getMaxMeanKmerAlignability()));
        orderedmap.put("Allow unbalanced margins?", service.getAllowUnbalancedMargins()? "yes":"no");
        orderedmap.put("Restriction enzymes", service.getAllSelectedEnzymeString());
        return orderedmap;
    }





}

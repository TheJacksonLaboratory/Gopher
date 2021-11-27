package gopher.gui.webpopup;

import gopher.gui.webpopup.SettingsPopup;
import gopher.model.Model;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Class to confirmDialog the current user-chosen settings for analysis.
 * @author Peter Robinson
 * @version 0.0.3 (2017-10-20).
 */
public class SettingsViewFactory {


    public static void showSettings(Model model) {
        Stage window;
        String windowTitle = "GOPHER Settings";
        window = new Stage();
        window.setOnCloseRequest( event -> window.close());
        window.setTitle(windowTitle);

        Map<String,String> settingsMap=getSettingsMap(model);
        SettingsPopup popup = new SettingsPopup(settingsMap, window);
        popup.popup();
    }

    private static Map<String,String> getSettingsMap(Model model) {
        Map<String,String> orderedmap = new LinkedHashMap<>();
        if (model == null) return orderedmap; // not initialized yet
        orderedmap.put("Genome build",model.getGenomeBuild());
        orderedmap.put("Path to genome directory",model.getGenomeDirectoryPath());
        orderedmap.put("Genome unpacked?",model.isGenomeUnpacked() ? "yes":"no");
        orderedmap.put("Genome indexed?",model.isGenomeIndexed() ? "yes":"no");
        orderedmap.put("Transcript file",model.getTranscriptsBasename());
        orderedmap.put("Targets path",model.getTargetGenesPath());
        orderedmap.put("RefGene path", model.getRefGenePath());
        orderedmap.put("Alignability map",model.getAlignabilityMapPathIncludingFileNameGz());
        orderedmap.put("Approach" ,model.getApproach().toString());
        if (model.getApproach().equals(Model.Approach.SIMPLE)) {
            orderedmap.put("Allow patching?", model.getAllowPatching()? "yes":"no");
        }
        orderedmap.put("Upstream size", String.format("%s bp",model.getSizeUp()));
        orderedmap.put("Downstream size", String.format("%s bp",model.getSizeDown()));
        orderedmap.put("Minimum fragment size", String.format("%s bp",model.getMinFragSize()));
        orderedmap.put("Minimum probe/fragment count", String.valueOf(model.getMinBaitCount()));
        orderedmap.put("Max. k-mer alignability", String.valueOf(model.getMaxMeanKmerAlignability()));
        orderedmap.put("Allow unbalanced margins?", model.getAllowUnbalancedMargins()? "yes":"no");
        orderedmap.put("Restriction enzymes", model.getAllSelectedEnzymeString());
        return orderedmap;
    }





}

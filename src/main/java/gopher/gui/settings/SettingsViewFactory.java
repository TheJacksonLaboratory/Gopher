package gopher.gui.settings;

import gopher.model.Model;
import gopher.model.RestrictionEnzyme;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;


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
        Map<String,String> settingsMap=getSettingsMap(model);
        presenter.setSettingsMap(settingsMap);

        window.setScene(new Scene(view.getView()));
        window.showAndWait();
    }

    private static Map<String,String> getSettingsMap(Model model) {
        Map<String,String> orderedmap = new LinkedHashMap<>();
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
        orderedmap.put("Allow unbalanced margins?", model.getAllowSingleMargin()? "yes":"no");
        orderedmap.put("Restriction enzymes", model.getAllSelectedEnzymeString());
        return orderedmap;
    }





}

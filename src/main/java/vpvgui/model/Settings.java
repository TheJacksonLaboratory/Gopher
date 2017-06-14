package vpvgui.model;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import vpvgui.io.Platform;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class bundles together the project settings specified by the user, including
 *    project name
 *    path from which to download genome file
 *    path from which to download transcripts file
 *    path from which to download repeats file
 *    path where genome file should be stored
 *    path where transcripts file should be stored
 *    path where repeats file should be stored
 *    list of restriction enzymes
 *    list of target genes
 *
 * @author Hannah Blau (blauh)
 * @version last modified 6/12/17
 */
public class Settings implements Serializable {

    public static final long versionID = 1;

    /* Project name
     */
    private StringProperty projectName = new SimpleStringProperty(this, "projectName");

    public final String getProjectName() {
        return projectName.getValue();
    }

    public final void setProjectName(String pn) {
        projectName.setValue(pn);
    }

    public final StringProperty projectNameProperty() {
        return projectName;
    }

    /* Path from which to download genome file
     */
    private StringProperty genomeFileFrom = new SimpleStringProperty(this, "genomeFileFrom");

    public final String getGenomeFileFrom() {
        return genomeFileFrom.getValue();
    }

    public final void setGenomeFileFrom(String gff) {
        genomeFileFrom.setValue(gff);
    }

    public final StringProperty genomeFileFromProperty() {
        return genomeFileFrom;
    }

    /* Path from which to download transcripts file
     */
    private StringProperty transcriptsFileFrom = new SimpleStringProperty(this, "transcriptsFileFrom");

    public final String getTranscriptsFileFrom() {
        return transcriptsFileFrom.getValue();
    }

    public final void setTranscriptsFileFrom(String tff) {
        transcriptsFileFrom.setValue(tff);
    }

    public final StringProperty transcriptsFileFromProperty() {
        return transcriptsFileFrom;
    }

    /* Path from which to download repeats file
     */
    private StringProperty repeatsFileFrom = new SimpleStringProperty(this, "repeatsFileFrom");

    public final String getRepeatsFileFrom() {
        return repeatsFileFrom.getValue();
    }

    public final void setRepeatsFileFrom(String rff) {
        repeatsFileFrom.setValue(rff);
    }

    public final StringProperty repeatsFileFromProperty() {
        return repeatsFileFrom;
    }

    /* Path where genome file should be stored
     */
    private StringProperty genomeFileTo = new SimpleStringProperty(this, "genomeFileTo");

    public final String getGenomeFileTo() {
        return genomeFileTo.getValue();
    }

    public final void setGenomeFileTo(String gft) {
        genomeFileTo.setValue(gft);
    }

    public final StringProperty genomeFileToProperty() {
        return genomeFileTo;
    }

    /* Path where transcripts file should be stored
     */
    private StringProperty transcriptsFileTo = new SimpleStringProperty(this, "transcriptsFileTo");

    public final String getTranscriptsFileTo() {
        return transcriptsFileTo.getValue();
    }

    public final void setTranscriptsFileTo(String tft) {
        transcriptsFileTo.setValue(tft);
    }

    public final StringProperty transcriptsFileToProperty() {
        return transcriptsFileTo;
    }

    /* Path where repeats file should be stored
     */
    private StringProperty repeatsFileTo = new SimpleStringProperty(this, "repeatsFileTo");

    public final String getRepeatsFileTo() {
        return repeatsFileTo.getValue();
    }

    public final void setRepeatsFileTo(String rft) {
        repeatsFileTo.setValue(rft);
    }

    public final StringProperty repeatsFileToProperty() {
        return repeatsFileTo;
    }

    private ListProperty<String> restrictionEnzymesList =
            new SimpleListProperty<>(this, "restrictionEnzymesList");

    public final ObservableList<String> getRestrictionEnzymesList() { return restrictionEnzymesList.getValue(); }

    public final void setRestrictionEnzymesList(ObservableList<String> rel) {
        restrictionEnzymesList.setValue(rel);
    }

    public final ListProperty restrictionEnzymesListProperty() { return restrictionEnzymesList; }

    private ListProperty<String> targetGenesList =
            new SimpleListProperty<>(this, "targetGenesList");

    public final ObservableList<String> getTargetGenesList() { return targetGenesList.getValue(); }

    public final void setTargetGenesList(ObservableList<String> tgl) { targetGenesList.setValue(tgl); }

    public final ListProperty targetGenesListProperty() { return targetGenesList; }

    private static final String UNSPEC = "unspecified";

    public Settings() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Settings settings = (Settings) o;

        return getProjectName().equals(settings.getProjectName()) &&
                getGenomeFileFrom().equals(settings.getGenomeFileFrom()) &&
                getTranscriptsFileFrom().equals(settings.getTranscriptsFileFrom()) &&
                getRepeatsFileFrom().equals(settings.getRepeatsFileFrom()) &&
                getGenomeFileTo().equals(settings.getGenomeFileTo()) &&
                getTranscriptsFileTo().equals(settings.getTranscriptsFileTo()) &&
                getRepeatsFileTo().equals(settings.getRepeatsFileTo()) &&
                getRestrictionEnzymesList().equals(settings.getRestrictionEnzymesList()) &&
                getTargetGenesList().equals(settings.getTargetGenesList());
/*
        if (!getProjectName().equals(settings.getProjectName())) return false;
        if (!getGenomeFileFrom().equals(settings.getGenomeFileFrom())) return false;
        if (!getTranscriptsFileFrom().equals(settings.getTranscriptsFileFrom())) return false;
        if (!getRepeatsFileFrom().equals(settings.getRepeatsFileFrom())) return false;
        if (!getGenomeFileTo().equals(settings.getGenomeFileTo())) return false;
        if (!getTranscriptsFileTo().equals(settings.getTranscriptsFileTo())) return false;
        if (!getRepeatsFileTo().equals(settings.getRepeatsFileTo())) return false;
        if (!getRestrictionEnzymesList().equals(settings.getRestrictionEnzymesList())) return false;
        return getTargetGenesList().equals(settings.getTargetGenesList());
*/
    }

    @Override
    public int hashCode() {
        int result = getProjectName().hashCode();
        result = 31 * result + getGenomeFileFrom().hashCode();
        result = 31 * result + getTranscriptsFileFrom().hashCode();
        result = 31 * result + getRepeatsFileFrom().hashCode();
        result = 31 * result + getGenomeFileTo().hashCode();
        result = 31 * result + getTranscriptsFileTo().hashCode();
        result = 31 * result + getRepeatsFileTo().hashCode();
        result = 31 * result + getRestrictionEnzymesList().hashCode();
        result = 31 * result + getTargetGenesList().hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        //sb.append("Current settings:");
        sb.append(String.format("\nProject name: %s", makeHumanReadable(getProjectName())));
        sb.append(String.format("\nGenome file source: %s", makeHumanReadable(getGenomeFileFrom())));
        sb.append(String.format("\nTranscripts file source: %s", makeHumanReadable(getTranscriptsFileFrom())));
        sb.append(String.format("\nRepeats file source: %s", makeHumanReadable(getRepeatsFileFrom())));
        sb.append(String.format("\nGenome file destination: %s", makeHumanReadable(getGenomeFileTo())));
        sb.append(String.format("\nTranscripts file destination: %s", makeHumanReadable(getTranscriptsFileTo())));
        sb.append(String.format("\nRepeats file destination: %s\n", makeHumanReadable(getRepeatsFileTo())));
        sb.append(toStringHelper("Restriction Enzymes", getRestrictionEnzymesList()));
        sb.append(toStringHelper("Target Genes", getTargetGenesList()));
        return sb.toString();
    }

    /*
     * Formats the list properties in human-readable format, checking for an empty list.
     */
    private String toStringHelper(String listName, ObservableList<String> lst) {
        if (lst==null || lst.isEmpty()) {
            return (String.format("%s: %s\n", listName, UNSPEC));
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s: (%d)\n", listName, lst.size()));
        for (String s : lst) {
            sb.append(String.format("\t%s\n", s));
        }
        return sb.toString();
    }

    private static String makeComputerReadable(String s) {
        return s.equals(UNSPEC) ? "" : s;
    }

    private static String makeHumanReadable(String s) {
        if (s==null || s.isEmpty()) return UNSPEC;
        else return s;
    }

    /**
     * Creates new Settings object to be populated from gui or from settings saved in file
     * @return empty Settings object
     */
    public static Settings factory() {
        Settings settings = new Settings();

        settings.setProjectName("");
        settings.setGenomeFileFrom("");
        settings.setTranscriptsFileFrom("");
        settings.setRepeatsFileFrom("");
        settings.setGenomeFileTo("");
        settings.setTranscriptsFileTo("");
        settings.setRepeatsFileTo("");
        settings.setRestrictionEnzymesList(FXCollections.observableArrayList());
        settings.setTargetGenesList(FXCollections.observableArrayList());
        return settings;
    }

    /**
     * Creates new Settings object by reading from specified settings file
     *
     * @param path file from which elements of the Settings object are read
     * @return Settings object populated from file
     */
    public static Settings factory(String path) {
        Settings settings = factory();
        String line;
        int colonIndex;

        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            while ((line = br.readLine()) != null) {
                colonIndex = line.indexOf(':');
                if (colonIndex < 1) {
                    System.out.println("[Settings.factory] Could not read settings line: " + line);
                    return null;
                }
                switch (line.substring(0, colonIndex)) {
                    case "Project name" :
                        settings.setProjectName(makeComputerReadable(line.substring(colonIndex + 1).trim()));
                        break;
                    case "Genome file source" :
                        settings.setGenomeFileFrom(makeComputerReadable(line.substring(colonIndex + 1).trim()));
                        break;
                    case "Transcripts file source" :
                        settings.setTranscriptsFileFrom(makeComputerReadable(line.substring(colonIndex + 1).trim()));
                        break;
                    case "Repeats file source" :
                        settings.setRepeatsFileFrom(makeComputerReadable(line.substring(colonIndex + 1).trim()));
                        break;
                    case "Genome file destination" :
                        settings.setGenomeFileTo(makeComputerReadable(line.substring(colonIndex + 1).trim()));
                        break;
                    case "Transcripts file destination" :
                        settings.setTranscriptsFileTo(makeComputerReadable(line.substring(colonIndex + 1).trim()));
                        break;
                    case "Repeats file destination" :
                        settings.setRepeatsFileTo(makeComputerReadable(line.substring(colonIndex + 1).trim()));
                        break;
                    case "Restriction Enzymes" :
                        readLst(line, br, settings.getRestrictionEnzymesList());
                        break;
                    case "Target Genes" :
                        readLst(line, br, settings.getTargetGenesList());
                        break;
                    default :
                        System.out.println("[Settings.factory] Did not recognize setting: " + line);
                        return null;
                } // end switch
            } // end while
        } catch (IOException e) {
            System.err.println("[Settings.factory] I/O Error reading settings file: " + e.getMessage());
        }
        return settings;
    }

    /**
     * Predicate indicating whether the current Settings object is complete (has no empty properties).
     *
     * @return true if object has no empty properties, false otherwise
     */
    public boolean isComplete() {
        return !(getProjectName().isEmpty() || getGenomeFileFrom().isEmpty() ||
                getTranscriptsFileFrom().isEmpty() || getRepeatsFileFrom().isEmpty() ||
                getGenomeFileTo().isEmpty() || getTranscriptsFileTo().isEmpty() ||
                getRepeatsFileTo().isEmpty() || getRestrictionEnzymesList().isEmpty() ||
                getTargetGenesList().isEmpty());
    }

    /*
     * Reads list of restriction enzymes or target genes (one entry per line, each line starts with a
     * tab character.
     *
     * line is the line with the name of the list and its length
     * br is the BufferedReader from which to read the list
     * lst is the list to which we add each new item read
     */
    private static void readLst(String line, BufferedReader br, ObservableList<String> lst) {
        int lstLength;
        Pattern p = Pattern.compile("\\d+");
        Matcher m = p.matcher(line);
        String nextLine;

        // if the lst is unspecified, there is nothing to do
        if (!line.endsWith(UNSPEC)) {
            if (m.find()) {
                lstLength = Integer.valueOf(m.group());

                try {
                    for (int i = 0; i < lstLength; i++) {
                        nextLine = br.readLine();
                        lst.add(nextLine.trim());
                    }
                } catch (IOException e) {
                    System.err.println("[Settings.readLst] I/O Error reading settings file: " + e.getMessage());
                }
            }
            else {
                System.err.println("[Settings.readLst] Cannot find list length: " + line);
            }
        }
    }

    /**
     * Saves settings to specified file.
     *
     * @param settings Settings object to be saved
     * @param settingsFile File to which settings should be saved
     * @return true if settings were successfully saved, false otherwise
     */
    public static boolean saveToFile(Settings settings, File settingsFile) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(settingsFile));
            // strip off the leading newline (and the ending one)
            bw.write(settings.toString().trim());
            // restore the ending newline
            bw.newLine();
            bw.close();
        } catch (IOException e) {
            System.err.println("[Settings.saveToFile] I/O Error writing settings file: " + e.getMessage());
            return false;
        }
        return true;
    }

    /** Ugly hack todo -- in future, allow user to choose project file */
    public String getDefaultPath() {
        File dir = Platform.getVPVDir();
        return String.format("%s%s.vpvsettings%s",dir.getAbsoluteFile(),File.separator,Model.PROJECT_FILENAME_SUFFIX);
    }
}
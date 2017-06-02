package vpvgui.model;

/* public class VPVSettings {
    private List<String> restrictionEnzymes;
    private List<String> targetGenes;
}
*/


import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.*;

public class Settings {

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
        return transcriptsFileFrom.getValue();
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

    public final void setGenomeFileTo(String gff) {
        genomeFileTo.setValue(gff);
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

    public final void setTranscriptsFileTo(String tff) {
        transcriptsFileTo.setValue(tff);
    }

    public final StringProperty transcriptsFileToProperty() {
        return transcriptsFileTo;
    }

    /* Path where repeats file should be stored
     */
    private StringProperty repeatsFileTo = new SimpleStringProperty(this, "repeatsFileTo");

    public final String getRepeatsFileTo() {
        return transcriptsFileTo.getValue();
    }

    public final void setRepeatsFileTo(String rff) {
        repeatsFileTo.setValue(rff);
    }

    public final StringProperty repeatsFileToProperty() {
        return repeatsFileTo;
    }


    public Settings() {
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        //sb.append("Current settings:");
        sb.append(String.format("\nProject name: %s", getProjectName()));
        sb.append(String.format("\nGenome file source: %s", getGenomeFileFrom()));
        sb.append(String.format("\nTranscripts file source: %s", getTranscriptsFileFrom()));
        sb.append(String.format("\nRepeats file source: %s", getRepeatsFileFrom()));
        sb.append(String.format("\nGenome file destination: %s", getGenomeFileTo()));
        sb.append(String.format("\nTranscripts file destination: %s", getTranscriptsFileTo()));
        sb.append(String.format("\nRepeats file destination: %s", getRepeatsFileTo()));
        sb.append("\n");
        return sb.toString();
    }

    private static String[] readPair(String line) {
        int i = line.indexOf(':');
        if (i < 0) {
            System.out.println("[WARN] Could not read settings line: " + line);
            return null;
        }
        String pair[] = new String[2];
        pair[0] = line.substring(0, i).trim();
        pair[1] = line.substring(i + 1).trim();
        return pair;
    }

    public static Settings factory(String path) {
        Settings settings = new Settings();

        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line;
            while ((line = br.readLine()) != null) {
                String pair[] = readPair(line);
                if (pair == null)
                    continue;
                if (pair[0].toLowerCase().contains("Project name")) {
                    settings.setProjectName(pair[1]);
                } else if (pair[0].toLowerCase().contains("genome file source")) {
                    settings.setGenomeFileFrom(pair[1]);
                } else if (pair[0].toLowerCase().contains("transcripts file source")) {
                    settings.setTranscriptsFileFrom(pair[1]);
                } else if (pair[0].toLowerCase().contains("repeats file source")) {
                    settings.setRepeatsFileFrom(pair[1]);
                } else if (pair[0].toLowerCase().contains("genome file destination")) {
                    settings.setGenomeFileTo(pair[1]);
                } else if (pair[0].toLowerCase().contains("transcripts file destination")) {
                    settings.setTranscriptsFileTo(pair[1]);
                } else if (pair[0].toLowerCase().contains("repeats file destination")) {
                    settings.setRepeatsFileTo(pair[1]);
                } else {
                    System.err.println("Did not recognize setting: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        return settings;

    }

    public static boolean saveToFile(Settings settings, File settingsFile) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(settingsFile));
            bw.write(String.format("Project name: %s\n", settings.getProjectName()));
            bw.write(String.format("Genome file source: %s\n", settings.getGenomeFileFrom()));
            bw.write(String.format("Transcripts file source: %s", settings.getTranscriptsFileFrom()));
            bw.write(String.format("Repeats file source: %s", settings.getRepeatsFileFrom()));
            bw.write(String.format("Genome file destination: %s\n", settings.getGenomeFileTo()));
            bw.write(String.format("Transcripts file destination: %s", settings.getTranscriptsFileTo()));
            bw.write(String.format("Repeats file destination: %s", settings.getRepeatsFileTo()));
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
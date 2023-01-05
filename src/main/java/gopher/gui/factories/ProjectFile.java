package gopher.gui.factories;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * This is a wrapper around the old project files that we will show the user to allow the user to delete
 * old project files.
 */
public class ProjectFile {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectFile.class.getName());
    private final StringProperty absolutePath;

    public ProjectFile(File path) {
        absolutePath = new SimpleStringProperty(path.getAbsolutePath());
    }

    public void deleteFile() {
        LOGGER.trace(String.format("Deleting project files %s",absolutePath.getValue()));
        (new File(absolutePath.getValue())).delete();
    }

    public String getProjectName() {
        return (new File(absolutePath.getValue())).getName();
    }
}

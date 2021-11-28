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
    static Logger logger = LoggerFactory.getLogger(ProjectFile.class.getName());
    private StringProperty absolutePath;
    /** Has this file been deleted? */
    private boolean isDeleted=false;
    /** Is this the file that is currently active on the GUI? */
    private boolean isActiveFile=false;


    public ProjectFile(String path) {
        absolutePath= new SimpleStringProperty(path);
    }

    public void setActive() { this.isActiveFile=true;}
    public void setInActive() { this.isActiveFile=false; }
    public void deleteFile() {
        logger.trace(String.format("Deleting project files %s",absolutePath.getValue()));
        (new File(absolutePath.getValue())).delete();
    }

    public String getProjectName() {
        return (new File(absolutePath.getValue())).getName();
    }
}

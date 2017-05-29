package vpvgui.model.project;

import javafx.beans.property.StringProperty;

/**
 * This class will help to coordinate and run a project.
 * A project contains
 * <ul>
 *     <li>Settings</li>
 *     <li>Paths to the genome build, the transcript files, the repeatmasker files</li>
 *     <li>A list of genes (provided by theuser through the GUI)</li>
 *     <li>One or more restriction enzymes for the capture C experiment</li>
 *     <li>A list of view points that contain restriction fragments and can be chosen by the user</li>
 * </ul>
 * The user can generate statistics about the view points and their properties
 */
public class Project {

    private StringProperty projectName;

    private StringProperty outputPath;

    private StringProperty genomeBuild;

    private final int maximumViewpointSize=10000;

    public  Project(){

    }
}

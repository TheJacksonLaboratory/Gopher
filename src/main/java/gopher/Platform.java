package gopher;

/*
 * #%L
 * PhenoteFX
 * %%
 * Copyright (C) 2017 Peter Robinson
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import gopher.gui.factories.ProjectFile;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Platform {
    private static final Logger LOGGER = LoggerFactory.getLogger(Platform.class);

    private static final String projectDirname = "projects";

    /**
     * Get path to directory where HRMD-gui stores global settings.
     * The path depends on underlying operating system. Linux, Windows & OSX
     * currently supported.
     * @return File to directory
     */
    public static File getGopherDir() {
        CurrentPlatform platform = figureOutPlatform();
        File linuxPath = new File(System.getProperty("user.home") + File.separator + ".gopher");
        File windowsPath = new File(System.getProperty("user.home") + File.separator + "gopher");
        File osxPath = new File(System.getProperty("user.home") + File.separator + ".gopher");

        switch (platform) {
            case LINUX: return linuxPath;
            case WINDOWS: return windowsPath;
            case OSX: return osxPath;
            case UNKNOWN: return null;
            default:
                Alert a = new Alert(AlertType.ERROR);
                a.setTitle("Find gui config dir");
                a.setHeaderText(null);
                a.setContentText(String.format("Unrecognized platform. %s", platform));
                a.showAndWait();
                return null;
        }
    }

    /**
     * Get the absolute path to the log file.
     * @return the absolute path,e.g., /home/user/.vpvgui/vpvgui.log
     */
    public static String getAbsoluteLogPath() {
        File dir = getGopherDir();
        return dir + File.separator +  "gopher.log";
    }



    public static File getParametersFile() {
        String parametersFileName = "parameters.yml";
        return new File(getGopherDir() + File.separator + parametersFileName);
    }


    /* Based on this post: http://www.mkyong.com/java/how-to-detect-os-in-java-systemgetpropertyosname/ */
    private static CurrentPlatform figureOutPlatform() {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            return CurrentPlatform.LINUX;
        } else if (osName.contains("win")) {
            return CurrentPlatform.WINDOWS;
        } else if (osName.contains("mac")) {
            return CurrentPlatform.OSX;
        } else {
            return CurrentPlatform.UNKNOWN;
        }
    }



    public static boolean isMacintosh() {
        return figureOutPlatform().equals(CurrentPlatform.OSX);
    }





    private enum CurrentPlatform {
        LINUX("Linux"),
        WINDOWS("Windows"),
        OSX("Mac OSX"),
        UNKNOWN("Unknown");
        private final String name;

        CurrentPlatform(String n) {this.name = n; }

        @Override
        public String toString() { return this.name; }
    }


    public static List<ProjectFile> getProjectFiles() {
        List<ProjectFile> pfiles = new ArrayList<>();
        File gopherDir = getGopherDir();
        String[] contents = gopherDir.list();
        for (String content : contents) {
            LOGGER.trace("Project file: {}", content);
            if (content.endsWith(".ser")) {
                File f = new File(gopherDir + File.separator + content);
                pfiles.add(new ProjectFile(f));
            }
        }

    return pfiles;
    }



}

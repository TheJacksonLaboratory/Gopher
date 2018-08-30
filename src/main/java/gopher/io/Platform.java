package gopher.io;


import java.io.File;
import java.io.IOException;

/**
 * This is used to figure out where Gopher will store the viewpoint files. For instance, with linux
 * this would be /home/username/.gopher/...
 */
public class Platform {

    /**
     * This method creates directory where Gopher stores global settings, if the directory does not exist.
     *
     * @throws IOException if it is not possible to create the Gopher directory
     */
    public static void createGopherDir() throws IOException {
        File target = getGopherDir();

        if (target == null)
            throw new IOException("Operating system not recognized. Supported systems: WINDOWS, OSX, LINUX");

        if (!target.isDirectory()) { // either does not exist or is not a directory
            boolean success = target.mkdirs();
            if (!success)
                throw new IOException("Unable to create directory for storing Gopher settings at '" + target.getAbsolutePath() + "'");
        }
    }


    /**
     * Get path to directory where Gopher stores global settings.
     * The path depends on underlying operating system. Linux, Windows and OSX currently supported.
     *
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
                return null;
        }
    }

    /**
     * Get the absolute path to the viewpoint file, which is a serialized Java file (suffix {@code .ser}).
     * @param basename The plain viewpoint name, e.g., human37cd4
     * @return the absolute path,e.g., /home/user/data/immunology/human37cd4.ser
     */
    public static String getAbsoluteProjectPath(String basename) {
        File dir = getGopherDir();
        return new String(dir + File.separator + basename + ".ser");
    }

    /**
     * Get the absolute path to the log file.
     * @return the absolute path,e.g., /home/user/.gopher/gopher.log
     */
    public static String getAbsoluteLogPath() {
        File dir = getGopherDir();
        return new String(dir + File.separator +  "gopher.log");
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


    private enum CurrentPlatform {
        LINUX("Linux"),
        WINDOWS("Windows"),
        OSX("Os X"),
        UNKNOWN("Unknown");

        private String name;

        CurrentPlatform(String n) {this.name = n; }

        @Override
        public String toString() { return this.name; }
    }

}

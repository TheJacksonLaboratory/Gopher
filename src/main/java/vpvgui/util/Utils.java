package vpvgui.util;

import java.util.List;
import java.util.StringJoiner;

public class Utils {



    public static String join(List<String> lst, String delim) {
        StringJoiner stringJoiner = new StringJoiner(delim);
        for (String item:lst) {
            stringJoiner.add(item);
        }
        return  stringJoiner.toString();
    }

    /**
     *
     * @param proxy http/https proxy to be set
     * @param port corresponding port.
     */
    public static void setSystemProxyAndPort(String proxy,String port) {
        System.setProperty("http.proxyHost",proxy);
        System.setProperty("http.proxyPort",port);
        System.setProperty("https.proxyHost",proxy);
        System.setProperty("https.proxyPort",port);
    }


}

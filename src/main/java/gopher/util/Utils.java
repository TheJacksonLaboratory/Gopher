package gopher.util;

public class Utils {


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

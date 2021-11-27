package gopher.service.model;

public class ProxyResults {

    private final String proxy;

    private final String port;


    public ProxyResults(String proxy, String proxyPort) {
        this.proxy = proxyPort;
        this.port = proxyPort;
    }

    public String getProxy() {
        return proxy;
    }

    public String getPort() {
        return port;
    }
}

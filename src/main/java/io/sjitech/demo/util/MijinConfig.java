package io.sjitech.demo.util;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by wang on 2016/07/15.
 */
@Component
@ConfigurationProperties(prefix="mijin")
public class MijinConfig {

    private String serverNodeIp;

    private int serverNodePort;

    private String serverNodeProtocol;

    private int connectionTimeout;

    private int socketTimeout;

    private int requestTimeout;

    private List<String> existAccountPrivateKeys;

    public String getServerNodeProtocol() {
        return serverNodeProtocol;
    }

    public void setServerNodeProtocol(String serverNodeProtocol) {
        this.serverNodeProtocol = serverNodeProtocol;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public int getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public List<String> getExistAccountPrivateKeys() {
        return existAccountPrivateKeys;
    }

    public void setExistAccountPrivateKeys(List<String> existAccountPrivateKeys) {
        this.existAccountPrivateKeys = existAccountPrivateKeys;
    }

    public String getServerNodeIp() {
        return serverNodeIp;
    }

    public void setServerNodeIp(String serverNodeIp) {
        this.serverNodeIp = serverNodeIp;
    }

    public int getServerNodePort() {
        return serverNodePort;
    }

    public void setServerNodePort(int serverNodePort) {
        this.serverNodePort = serverNodePort;
    }
}

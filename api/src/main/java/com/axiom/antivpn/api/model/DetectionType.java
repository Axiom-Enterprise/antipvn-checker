package com.axiom.antivpn.api.model;

public enum DetectionType {
    VPN,
    PROXY,
    TOR,
    DATACENTER,
    RESIDENTIAL,
    MOBILE;

    public boolean isBlocked(VpnResponse response) {
        return switch (this) {
            case VPN -> response.vpn();
            case PROXY -> response.proxy();
            case TOR -> response.tor();
            case DATACENTER -> response.datacenter();
            case RESIDENTIAL -> response.residential();
            case MOBILE -> response.mobile();
        };
    }
}

package com.axiom.antivpn.minestom;

/** Minestom integration entrypoint; register it from the server bootstrap. */
public final class AxiomAntiVpnMinestom {
    private AxiomAntiVpnMinestom() { }

    public static AxiomAntiVpnMinestom create() {
        return new AxiomAntiVpnMinestom();
    }
}

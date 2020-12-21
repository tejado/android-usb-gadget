package net.tjado.usbgadget;

public interface GadgetAdapterClickInterface {

    void onGadgetClicked(GadgetObject gadget);
    void onStatusChange( GadgetObject gadget, Boolean newStatus);
}

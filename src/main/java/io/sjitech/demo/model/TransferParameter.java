package io.sjitech.demo.model;

import java.util.List;

/**
 * Created by wang on 2016/08/24.
 */
public class TransferParameter {

    // recipient address
    private String address;

    // !!! XEM, when send mosaic, mean change to multiply
    private long amount;

    private String message;

    private boolean secureMessage;

    // recipient public key, needed when secureMessage is true
    private String publicKeyHexString;

    public String getPublicKeyHexString() {
        return publicKeyHexString;
    }

    public void setPublicKeyHexString(String publicKeyHexString) {
        this.publicKeyHexString = publicKeyHexString;
    }

    private List<MosaicParameter> mosaics;

    public boolean isSecureMessage() {
        return secureMessage;
    }

    public void setSecureMessage(boolean secureMessage) {
        this.secureMessage = secureMessage;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<MosaicParameter> getMosaics() {
        return mosaics;
    }

    public void setMosaics(List<MosaicParameter> mosaics) {
        this.mosaics = mosaics;
    }
}

package io.sjitech.demo.model;

import org.nem.core.model.ncc.AccountMetaDataPair;

/**
 * Created by wang on 2016/07/19.
 */
public class MijinAccount {

    private String privateKey;

    private String publicKey;

    private String address;

    private AccountMetaDataPair accountMetaDataPair;

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public AccountMetaDataPair getAccountMetaDataPair() {
        return accountMetaDataPair;
    }

    public void setAccountMetaDataPair(AccountMetaDataPair accountMetaDataPair) {
        this.accountMetaDataPair = accountMetaDataPair;
    }
}

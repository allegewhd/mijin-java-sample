package io.sjitech.demo.model;

import java.util.List;

/**
 * Created by wang on 2016/08/25.
 */
public class MultisigParameter {

    // singer private key
    private String signerPrivateKey;

    // multisig account public key
    private String multisigPublicKey;

    // add cosigners public key
    private List<String> addCosignerPublicKeys;

    // del cosigners public key
    private List<String> delCosignerPublicKeys;

    // relative change of minimum required cosigner number
    private int relativeMinCosignerNum;

    // inner transfer transaction
    private TransferParameter transferParameter;

    // wrapped transaction hash in MultisigTransaction
    private String innerHashString;

    public String getInnerHashString() {
        return innerHashString;
    }

    public void setInnerHashString(String innerHashString) {
        this.innerHashString = innerHashString;
    }

    public String getSignerPrivateKey() {
        return signerPrivateKey;
    }

    public void setSignerPrivateKey(String signerPrivateKey) {
        this.signerPrivateKey = signerPrivateKey;
    }

    public String getMultisigPublicKey() {
        return multisigPublicKey;
    }

    public void setMultisigPublicKey(String multisigPublicKey) {
        this.multisigPublicKey = multisigPublicKey;
    }

    public List<String> getAddCosignerPublicKeys() {
        return addCosignerPublicKeys;
    }

    public void setAddCosignerPublicKeys(List<String> addCosignerPublicKeys) {
        this.addCosignerPublicKeys = addCosignerPublicKeys;
    }

    public List<String> getDelCosignerPublicKeys() {
        return delCosignerPublicKeys;
    }

    public void setDelCosignerPublicKeys(List<String> delCosignerPublicKeys) {
        this.delCosignerPublicKeys = delCosignerPublicKeys;
    }

    public int getRelativeMinCosignerNum() {
        return relativeMinCosignerNum;
    }

    public void setRelativeMinCosignerNum(int relativeMinCosignerNum) {
        this.relativeMinCosignerNum = relativeMinCosignerNum;
    }

    public TransferParameter getTransferParameter() {
        return transferParameter;
    }

    public void setTransferParameter(TransferParameter transferParameter) {
        this.transferParameter = transferParameter;
    }
}

package io.sjitech.demo.model;

import org.nem.core.model.TransactionTypes;

import java.util.List;

/**
 * Created by wang on 2016/07/16.
 */
public class TransactionData {

    private long id;

    private String hash;

    private long height;

    private long amount;

    private long fee;

    private String recipient;

    private int type;

    private String typeName;

    private String message;

    private int version;

    private String signer;

    private int timestamp;

    private int deadline;

    public String getTypeName() {
        if (type == TransactionTypes.TRANSFER) {
            return "transfer";
        } else if (type == TransactionTypes.PROVISION_NAMESPACE) {
            return "create namespace";
        } else if (type == TransactionTypes.MOSAIC_DEFINITION_CREATION) {
            return "create mosaic";
        } else if (type == TransactionTypes.MULTISIG) {
            return "multisig";
        } else if (type == TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION) {
            return "multisig modification";
        } else if (type == TransactionTypes.MULTISIG_SIGNATURE) {
            return "multisig signature";
        } else if (type == TransactionTypes.MOSAIC_SUPPLY_CHANGE) {
            return "mosaic supply change";
        }

        return "unknown type: " + type;
    }

    private List<MosaicData> mosaics;

    public List<MosaicData> getMosaics() {
        return mosaics;
    }

    public void setMosaics(List<MosaicData> mosaics) {
        this.mosaics = mosaics;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public long getFee() {
        return fee;
    }

    public void setFee(long fee) {
        this.fee = fee;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getSigner() {
        return signer;
    }

    public void setSigner(String signer) {
        this.signer = signer;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public int getDeadline() {
        return deadline;
    }

    public void setDeadline(int deadline) {
        this.deadline = deadline;
    }
}

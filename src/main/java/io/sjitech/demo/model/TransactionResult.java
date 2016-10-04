package io.sjitech.demo.model;

import org.nem.core.crypto.Hash;

/**
 * Created by wang on 2016/07/16.
 */
public class TransactionResult {

    private boolean success;

    private int type;
    private int code;
    private String message;

    private String transactionHash;
    private String innerTransactionHash;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }

    public String getInnerTransactionHash() {
        return innerTransactionHash;
    }

    public void setInnerTransactionHash(String innerTransactionHash) {
        this.innerTransactionHash = innerTransactionHash;
    }
}

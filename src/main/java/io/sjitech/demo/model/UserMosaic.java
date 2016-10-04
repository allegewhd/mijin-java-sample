package io.sjitech.demo.model;

import org.nem.core.model.mosaic.Mosaic;

/**
 * Created by wang on 2016/07/28.
 */
public class UserMosaic {

    private String namespace;

    private String mosaic;

    private long quantity;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getMosaic() {
        return mosaic;
    }

    public void setMosaic(String mosaic) {
        this.mosaic = mosaic;
    }

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }
}

package io.sjitech.demo.model;

/**
 * Created by wang on 2016/08/24.
 */
public class MosaicParameter {

    private String namespace;

    private String mosaic;

    private long quantity;

    private String description;

    private long initialSupply;

    private long divisibility;

    private boolean supplyMutable;

    private boolean transferable;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getInitialSupply() {
        return initialSupply;
    }

    public void setInitialSupply(long initialSupply) {
        this.initialSupply = initialSupply;
    }

    public long getDivisibility() {
        return divisibility;
    }

    public void setDivisibility(long divisibility) {
        this.divisibility = divisibility;
    }

    public boolean isSupplyMutable() {
        return supplyMutable;
    }

    public void setSupplyMutable(boolean supplyMutable) {
        this.supplyMutable = supplyMutable;
    }

    public boolean isTransferable() {
        return transferable;
    }

    public void setTransferable(boolean transferable) {
        this.transferable = transferable;
    }
}

package io.sjitech.demo.model;

import org.nem.core.model.mosaic.MosaicTransferFeeType;

/**
 * Created by wang on 2016/07/27.
 */
public class MijinMosaic {

    private String namespaceId;

    private String name;

    private String creator;

    private long id;

    private String description;

    private int divisibility;

    private long initialSupply;

    private boolean supplyMutable;

    private boolean transferable;

    private boolean hasLevy;

    private MosaicTransferFeeType levyType;

    private String recipientAddress;

    private long levyFee;

    public MosaicTransferFeeType getLevyType() {
        return levyType;
    }

    public void setLevyType(MosaicTransferFeeType levyType) {
        this.levyType = levyType;
    }

    public String getRecipientAddress() {
        return recipientAddress;
    }

    public void setRecipientAddress(String recipientAddress) {
        this.recipientAddress = recipientAddress;
    }

    public long getLevyFee() {
        return levyFee;
    }

    public void setLevyFee(long levyFee) {
        this.levyFee = levyFee;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDivisibility() {
        return divisibility;
    }

    public void setDivisibility(int divisibility) {
        this.divisibility = divisibility;
    }

    public long getInitialSupply() {
        return initialSupply;
    }

    public void setInitialSupply(long initialSupply) {
        this.initialSupply = initialSupply;
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

    public boolean isHasLevy() {
        return hasLevy;
    }

    public void setHasLevy(boolean hasLevy) {
        this.hasLevy = hasLevy;
    }

    public String getNamespaceId() {
        return namespaceId;
    }

    public void setNamespaceId(String namespaceId) {
        this.namespaceId = namespaceId;
    }

}

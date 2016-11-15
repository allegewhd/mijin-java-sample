package io.sjitech.demo.util;

import org.nem.core.node.ApiId;

/**
 * Created by wang on 2016/07/19.
 */
public enum NisExtendApiId implements ApiId {
    /**
     * The /mosaic/definition API
     */
    NIS_REST_MOSAIC_DEFINITION("/mosaic/definition"),

    /**
     * The /mosaic/supply API
     */
    NIS_REST_MOSAIC_SUPPLY("/mosaic/supply"),

    /**
     * Retrieving mosaics that an account owns
     */
    NIS_REST_ACCOUNT_MOSAIC_OWNED("/account/mosaic/owned"),

    /**
     * Retrieving mosaic definitions
     */
    NIS_REST_MOSAIC_DEFINITION_PAGE("/namespace/mosaic/definition/page"),

    /**
     * get namespace data API
     */
    NIS_REST_NAMESPACE("/namespace"),

    /**
     * https://blog.nem.io/nem-updated-0-6-82/
     *
     * There is a new API request /transaction/get to look up a transaction by hash.
     * Example: http://bigalice3.nem.ninja:7890/transaction/get?hash=215b900475b13f724acc9fbe249bb9ffd31451c2352ed51b9637143cde4c260a
     * To fully support hash based transaction lookup, a node must set the entry in the NIS config.properties
     * (config-user.properties) nis.transactionHashRetentionTime to -1.
     *
     * This means all transaction hashes in the blockchain are held in memory and
     * therefore is only recommended for nodes with at least 2GB memory.
     *
     */
    NIS_REST_GET_TRANSACTION_BY_HASH("transaction/get"),

    /**
     * The account/generate API
     */
    NIS_REST_ACCOUNT_GENERATE("/account/generate");


    //endregion

    private final String value;

    /**
     * Creates a NIS API id.
     *
     * @param value The string representation.
     */
    NisExtendApiId(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

}

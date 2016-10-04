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

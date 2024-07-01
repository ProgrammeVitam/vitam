/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */

package fr.gouv.vitam.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * DatabaseCursor class Show database position of request response
 */

// TODO P1 ; refactor to the common vitam

public class DatabaseCursor {

    @JsonProperty("total")
    private long total;

    @JsonProperty("offset")
    private long offset;

    @JsonProperty("limit")
    private long limit;

    @JsonProperty("size")
    private long size;

    @JsonProperty("scrollId")
    @JsonInclude(Include.NON_NULL)
    private String scrollId;

    /**
     * For Json
     */
    protected DatabaseCursor() {
        // Empty
    }

    /**
     * DatabaseCursor constructor
     *
     * @param total total of inserted/modified/selected items
     * @param offset the offset of items in database
     * @param limit number limit of items per response
     */
    public DatabaseCursor(long total, long offset, long limit) {
        this.total = total;
        this.offset = offset;
        this.limit = limit;
        size = total;
    }

    /**
     * DatabaseCursor constructor
     *
     * @param total total of inserted/modified/selected items
     * @param offset the offset of items in database
     * @param limit number limit of items per response
     * @param size size of the current response
     */
    public DatabaseCursor(long total, long offset, long limit, long size) {
        this.total = total;
        this.offset = offset;
        this.limit = limit;
        this.size = size;
    }

    /**
     * DatabaseCursor constructor
     *
     * @param total total of inserted/modified/selected items
     * @param offset the offset of items in database
     * @param limit number limit of items per response
     * @param size size of the current response
     * @param scrollId cursorId of the current response
     */
    public DatabaseCursor(long total, long offset, long limit, long size, String scrollId) {
        this.total = total;
        this.offset = offset;
        this.limit = limit;
        this.size = size;
        this.scrollId = scrollId;
    }

    /**
     * @return the total of units inserted/modified/selected as potential total response size (beyond current limit)
     */
    public long getTotal() {
        return total;
    }

    /**
     * @param total of units as integer
     * @return the DatabaseCursor with the total is setted
     */
    public DatabaseCursor setTotal(long total) {
        this.total = total;
        return this;
    }

    /**
     * @return the offset of units in database
     */
    public long getOffset() {
        return offset;
    }

    /**
     * @param offset the offset of units in database
     * @return the DatabaseCursor with the offset is setted
     */
    public DatabaseCursor setOffset(long offset) {
        this.offset = offset;
        return this;
    }

    /**
     * @return the limit of units per response
     */
    public long getLimit() {
        return limit;
    }

    /**
     * @param limit limit of units as integer
     * @return the DatabaseCursor with the limits of units is setted
     */
    public DatabaseCursor setLimit(long limit) {
        this.limit = limit;
        return this;
    }

    /**
     * @return the size as current response size
     */
    public long getSize() {
        return size;
    }

    /**
     * @param size the size as current response size
     * @return this
     */
    public DatabaseCursor setSize(long size) {
        this.size = size;
        return this;
    }

    /**
     * @return the scrollId as current response size
     */
    public String getScrollId() {
        return this.scrollId;
    }

    /**
     * @param scrollId the cursorId as current response size
     * @return this
     */
    public DatabaseCursor setScrollId(String scrollId) {
        this.scrollId = scrollId;
        return this;
    }
}

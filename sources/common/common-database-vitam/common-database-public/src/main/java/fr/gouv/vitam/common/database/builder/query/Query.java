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
package fr.gouv.vitam.common.database.builder.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERY;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERYARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

import java.util.Date;

/**
 * Query component
 */
public class Query {

    private static final String CANNOT_BE_CREATED_WITH_EMPTY_VARIABLE_NAME =
        " cannot be created with empty variable name";
    private static final String QUERY2 = "Query ";
    /**
     * DATE item
     */
    public static final String DATE = "$date";
    protected ObjectNode currentQuery;
    protected JsonNode currentObject;
    protected QUERY currentTokenQUERY;
    protected boolean isFullText = false;
    protected boolean ready;
    // default is immediate next level
    protected int relativedepth = 1;
    // default is to not specify any exact depth (implicit)
    protected int exactdepth = 0;

    protected int extraInfo;

    /**
     * Empty constructor
     */
    protected Query() {
        currentQuery = JsonHandler.createObjectNode();
        currentObject = currentQuery;
        currentTokenQUERY = null;
        ready = false;
    }

    protected final void createQueryArray(final QUERY query) {
        currentObject = ((ObjectNode) currentObject).putArray(query.exactToken());
    }

    protected final void createQueryVariable(final QUERY query, final String variableName)
        throws InvalidCreateOperationException {
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(QUERY2 + query + CANNOT_BE_CREATED_WITH_EMPTY_VARIABLE_NAME);
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        ((ObjectNode) currentObject).put(query.exactToken(), variableName.trim());
    }

    protected final void createQueryVariableValue(final QUERY query, final String variableName, final long value)
        throws InvalidCreateOperationException {
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(QUERY2 + query + CANNOT_BE_CREATED_WITH_EMPTY_VARIABLE_NAME);
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        currentObject = ((ObjectNode) currentObject).putObject(query.exactToken());
        ((ObjectNode) currentObject).put(variableName.trim(), value);
    }

    protected final void createQueryVariableValue(final QUERY query, final String variableName, final double value)
        throws InvalidCreateOperationException {
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(QUERY2 + query + CANNOT_BE_CREATED_WITH_EMPTY_VARIABLE_NAME);
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        currentObject = ((ObjectNode) currentObject).putObject(query.exactToken());
        ((ObjectNode) currentObject).put(variableName.trim(), value);
    }

    protected final void createQueryVariableValue(final QUERY query, final String variableName, final String value)
        throws InvalidCreateOperationException {
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(QUERY2 + query + CANNOT_BE_CREATED_WITH_EMPTY_VARIABLE_NAME);
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
            GlobalDatas.sanityValueCheck(value);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        currentObject = ((ObjectNode) currentObject).putObject(query.exactToken());
        ((ObjectNode) currentObject).put(variableName.trim(), value);
    }

    protected final void createQueryVariableValue(final QUERY query, final String variableName, final Date value)
        throws InvalidCreateOperationException {
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(QUERY2 + query + CANNOT_BE_CREATED_WITH_EMPTY_VARIABLE_NAME);
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        currentObject = ((ObjectNode) currentObject).putObject(query.exactToken());
        ((ObjectNode) currentObject).set(variableName.trim(), GlobalDatas.getDate(value));
    }

    protected final void createQueryVariableValue(final QUERY query, final String variableName, final boolean value)
        throws InvalidCreateOperationException {
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(QUERY2 + query + CANNOT_BE_CREATED_WITH_EMPTY_VARIABLE_NAME);
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        currentObject = ((ObjectNode) currentObject).putObject(query.exactToken());
        ((ObjectNode) currentObject).put(variableName.trim(), value);
    }

    protected final void createQueryVariableValue(final QUERY query, final String variableName, final JsonNode value)
        throws InvalidCreateOperationException {
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new InvalidCreateOperationException(QUERY2 + query + CANNOT_BE_CREATED_WITH_EMPTY_VARIABLE_NAME);
        }
        try {
            GlobalDatas.sanityParameterCheck(variableName);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
        currentObject = ((ObjectNode) currentObject).putObject(query.exactToken());
        ((ObjectNode) currentObject).put(variableName.trim(), value);
    }

    /**
     * Clean the object
     */
    public void clean() {
        currentQuery.removeAll();
        currentObject = currentQuery;
        currentTokenQUERY = null;
        ready = false;
    }

    /**
     * Removing exact depth and depth
     */
    protected void cleanDepth() {
        currentQuery.remove(QUERYARGS.EXACTDEPTH.exactToken());
        currentQuery.remove(QUERYARGS.DEPTH.exactToken());
        relativedepth = 1;
        exactdepth = 0;
    }

    /**
     * @param exactdepth 0 to ignore
     * @return the single request ready to be added to global Query (remove previous exact depth and depth if any)
     */
    public final Query setExactDepthLimit(final int exactdepth) {
        cleanDepth();
        if (exactdepth != 0) {
            this.exactdepth = exactdepth;
            currentQuery.put(QUERYARGS.EXACTDEPTH.exactToken(), exactdepth);
        }
        return this;
    }

    /**
     * @param relativedepth query's depth
     * @return the single request ready to be added to global Query (remove previous exact depth and depth if any)
     */
    public final Query setRelativeDepthLimit(final int relativedepth) {
        cleanDepth();
        this.relativedepth = relativedepth;
        currentQuery.put(QUERYARGS.DEPTH.exactToken(), relativedepth);
        return this;
    }

    /**
     * Relative Depth, similar to {@link #setRelativeDepthLimit(int)}
     *
     * @param relativedepth query's depth
     * @return the single request ready to be added to global Query (remove previous exact depth and depth if any)
     */
    public final Query setDepthLimit(final int relativedepth) {
        return setRelativeDepthLimit(relativedepth);
    }

    /**
     * @return the currentQuery
     */
    public ObjectNode getCurrentQuery() {
        return currentQuery;
    }

    /**
     * @param key of node
     * @return the root node named key
     */
    public JsonNode getNode(String key) {
        return currentQuery.get(key);
    }

    /**
     * @return the currentObject (internal use only during parse)
     */
    public JsonNode getCurrentObject() {
        return currentObject;
    }

    /**
     * @return the current QUERY
     */
    public QUERY getQUERY() {
        return currentTokenQUERY;
    }

    /**
     * @return the ready
     */
    public boolean isReady() {
        return ready;
    }

    /**
     * @param ready the ready to set
     * @return this QUERY
     */
    protected Query setReady(final boolean ready) {
        this.ready = ready;
        return this;
    }

    /**
     * @param val extra value
     * @return this Query
     */
    public Query setExtraInfo(int val) {
        extraInfo = val;
        return this;
    }

    /**
     * @return the extra info
     */
    public int getExtraInfo() {
        return extraInfo;
    }

    /**
     * @return the isFullText
     */
    public final boolean isFullText() {
        return isFullText;
    }

    /**
     * @param isFullText the isFullText to set
     * @return this
     */
    public final Query setFullText(boolean isFullText) {
        this.isFullText = isFullText;
        return this;
    }

    /**
     * @return the exact depth
     */
    public final int getParserExactdepth() {
        return exactdepth;
    }

    /**
     * @param exactdepth the exact depth to set
     * @return this
     */
    public final Query setParserExactdepth(int exactdepth) {
        this.exactdepth = exactdepth;
        return this;
    }

    /**
     * @return the relative depth
     */
    public final int getParserRelativeDepth() {
        return relativedepth;
    }

    /**
     * @param relativedepth the relative depth to set
     * @return this
     */
    public final Query setParserRelativeDepth(int relativedepth) {
        this.relativedepth = relativedepth;
        return this;
    }

    @Override
    public String toString() {
        return JsonHandler.unprettyPrint(currentQuery);
    }
}

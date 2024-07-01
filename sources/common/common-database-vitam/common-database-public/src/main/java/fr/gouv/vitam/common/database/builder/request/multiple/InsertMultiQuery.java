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
package fr.gouv.vitam.common.database.builder.request.multiple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.GLOBAL;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.MULTIFILTER;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

/**
 * Insert: { $roots: roots, $query : query, $filter : multi, $data : data } or [ roots, query, multi, data ]
 */
public class InsertMultiQuery extends RequestMultiple {

    protected ObjectNode data;

    /**
     * @return this Insert
     */
    public final InsertMultiQuery resetData() {
        if (data != null) {
            data.removeAll();
        }
        return this;
    }

    /**
     * @return this Insert
     */
    @Override
    public final InsertMultiQuery reset() {
        super.reset();
        resetData();
        return this;
    }

    /**
     * @param mult True to act on multiple elements, False to act only on 1 element
     * @return this Insert
     */
    public final InsertMultiQuery setMult(final boolean mult) {
        if (filter == null) {
            filter = JsonHandler.createObjectNode();
        }
        filter.put(MULTIFILTER.MULT.exactToken(), mult);
        return this;
    }

    /**
     * @param filterContent json filter
     * @return this Insert
     */
    public final InsertMultiQuery setMult(final JsonNode filterContent) {
        if (filter == null) {
            filter = JsonHandler.createObjectNode();
        }
        if (filterContent.has(MULTIFILTER.MULT.exactToken())) {
            filter.setAll((ObjectNode) filterContent);
        }
        return this;
    }

    /**
     * @param filterContent json filter
     * @return this Insert
     * @throws InvalidParseOperationException when query is invalid
     */
    @Override
    public final InsertMultiQuery setFilter(final JsonNode filterContent) throws InvalidParseOperationException {
        super.setFilter(filterContent);
        return setMult(filterContent);
    }

    /**
     * Note that if previous attributes have the same name, they will be replaced.
     *
     * @param data list of json data
     * @return this Insert
     */
    public final InsertMultiQuery addData(final ObjectNode... data) {
        if (this.data == null) {
            this.data = JsonHandler.createObjectNode();
        }
        for (final ObjectNode act : data) {
            if (!act.isMissingNode()) {
                this.data.setAll(act);
            }
        }
        return this;
    }

    /**
     * @param dataContent json data
     * @return this Insert
     * @throws InvalidParseOperationException when query is invalid
     */
    public final InsertMultiQuery setData(final JsonNode dataContent) throws InvalidParseOperationException {
        if (data == null) {
            data = JsonHandler.createObjectNode();
        }
        data.setAll((ObjectNode) dataContent);
        return this;
    }

    /**
     * @param data string data
     * @return this Insert
     * @throws InvalidParseOperationException when query is invalid
     */
    public final InsertMultiQuery parseData(final String data) throws InvalidParseOperationException {
        GlobalDatas.sanityValueCheck(data);
        final JsonNode dataContent = JsonHandler.getFromString(data);
        return setData(dataContent);
    }

    /**
     * @return the Final Insert containing all 4 parts: roots, queries array, filter and data
     */
    public final ObjectNode getFinalInsert() {
        final ObjectNode node = getFinal();
        if (data != null && data.size() > 0) {
            node.set(GLOBAL.DATA.exactToken(), data);
        } else {
            node.putObject(GLOBAL.DATA.exactToken());
        }
        return node;
    }

    /**
     * @return the data
     */
    @Override
    public final ObjectNode getData() {
        if (data == null) {
            return JsonHandler.createObjectNode();
        }
        return data;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("INSERT: ").append(super.toString()).append("\n\tData: ").append(data);
        return builder.toString();
    }
}

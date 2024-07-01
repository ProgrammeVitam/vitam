/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.database.parser.request.multiple;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.GLOBAL;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.multiple.InsertMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.RequestMultiple;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameInsertAdapter;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

/**
 * Insert Parser: { $roots: root, $query : query, $filter : filter, $data : data}
 */
public class InsertParserMultiple extends RequestParserMultiple {

    VarNameInsertAdapter insertAdapter;

    /**
     * Should be used in Internal API
     */
    public InsertParserMultiple() {
        super();
        insertAdapter = new VarNameInsertAdapter(adapter);
    }

    /**
     * Should be used in Masterdata or Metadata
     *
     * @param adapter VarNameAdapter
     */
    public InsertParserMultiple(VarNameAdapter adapter) {
        super(adapter);
        insertAdapter = new VarNameInsertAdapter(adapter);
    }

    @Override
    protected RequestMultiple getNewRequest() {
        return new InsertMultiQuery();
    }

    /**
     * @param request containing a parsed JSON as { $roots: root, $query : query, $filter : filter, $data : data}
     * @throws InvalidParseOperationException if request could not parse to JSON
     */
    @Override
    public void parse(final JsonNode request) throws InvalidParseOperationException {
        parseJson(request);
        internalParseInsert();
    }

    /**
     * @throws InvalidParseOperationException
     */
    private void internalParseInsert() throws InvalidParseOperationException {
        // { $roots: root, $query : query, $filter : filter, $data : data }
        dataParse(rootNode.get(GLOBAL.DATA.exactToken()));

        final JsonNode node = getRequest().getData();
        final int nodeDepth = GlobalDatasParser.getJsonNodedepth(node);
        if (nodeDepth >= GlobalDatas.MAXDEPTH) {
            throw new InvalidParseOperationException("Node depth exception, value: " + nodeDepth);
        }
    }

    /**
     * {$data : { field: value, ... }
     *
     * @param rootNode JsonNode
     * @throws InvalidParseOperationException if rootNode could not parse to JSON
     */
    protected void dataParse(final JsonNode rootNode) throws InvalidParseOperationException {
        if (rootNode == null) {
            throw new InvalidParseOperationException("Parse in error for Insert: empty data");
        }
        GlobalDatas.sanityValueCheck(rootNode.toString());
        // Fix varname using adapter
        // TODO P1 Note: values are not changed. This shall be a specific computation
        // For instance: mavar : #id
        final JsonNode newRootNode = insertAdapter.getFixedVarNameJsonNode(rootNode);
        try {
            ((InsertMultiQuery) request).setData(newRootNode);
        } catch (final Exception e) {
            throw new InvalidParseOperationException("Parse in error for Insert: " + rootNode, e);
        }
    }

    @Override
    public String toString() {
        return new StringBuilder().append(request.toString()).append("\n\tLastLevel: ").append(lastDepth).toString();
    }

    @Override
    public InsertMultiQuery getRequest() {
        return (InsertMultiQuery) request;
    }
}

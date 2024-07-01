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
package fr.gouv.vitam.common.database.builder.facet;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FACET;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FACETARGS;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

/**
 * Facet component
 */
public class Facet {

    private String name;
    protected ObjectNode currentFacet;
    protected FACET currentTokenFACET;

    /**
     * Constructor
     *
     * @param name name
     */
    public Facet(String name) {
        this.name = name;
        currentFacet = JsonHandler.createObjectNode();
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return FACET token
     */
    public FACET getCurrentTokenFACET() {
        return currentTokenFACET;
    }

    /**
     * Retrieve the current facet
     *
     * @return facet as an ObjectNode
     */
    public ObjectNode getCurrentFacet() {
        ObjectNode currentNode = JsonHandler.createObjectNode();
        currentNode.put(FACETARGS.NAME.exactToken(), name);
        currentNode.set(currentTokenFACET.exactToken(), currentFacet);
        return currentNode;
    }

    /**
     * Check if parameter is valid
     *
     * @param param parameter name
     * @param value parameter value
     * @throws InvalidCreateOperationException
     */
    protected void checkStringParameterValue(String param, String value) throws InvalidCreateOperationException {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidCreateOperationException(param + " is mandatory in Facet");
        }
        try {
            GlobalDatas.sanityParameterCheck(value);
        } catch (final InvalidParseOperationException e) {
            throw new InvalidCreateOperationException(e);
        }
    }

    @Override
    public String toString() {
        return JsonHandler.unprettyPrint(getCurrentFacet());
    }
}

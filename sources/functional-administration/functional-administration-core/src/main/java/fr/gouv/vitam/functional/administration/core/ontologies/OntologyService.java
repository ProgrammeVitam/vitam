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
package fr.gouv.vitam.functional.administration.core.ontologies;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;

import java.io.IOException;
import java.util.List;

/**
 * This service manages CRUD on Ontologies
 */
public interface OntologyService {
    /**
     * Import a collections of ontologies After passing the validation steps. If all the ontologies are valid, they are
     * stored in the collection and indexed. </BR>
     * The ontologies are not valid in the following situations : </BR>
     * <ul>
     * <li>The json is invalid</li>
     * <li>The json contains an already used identifier</li>
     * <li>One or more mandatory field is missing</li>
     * <li>A field has an invalid format</li>
     * </ul>
     *
     * @param ontologyList the list of ontologies to be imported
     * @param forceUpdate
     * @return RequestResponseOK if success or VitamError
     * @throws VitamException if in error occurs while validating ontologies
     */
    RequestResponse<OntologyModel> importOntologies(boolean forceUpdate, List<OntologyModel> ontologyList)
        throws VitamException, IOException;

    /**
     * Import a collections of ontologies After passing the validation steps. If all the ontologies are valid, they are
     * stored in the collection and indexed.
     * If there is some external ontologies used by vitam at the moment of import, they will be preserved and merged
     * with the new internal ontologies being imported
     *
     * @param ontologyList the list of internal ontologies to be imported
     * @return RequestResponseOK if success or VitamError
     * @throws VitamException if in error occurs while validating ontologies
     */
    RequestResponse<OntologyModel> importInternalOntologies(List<OntologyModel> ontologyList)
        throws VitamException, IOException;

    /**
     * find an ontology by QueryDsl
     *
     * @param queryDsl the query as a json to be executed
     * @return list of OntologyModel
     * @throws ReferentialException thrown if the query could not be executed
     * @throws InvalidParseOperationException thrown if the query could not be executed
     */
    RequestResponseOK<OntologyModel> findOntologies(JsonNode queryDsl)
        throws ReferentialException, InvalidParseOperationException;

    /**
     * find an ontology by QueryDsl (find directly in db)
     *
     * @param queryDsl the query as a json to be executed
     * @return list of OntologyModel
     * @throws ReferentialException thrown if the query could not be executed
     * @throws InvalidParseOperationException thrown if the query could not be executed
     */
    RequestResponseOK<OntologyModel> findOntologiesForCache(JsonNode queryDsl)
        throws ReferentialException, InvalidParseOperationException;

    /**
     * Check the safety of ontology import at vitam reinstall/upgrade
     *
     * @param ontologyList the list of internal ontologies to check
     * @return Response the response of check
     * @throws VitamException if in error occurs while validating ontologies
     */
    RequestResponse<OntologyModel> checkUpgradeOntologies(List<OntologyModel> ontologyList) throws VitamException;
}

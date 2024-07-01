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
package fr.gouv.vitam.functional.administration.client;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.client.OntologyLoader;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.OntologyModel;

import java.util.List;
import java.util.Optional;

public class AdminManagementOntologyLoader implements OntologyLoader {

    private final AdminManagementClientFactory factory;
    private final ObjectNode select;

    public AdminManagementOntologyLoader(AdminManagementClientFactory factory, Optional<String> collectionName) {
        this.factory = factory;
        this.select = getSelectOntology(collectionName);
    }

    private ObjectNode getSelectOntology(Optional<String> collectionName) {
        try {
            Select selectOntologies = new Select();
            if (collectionName.isPresent()) {
                selectOntologies.setQuery(QueryHelper.in(OntologyModel.TAG_COLLECTIONS, collectionName.get()));
            }
            selectOntologies.addUsedProjection(OntologyModel.TAG_IDENTIFIER);
            selectOntologies.addUsedProjection(OntologyModel.TAG_TYPE);
            return selectOntologies.getFinalSelect();
        } catch (InvalidParseOperationException | InvalidCreateOperationException e) {
            throw new VitamRuntimeException(e);
        }
    }

    @Override
    public List<OntologyModel> loadOntologies() {
        try (AdminManagementClient adminClient = factory.getClient()) {
            RequestResponse<OntologyModel> responseOntologies = adminClient.findOntologies(select);
            if (!responseOntologies.isOk()) {
                throw new VitamRuntimeException("Could not load ontologies.");
            }

            return ((RequestResponseOK<OntologyModel>) responseOntologies).getResults();
        } catch (InvalidParseOperationException e) {
            throw new VitamRuntimeException("Could not load ontologies.", e);
        }
    }
}

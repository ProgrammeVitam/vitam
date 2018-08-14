/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.access.internal.core;

import com.fasterxml.jackson.databind.node.ArrayNode;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.action.Action;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.json.SchemaValidationUtils;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.MetadataType;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.OntologyType;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;

import java.util.List;

public class OntologyUtils {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(OntologyUtils.class);


    /**
     * Add ontology's fields to be checked when executing the update.
     * This is done by adding a transient Action to the update query that contains the list of ontologies.
     * The transient action is removed by the executor of the Action (DBRequest)
     * 
     * @param updateParser The parser containing the update Query
     * @throws InvalidCreateOperationException
     * @throws AdminManagementClientServerException
     * @throws InvalidParseOperationException
     */
    public static void addOntologyFieldsToBeUpdated(UpdateParserMultiple updateParser)
            throws InvalidCreateOperationException, AdminManagementClientServerException,
            InvalidParseOperationException {
        UpdateMultiQuery request = updateParser.getRequest();
        Select selectOntologies = new Select();
        List<OntologyModel> ontologyModelList;
        try (AdminManagementClient adminClient = AdminManagementClientFactory.getInstance().getClient()) {
            selectOntologies.setQuery(
                    QueryHelper.and()
                            .add(QueryHelper.in(OntologyModel.TAG_TYPE, OntologyType.DOUBLE.getType(),
                                    OntologyType.BOOLEAN.getType(),
                                    OntologyType.DATE.getType(),
                                    OntologyType.LONG.getType()))
                            .add(QueryHelper.in(OntologyModel.TAG_COLLECTIONS, MetadataType.UNIT.getName()))
            );
            selectOntologies
                    .setProjection(JsonHandler.getFromString("{\"$fields\": { \"Identifier\": 1, \"Type\": 1}}"));
            RequestResponse<OntologyModel> responseOntologies =
                    adminClient.findOntologies(selectOntologies.getFinalSelect());
            if (responseOntologies.isOk() &&
                    ((RequestResponseOK<OntologyModel>) responseOntologies).getResults().size() > 0) {
                ontologyModelList =
                        ((RequestResponseOK<OntologyModel>) responseOntologies).getResults();
            } else {
                // no external ontology, nothing to do
                return;
            }
            if (ontologyModelList.size() > 0) {
                ArrayNode ontologyArrayNode = JsonHandler.createArrayNode();
                ontologyModelList.forEach(ontology -> {
                    try {
                        ontologyArrayNode.add(JsonHandler.toJsonNode(ontology));
                    } catch (InvalidParseOperationException e) {
                        LOGGER.error("could not parse this ontology", e);
                    }
                });
                Action action =
                        new SetAction(SchemaValidationUtils.TAG_ONTOLOGY_FIELDS,
                                JsonHandler.unprettyPrint(ontologyArrayNode));
                request.addActions(action);
            }
        } catch (InvalidCreateOperationException | AdminManagementClientServerException |
                InvalidParseOperationException e) {
            throw e;
        }
    }
}

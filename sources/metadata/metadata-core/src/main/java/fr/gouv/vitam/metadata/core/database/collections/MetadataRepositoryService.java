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
package fr.gouv.vitam.metadata.core.database.collections;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.api.VitamRepositoryProvider;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.ID;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.TENANT_ID;

/**
 * Metadata repository : direct access to databases
 */
public class MetadataRepositoryService {

    /**
     * Vitam databases direct access provider.
     */
    private VitamRepositoryProvider vitamRepositoryProvider;

    public MetadataRepositoryService(VitamRepositoryProvider vitamRepositoryProvider) {
        this.vitamRepositoryProvider = vitamRepositoryProvider;
    }

    /**
     * Retrieve document by its ID in a given collection filtered by a tenant in mongo
     *
     * @param collection collection
     * @param id id
     * @param tenant tenant
     * @return the document as JsonNode
     * @throws DatabaseException database access error
     * @throws MetaDataNotFoundException document not found
     * @throws InvalidParseOperationException could not parse response
     */
    public JsonNode getDocumentById(MetadataCollections collection, String id, Integer tenant)
        throws DatabaseException, MetaDataNotFoundException, InvalidParseOperationException {
        Optional<Document> document = vitamRepositoryProvider
            .getVitamMongoRepository(collection.getVitamCollection())
            .getByID(id, tenant);
        if (document.isPresent()) {
            switch (collection) {
                case UNIT:
                    return JsonHandler.toJsonNode(new Unit(document.get()));
                case OBJECTGROUP:
                    return JsonHandler.toJsonNode(new ObjectGroup(document.get()));
                default:
                    throw new IllegalArgumentException("Metadata collection invalid");
            }
        } else {
            throw new MetaDataNotFoundException(
                String.format(
                    "Could not find document of type %s by id %s for tenant %d",
                    collection.getName(),
                    id,
                    tenant
                )
            );
        }
    }

    /**
     * Retrieve document by its ID in a given collection filtered by a tenant in mongo
     *
     * @param collection collection
     * @param ids ids
     * @param tenant tenant
     * @return the document as JsonNode
     * @throws InvalidParseOperationException could not parse response
     */
    public List<JsonNode> getDocumentsByIds(MetadataCollections collection, Collection<String> ids, Integer tenant)
        throws InvalidParseOperationException {
        MongoCursor<Document> documents = vitamRepositoryProvider
            .getVitamMongoRepository(collection.getVitamCollection())
            .findDocuments(and(in(ID, ids), eq(TENANT_ID, tenant)), VitamConfiguration.getBatchSize())
            .iterator();

        List<JsonNode> entries = new ArrayList<>();

        while (documents.hasNext()) {
            Document document = documents.next();
            switch (collection) {
                case UNIT:
                    entries.add(JsonHandler.toJsonNode(new Unit(document)));
                    break;
                case OBJECTGROUP:
                    entries.add(JsonHandler.toJsonNode(new ObjectGroup(document)));
                    break;
                default:
                    throw new IllegalArgumentException("Metadata collection invalid");
            }
        }

        return entries;
    }
}

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
package fr.gouv.vitam.functional.administration.common.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.database.builder.request.single.Delete;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.DocumentAlreadyExistsException;
import fr.gouv.vitam.common.exception.SchemaValidationException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;

/**
 * MongoDb Access Referential interface
 */
public interface MongoDbAccessReferential {
    /**
     * insert documents
     *
     * @param arrayNode of documents
     * @param collection collection of Mongo for insert
     * @return DbRequestResult
     * @throws ReferentialException when error occurs
     */
    DbRequestResult insertDocuments(ArrayNode arrayNode, FunctionalAdminCollections collection)
        throws ReferentialException, SchemaValidationException, DocumentAlreadyExistsException;

    /**
     * insert documents
     *
     * @param arrayNode of documents
     * @param collection collection of Mongo for insert
     * @return DbRequestResult
     * @throws ReferentialException when error occurs
     */
    DbRequestResult insertDocuments(ArrayNode arrayNode, FunctionalAdminCollections collection, Integer version)
        throws DocumentAlreadyExistsException, ReferentialException, SchemaValidationException;

    /**
     * insert documents
     *
     * @param jsonNode of documents
     * @param collection collection of Mongo for insert
     * @return DbRequestResult
     * @throws ReferentialException when error occurs
     */
    DbRequestResult insertDocument(JsonNode jsonNode, FunctionalAdminCollections collection)
        throws ReferentialException, SchemaValidationException, DocumentAlreadyExistsException;

    // Not check, test feature !
    @VisibleForTesting
    DbRequestResult deleteCollectionForTesting(FunctionalAdminCollections collection, Delete delete)
        throws DatabaseException, ReferentialException;

    /**
     * Delete FileFormat collections
     *
     * @param collection collection of Mongo for delete
     * @return DbRequestResult
     * @throws DatabaseException thrown when error on delete
     * @throws ReferentialException when error occurs
     */
    @VisibleForTesting
    DbRequestResult deleteCollectionForTesting(FunctionalAdminCollections collection)
        throws DatabaseException, ReferentialException, SchemaValidationException;

    /**
     * @param id of vitam document
     * @param collection collection of Mongo
     * @return vitam document
     * @throws ReferentialException when error occurs
     */
    VitamDocument<?> getDocumentById(String id, FunctionalAdminCollections collection) throws ReferentialException;

    /**
     * @param id functional id value
     * @param collection Mongo collection
     * @param field unique field in collection as functional id
     * @return
     * @throws ReferentialException
     */
    VitamDocument<?> getDocumentByUniqueId(String id, FunctionalAdminCollections collection, String field)
        throws ReferentialException;

    void replaceDocument(
        JsonNode document,
        String identifier,
        String identifierName,
        FunctionalAdminCollections vitamCollection
    ) throws DatabaseException;

    /**
     * Update with queryDsl
     *
     * @param update JsonNode to update
     * @param collection collection of Mongo Type for update
     * @return DbRequestResult
     * @throws ReferentialException when error occurs;
     * @throws SchemaValidationException
     * @throws BadRequestException
     */
    DbRequestResult updateData(JsonNode update, FunctionalAdminCollections collection)
        throws ReferentialException, SchemaValidationException, BadRequestException;

    /**
     * Update with queryDsl
     *
     * @param update JsonNode to update
     * @param collection collection of Mongo Type for update
     * @param version
     * @return DbRequestResult
     * @throws ReferentialException when error occurs;
     * @throws SchemaValidationException
     * @throws BadRequestException
     */
    DbRequestResult updateData(JsonNode update, FunctionalAdminCollections collection, Integer version)
        throws ReferentialException, SchemaValidationException, BadRequestException;

    /**
     * @param select filter
     * @param collection collection of Mongo for find
     * @return DbRequestResult
     * @throws ReferentialException when error occurs
     * @throws BadRequestException when query is incorrect
     */
    DbRequestResult findDocuments(JsonNode select, FunctionalAdminCollections collection) throws ReferentialException;

    /**
     * @param delete filter
     * @param collection collection of Mongo for delete
     * @return DbRequestResult
     * @throws ReferentialException when error occurs
     * @throws SchemaValidationException
     * @throws BadRequestException
     */
    DbRequestResult deleteDocument(JsonNode delete, FunctionalAdminCollections collection)
        throws ReferentialException, BadRequestException, SchemaValidationException;
}

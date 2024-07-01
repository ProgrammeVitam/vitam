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
package fr.gouv.vitam.functional.administration.common;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.security.IllegalPathException;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.FileFormatNotFoundException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;

import java.io.IOException;
import java.io.InputStream;

/**
 * ReferentialFile<E>
 *
 * @param <E> Type of Referential
 */
public interface ReferentialFile<E> {
    /**
     * importFile : import reference file to database
     *
     * @param file as InputStream
     * @param filename file name
     * @throws ReferentialException when there is error of import
     * @throws DatabaseConflictException when there is a database conflict
     * @throws IOException
     * @throws InvalidParseOperationException
     * @throws InvalidCreateOperationException
     */
    void importFile(InputStream file, String filename)
        throws VitamException, IOException, InvalidCreateOperationException, IllegalPathException;

    /**
     * find document based on a given Id
     *
     * @param id of vitam document
     * @return vitam document
     * @throws ReferentialException when error occurs
     */
    VitamDocument<E> findDocumentById(String id) throws ReferentialException;

    /**
     * find document based on DSL query
     *
     * @param select filter
     * @return vitam document list
     * @throws FileFormatNotFoundException when no results found
     * @throws ReferentialException when error occurs
     */
    RequestResponseOK<E> findDocuments(JsonNode select) throws FileFormatNotFoundException, ReferentialException;
}

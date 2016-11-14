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
package fr.gouv.vitam.common.database.server.mongodb;

import org.bson.Document;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.json.JsonHandler;

/**
 * Vitam Document MongoDb abstract
 *
 * @param <E> Class used to implement the Document
 */
public abstract class VitamDocument<E> extends Document {
    private static final long serialVersionUID = 4051636259488359930L;
    /**
     * ID of each line: different for each sub type
     */
    public static final String ID = "_id";
    /**
     * TenantId
     */
    public static final String TENANT_ID = "_tenant";

    /**
     * Empty constructor
     */
    public VitamDocument() {
        // Empty
    }

    /**
     * Constructor from Json
     *
     * @param content String
     * @throws IllegalArgumentException if Id is not a GUID
     */
    public VitamDocument(String content) {
        super(Document.parse(content));
        checkId();
    }

    /**
     * Constructor from Json
     *
     * @param content as JsonNode
     * @throws IllegalArgumentException if Id is not a GUID
     */
    public VitamDocument(JsonNode content) {
        super(Document.parse(JsonHandler.unprettyPrint(content)));
        checkId();
    }

    /**
     * Constructor from Document
     *
     * @param content Document
     * @throws IllegalArgumentException if Id is not a GUID
     */
    public VitamDocument(Document content) {
        super(content);
        checkId();
    }

    /**
     * check if Id is valid
     *
     * @return this
     * @throws IllegalArgumentException if Id is not a GUID
     */
    public VitamDocument<E> checkId() {
        String id = getId();
        if (id == null) {
            id = getString(ID);
        }
        try {
            final int tenantId = GUIDReader.getGUID(id).getTenantId();
            append(TENANT_ID, tenantId).append(ID, id);
        } catch (final InvalidGuidOperationException e) {
            throw new IllegalArgumentException("ID is not a GUID: " + id, e);
        }
        return this;
    }

    /**
     *
     * @return the ID
     */
    public String getId() {
        return getString(ID);
    }

    /**
     *
     * @return the TenantId
     */
    public final int getTenantId() {
        return getInteger(TENANT_ID);
    }

    /**
     *
     * @return the bypass toString
     */
    public String toStringDirect() {
        return super.toString();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ": " + super.toString();
    }
}

/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
import com.mongodb.client.MongoCollection;
import org.bson.Document;


/**
 * ObjectGroup:<br>
 *
 * @formatter:off { MD technique globale (exemple GPS), _id : UUID, _tenant : tenant, _profil:
 * audio|video|document|text|image|..., _up : [ UUIDUnit1, UUIDUnit2, ... ], _nbc : nb objects, _uses : [
 * { strategy : conservationId, versions : [ { // Object _version : rank, _creadate : date, _id:
 * UUIDObject, digest : { val : val, typ : type }, size: size, fmt: fmt, MD techniques, _copies : [ { sid
 * : id, storageDigest: val }, { sid, ...}, ... ] }, { _version : N, ...}, ... ] }, { strategy :
 * diffusion, ... }, ... ] }
 * @formatter:on
 */
public class ObjectGroup extends MetadataDocument<ObjectGroup> {
    private static final long serialVersionUID = -1761786017392977575L;

    /**
     * Usages
     */
    public static final String USAGES = "_qualifiers.qualifier";
    /**
     * Storage Id
     */
    public static final String STORAGE = "_storage";

    /**
     * Versions
     */
    public static final String VERSIONS = "_qualifiers.versions";
    /**
     * DataObjectVersion
     */
    public static final String DATAOBJECTVERSION = VERSIONS + "." + "DataObjectVersion";

    /**
     * storage to objectGroup
     */
    public static final String VERSIONS_STORAGE = VERSIONS + "." + "storage";
    /**
     * Storage Id
     */
    public static final String OBJECTSTRATEHY = VERSIONS_STORAGE + "." + "strategyId";
    /**
     * Version
     */
    public static final String OBJECTVERSION = VERSIONS + "." + "_version";
    /**
     * Object UUID
     */
    public static final String OBJECTID = VERSIONS + "." + "_id";
    /**
     * Object size
     */
    public static final String OBJECTSIZE = VERSIONS + "." + "Size";
    /**
     * Object format
     */
    public static final String OBJECTFORMAT = VERSIONS + "." + "FormatIdentification.FormatId";
    /**
     * Digest
     */
    public static final String OBJECTDIGEST = VERSIONS;
    /**
     * Digest Value
     */
    public static final String OBJECTDIGEST_VALUE = OBJECTDIGEST + "." + "MessageDigest";
    /**
     * Digest Type
     */
    public static final String OBJECTDIGEST_TYPE = OBJECTDIGEST + "." + "Algorithm";
    /**
     * Copies
     */
    public static final String COPIES = VERSIONS + "." + "_copies";
    /**
     * depths
     */
    public static final String OGDEPTHS = "_ops";

    /**
     * Empty constructor
     */

    public ObjectGroup() {
        // Empty constructor
    }

    /**
     * Constructor from Json
     *
     * @param content the objectgroup of JsonNode format
     */
    public ObjectGroup(JsonNode content) {
        super(content);
    }

    /**
     * Constructor from Document
     *
     * @param content the objectgroup of Document format
     */
    public ObjectGroup(Document content) {
        super(content);
    }

    /**
     * Constructor from Json as Text
     *
     * @param content the objectgroup of String format
     */
    public ObjectGroup(String content) {
        super(content);
    }

    @Override
    protected MetadataCollections getMetadataCollections() {
        return MetadataCollections.OBJECTGROUP;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected MongoCollection<ObjectGroup> getCollection() {
        return MetadataCollections.OBJECTGROUP.getCollection();
    }

    @Override
    public MetadataDocument<ObjectGroup> newInstance(JsonNode content) {
        return new ObjectGroup(content);
    }
}

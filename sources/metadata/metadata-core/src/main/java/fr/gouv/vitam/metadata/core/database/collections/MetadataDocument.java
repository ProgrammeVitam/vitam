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
package fr.gouv.vitam.metadata.core.database.collections;

import static com.mongodb.client.model.Filters.eq;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;

import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;

/**
 * The default Vitam Type object to be stored in the database (MongoDb/ElasticSearch mode)
 *
 * @param <E> Class associated with this Document
 *
 */
public abstract class MetadataDocument<E> extends Document {
    private static final long serialVersionUID = 7912599149562030658L;
    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(MetadataDocument.class);
    /**
     * Default ID field name
     */
    public static final String ID = "_id";
    /**
     * Object Type (text, audio, video, document, image, ...) Unit Type (facture, paye, ...)
     */
    public static final String QUALIFIERS = "_qualifiers";

    /**
     * Object Type (text, audio, video, document, image, ...) Unit Type (facture, paye, ...)
     */
    public static final String TYPE = "_type";
    /**
     * DomainId
     */
    public static final String DOMID = "_dom";
    /**
     * Parents link (Units or ObjectGroup to parent Units)
     */
    public static final String UP = "_up";
    /**
     * Unused field
     */
    public static final String UNUSED = "_unused";
    /**
     * ObjectGroup link (Unit to ObjectGroup)
     */
    public static final String OG = "_og";

    /**
     * Empty constructor
     */
    public MetadataDocument() {}

    /**
     * Constructor from Json
     *
     * @param content
     */
    public MetadataDocument(JsonNode content) {
        super(Document.parse(content.toString()));
        checkId();
    }

    /**
     * Constructor from Document
     *
     * @param content
     */
    public MetadataDocument(Document content) {
        super(content);
        checkId();
    }

    /**
     * Constructor from Json as text
     *
     * @param content
     */
    public MetadataDocument(String content) {
        super(Document.parse(content));
        checkId();
    }

    /**
     *
     * @return the associated GUIDObjectType
     */
    public static int getGUIDObjectTypeId() {
        throw new UnsupportedOperationException("Should override it on implementation class");
    }

    /**
     * Create a new ID
     *
     * @param tenantId
     * @return this
     */
    public MetadataDocument<E> checkId() {
        final String id = getId();
        if (id != null) {
            try {
                final int domainId = GUIDReader.getGUID(id).getTenantId();
                append(DOMID, domainId);
            } catch (final InvalidGuidOperationException e) {
                LOGGER.warn(e);
            }
        }
        return this;
    }

    MetadataDocument<E> testAndCheckId() {
        if (!containsKey(DOMID)) {
            return checkId();
        }
        return this;
    }

    /**
     *
     * @return the ID
     */
    public final String getId() {
        return getString(ID);
    }

    /**
     *
     * @return the domainId
     */
    public final int getDomainId() {
        return this.getInteger(DOMID);
    }

    /**
     * Load from a JSON String
     *
     * @param json
     * @return this
     */
    public final MetadataDocument<E> load(final String json) {
        putAll(Document.parse(json));
        getAfterLoad();
        checkId();
        return this;
    }

    /**
     * To be called after any automatic load or loadFromJson to update HashMap values.
     *
     * @return this
     */
    public abstract MetadataDocument<E> getAfterLoad();

    /**
     * To be called before any collection.insert() or update if HashMap values is changed.
     *
     * @return this
     */
    public abstract MetadataDocument<E> putBeforeSave();

    /**
     *
     * @return the associated collection
     */
    protected abstract MongoCollection<E> getCollection();

    /**
     *
     * @return the associated VitamCollection
     */
    protected abstract MetadataCollections getMetadataCollections();

    /**
     * Save the object. Implementation should call putBeforeSave before the real save operation (insert or update)
     *
     * @return this
     * @throws MetaDataExecutionException
     */
    public abstract MetadataDocument<E> save() throws MetaDataExecutionException;

    /**
     * Update the object to the database
     *
     * @param update
     * @return this
     * @throws MetaDataExecutionException
     */
    public MetadataDocument<E> update(final Bson update)
        throws MetaDataExecutionException {
        try {
            getCollection().updateOne(eq(ID, getId()), update);
        } catch (final MongoException e) {
            LOGGER.debug(e);
            throw new MetaDataExecutionException(e);
        } catch (final IllegalArgumentException e) {
            throw new MetaDataExecutionException(e);
        }
        return this;
    }

    /**
     * try to update the object if necessary (difference from the current value in the database)
     *
     * @return True if the object does not need any extra save operation
     * @throws MetaDataExecutionException
     */
    protected abstract boolean updated() throws MetaDataExecutionException;

    /**
     * load the object from the database, ignoring any previous data, except ID
     *
     * @return True if the object is loaded
     */
    public abstract boolean load();

    /**
     * Insert the document (only for new): should not be called elsewhere
     *
     * @return this
     * @throws MetaDataExecutionException
     */
    @SuppressWarnings("unchecked")
    protected final MetadataDocument<E> insert() throws MetaDataExecutionException {
        testAndCheckId();
        try {
            getCollection().insertOne((E) this);
        } catch (final MongoException e) {
            throw new MetaDataExecutionException(e);
        } catch (final IllegalArgumentException e) {
            throw new MetaDataExecutionException(e);
        }
        return this;
    }

    /**
     * Save the document if new, update it (keeping non set fields, replacing set fields)
     *
     * @return this
     * @throws MetaDataExecutionException
     */
    @SuppressWarnings("unchecked")
    protected final MetadataDocument<E> updateOrSave()
        throws MetaDataExecutionException {
        testAndCheckId();
        final String id = this.getId();
        try {
            if (MongoDbMetadataHelper.exists(getMetadataCollections(), id)) {
                getCollection().replaceOne(eq(ID, id), (E) this);
            } else {
                getCollection().insertOne((E) this);
            }
        } catch (final MongoException e) {
            throw new MetaDataExecutionException(e);
        } catch (final IllegalArgumentException e) {
            throw new MetaDataExecutionException(e);
        }
        return this;
    }

    /**
     * Force the save (insert) of this document (no putBeforeSave done)
     *
     * @return this
     * @throws MetaDataExecutionException
     */
    protected final MetadataDocument<E> forceSave()
        throws MetaDataExecutionException {
        testAndCheckId();
        try {
            getCollection().updateOne(eq(ID, getId()), this, new UpdateOptions().upsert(true));
        } catch (final MongoException e) {
            throw new MetaDataExecutionException(e);
        } catch (final IllegalArgumentException e) {
            throw new MetaDataExecutionException(e);
        }
        return this;
    }

    /**
     * Delete the current object
     *
     * @return this
     * @throws MetaDataExecutionException
     */
    public final MetadataDocument<E> delete() throws MetaDataExecutionException {
        try {
            getCollection().deleteOne(eq(ID, this.getId()));
        } catch (final MongoException e) {
            throw new MetaDataExecutionException(e);
        }
        return this;
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

    /**
     *
     * @return the toString for Debug mode
     */
    public String toStringDebug() {
        return this.getClass().getSimpleName() + ": " + this.get(ID);
    }

}

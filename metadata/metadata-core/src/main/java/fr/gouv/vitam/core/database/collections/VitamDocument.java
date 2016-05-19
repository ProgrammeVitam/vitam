/*******************************************************************************
 * This file is part of Vitam Project.
 * 
 * Copyright Vitam (2012, 2015)
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify
 * and/ or redistribute the software under the terms of the CeCILL license as
 * circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify
 * and redistribute granted by the license, users are provided only with a
 * limited warranty and the software's author, the holder of the economic
 * rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with
 * loading, using, modifying and/or developing or reproducing the software by
 * the user in light of its specific status of free software, that may mean that
 * it is complicated to manipulate, and that also therefore means that it is
 * reserved for developers and experienced professionals having in-depth
 * computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling
 * the security of their systems and/or data to be ensured and, more generally,
 * to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.core.database.collections;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteConcernException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;

import static com.mongodb.client.model.Filters.*;

import fr.gouv.vitam.core.database.collections.MongoDbAccess.VitamCollections;
import fr.gouv.vitam.core.database.configuration.GlobalDatasDb;
import fr.gouv.vitam.common.exception.InvalidUuidOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * The default Vitam Type object to be stored in the database
 * (MongoDb/ElasticSearch mode)
 * 
 * @param <E>
 *            Class associated with this Document
 *
 */
public abstract class VitamDocument<E> extends Document {
    private static final long serialVersionUID = 7912599149562030658L;
    private static final VitamLogger LOGGER =
            VitamLoggerFactory.getInstance(VitamDocument.class);
    /**
     * Default ID field name
     */
    public static final String ID = "_id";
    /**
     * Object Type (text, audio, video, document, image, ...)
     * Unit Type (facture, paye, ...)
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
    public VitamDocument() {
    }

    /**
     * Constructor from Json
     * @param content
     */
    public VitamDocument(JsonNode content) {
    	super(Document.parse(content.toString()));
    	checkId();
    }

    /**
     * Constructor from Document
     * @param content
     */
    public VitamDocument(Document content) {
    	super(content);
    	checkId();
    }

    /**
     * Constructor from Json as text
     * @param content
     */
    public VitamDocument(String content) {
    	super(Document.parse(content));
    	checkId();
    }

    /**
     * Create a new ID
     * 
     * @param domainId
     * @return this
     */
    public VitamDocument<E> checkId() {
		try {
	    	int domainId = GlobalDatasDb.UUID_FACTORY.getUuid(getId()).getDomainId();
	        append(DOMID, domainId);
		} catch (InvalidUuidOperationException e) {
			// TODO REVIEW Auto-generated catch block
			e.printStackTrace();
		}
        return this;
    }

    /**
     *
     * @return the ID
     */
    public final String getId() {
        return this.getString(ID);
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
    public final VitamDocument<E> load(final String json) {
        this.putAll(Document.parse(json));
        getAfterLoad();
    	checkId();
        return this;
    }

    /**
     * To be called after any automatic load or loadFromJson to update HashMap
     * values.
     * @return this
     */
    public abstract VitamDocument<?> getAfterLoad();

    /**
     * To be called before any collection.insert() or update if HashMap values
     * is changed.
     * @return this
     */
    public abstract VitamDocument<?> putBeforeSave();

    /**
     * 
     * @return the associated collection
     */
    protected abstract MongoCollection<E> getCollection();
    /**
     * 
     * @return the associated VitamCollection
     */
    protected abstract VitamCollections getVitamCollections();

    /**
     * Save the object. Implementation should call putBeforeSave before the real
     * save operation (insert or update)
     * @return this
     */
    public abstract VitamDocument<E> save() throws MongoWriteException, MongoWriteConcernException, MongoException;

    /**
     * Update the object to the database
     *
     * @param update
     * @return this
     */
    public VitamDocument<E> update(final Bson update) throws MongoWriteException, MongoWriteConcernException, MongoException {
        try {
        	getCollection().updateOne(eq(ID, getId()), update);
        } catch (final MongoException e) {
            LOGGER.error("Exception for " + update, e);
            throw e;
        }
        return this;
    }

    /**
     * try to update the object if necessary (difference from the current value
     * in the database)
     *
     * @return True if the object does not need any extra save operation
     */
    protected abstract boolean updated();

    /**
     * load the object from the database, ignoring any previous data, except ID
     *
     * @return True if the object is loaded
     */
    public abstract boolean load();

    /**
     * Insert the document (only for new)
     *
     * @return this
     */
    @SuppressWarnings("unchecked")
    protected final VitamDocument<E> insert() throws MongoWriteException, MongoWriteConcernException, MongoException {
        getCollection().insertOne((E) this);
        return this;
    }

    /**
     * Save the document if new, update it (keeping non set fields, replacing
     * set fields)
     *
     * @return this
     */
    @SuppressWarnings("unchecked")
    protected final VitamDocument<E> updateOrSave() throws MongoWriteException, MongoWriteConcernException, MongoException {
        final String id = this.getId();
        if (MongoDbHelper.exists(getVitamCollections(), id)) {
            getCollection().replaceOne(eq(ID, id), (E) this);
        } else {
            getCollection().insertOne((E) this);
        }
        return this;
    }

    /**
     * Force the save (insert) of this document (no putBeforeSave done)
     * @return this
     */
    protected final VitamDocument<E> forceSave() throws MongoWriteException, MongoWriteConcernException, MongoException {
        getCollection().updateOne(eq(ID, getId()), this, new UpdateOptions().upsert(true));
        return this;
    }

    /**
     * Delete the current object
     * @return this
     */
    public final VitamDocument<E> delete() throws MongoWriteException, MongoWriteConcernException, MongoException {
        getCollection().deleteOne(eq(ID, this.getId()));
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

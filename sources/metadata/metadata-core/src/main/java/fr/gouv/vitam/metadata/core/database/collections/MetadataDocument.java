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

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.bson.Document;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * The default Vitam Type object to be stored in the database (MongoDb/ElasticSearch mode)
 *
 * @param <E> Class associated with this Document
 */
public abstract class MetadataDocument<E> extends VitamDocument<E> {
    private static final long serialVersionUID = 7912599149562030658L;
    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(MetadataDocument.class);
    /**
     * Internal version of the document: Incremented for each update (including computed fields)
     */
    public static final String ATOMIC_VERSION = "_av";
    /**
     * Object Type (text, audio, video, document, image, ...) Unit Type (facture, paye, ...)
     */
    public static final String QUALIFIERS = "_qualifiers";
    /**
     * Number of copies or<br>
     * Number of Immediate child (Unit)
     */
    public static final String NBCHILD = "_nbc";

    /**
     * Object Type (text, audio, video, document, image, ...) Unit Type (facture, paye, ...)
     */
    public static final String TYPE = "_profil";
    /**
     * Parents link (Units or ObjectGroup to parent Units)
     */
    public static final String UP = "_up";
    /**
     * ObjectGroup link (Unit to ObjectGroup)
     */
    public static final String OG = "_og";
    /**
     * Array of operations (both Unit and ObjectGroup)
     */
    public static final String OPS = "_ops";
    /**
     * Initial Operation (both Unit and ObjectGroup)
     */
    public static final String OPI = "_opi";
    /**
     * ORIGINATING_AGENCy : Principal originating agency for unit
     */
    public static final String ORIGINATING_AGENCY = "_sp";

    /**
     * ORIGINATING_AGENCIES : list of all originating agencies for unit
     */
    public static final String ORIGINATING_AGENCIES = "_sps";
    /**
     * All unit parents
     */
    public static final String UNITUPS = "_us";
    /**
     * Last persisted date of Unit / Object Group
     */
    public static final String GRAPH_LAST_PERSISTED_DATE = "_glpd";

    /**
     * Quick projection for ID and ObjectGroup Only
     */
    static final String[] ES_PROJECTION = {
        ID, MetadataDocument.NBCHILD, TENANT_ID, SCORE};

    /**
     * Empty constructor
     */
    public MetadataDocument() {
    }

    /**
     * Constructor from Json
     *
     * @param content in JsonNode format for building MetadataDocument
     */
    public MetadataDocument(JsonNode content) {
        super(Document.parse(JsonHandler.unprettyPrint(content)));
    }

    /**
     * Constructor from Document
     *
     * @param content in Document format for building MetadataDocument
     */
    public MetadataDocument(Document content) {
        super(content);
    }

    /**
     * Constructor from Json as text
     *
     * @param content in String format for building MetadataDocument
     */
    public MetadataDocument(String content) {
        super(Document.parse(content));
    }

    /**
     * Create a new ID
     *
     * @return this MetadataDocument
     */
    public MetadataDocument<E> checkId() {
        return this;
    }

    /**
     * @return the ID
     */
    public final String getId() {
        return getString(ID);
    }

    /**
     * @return the associated collection
     */
    protected abstract MongoCollection<E> getCollection();

    /**
     * @return the associated VitamCollection
     */
    protected abstract MetadataCollections getMetadataCollections();

    /**
     * @return the bypass toString
     */
    public String toStringDirect() {
        return super.toString();
    }

    /**
     * @return the toString for Debug mode
     */
    public String toStringDebug() {
        return this.getClass().getSimpleName() + ": " + this.get(ID);
    }

    public <T> Collection<T> getCollectionOrEmpty(String name) {
        Collection<T> collection = this.get(name, Collection.class);
        if (collection == null)
            return Collections.EMPTY_LIST;
        return collection;
    }

    public <V> Map<String, V> getMapOrEmpty(String name) {
        Map<String, V> map = this.get(name, Map.class);
        if (map == null)
            return Collections.EMPTY_MAP;
        return map;
    }

    public String getSp() {
        return get(ORIGINATING_AGENCY, String.class);
    }

    public Collection<String> getUp() {
        return getCollectionOrEmpty(Unit.UP);
    }

    public Integer getAtomicVersion() {
        return getInteger(ATOMIC_VERSION);
    }
}

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.BasicDBList;

import fr.gouv.vitam.common.SingletonUtils;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * Abstract class for Result
 *
 */
public abstract class Result {
    /**
     * Field containing the full documents result as an array of document
     */
    public static final String RESULT_FIELD = "results";

    /**
     * Current Units in the result
     */
    public static final String IDLIST = "idList";

    /**
     * Current Ids in the result
     */
    protected Set<String> currentIds = new HashSet<>();
    /**
     * Number of result (might be different on update/delete than currentUnits)
     */
    protected long nbResult = 0;
    /**
     * The type of the results (Units, ObjectGroups, Objects)
     */
    protected final FILTERARGS type;
    /**
     * The final Result part
     */
    protected Document finalResult;

    /**
     * Constructor for empty result
     *
     * @param type of filter
     */
    public Result(FILTERARGS type) {
        this.type = type;
    }

    /**
     * Constructor from a set, setting the nbResult to the size of Set
     *
     * @param type of filter
     * @param collection the set of working collection
     */
    public Result(FILTERARGS type, Collection<String> collection) {
        this.type = type;
        currentIds.addAll(collection);
        currentIds.remove("");
        nbResult = currentIds.size();
    }

    /**
     * Clear the Result
     *
     * @return this
     */
    public Result clear() {
        currentIds.clear();
        nbResult = 0;
        finalResult = null;
        return this;
    }

    /**
     * Put from argument
     *
     * @param from the Result for creating another 
     * @return Result created 
     */
    public Result putFrom(final Result from) {
        currentIds.addAll(from.currentIds);
        nbResult = from.nbResult;
        return this;
    }

    /**
     * @return True if this result is in error status (list containing error status)
     */
    public boolean isError() {
        return false;
    }

    /**
     * @return the current Ids
     */
    public Set<String> getCurrentIds() {
        return currentIds;
    }

    /**
     * Ad one Id to CurrentIds
     *
     * @param id the id as String adding to current result 
     * @return this
     */
    public Result addId(String id) {
        if (id != null) {
            currentIds.add(id);
        }
        return this;
    }

    /**
     * @param currentIds the current Ids to set
     * @return this
     */
    public Result setCurrentIds(Set<String> currentIds) {
        this.currentIds = currentIds;
        return this;
    }

    /**
     * @return the nbResult
     */
    public final long getNbResult() {
        return nbResult;
    }

    /**
     * @param nbResult the nbResult to set
     * @return this
     */
    public final Result setNbResult(long nbResult) {
        this.nbResult = nbResult;
        return this;
    }

    /**
     * @return the final Result
     */
    public Document getFinal() {
        if (finalResult == null) {
            finalResult = new Document(RESULT_FIELD, null);
        }
        return finalResult;
    }

    /**
     *
     * @return the filtered list for Select operation
     * @throws InvalidParseOperationException if exception occurred when getting the filter list 
     */
    public List<MetadataDocument<?>> getMetadataDocumentListFiltered() throws InvalidParseOperationException {
        final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ElasticsearchAccessMetadata.class);
        LOGGER.debug(toString());
        if (finalResult == null) {
            if (nbResult != 0) {
                throw new InvalidParseOperationException("Invalid number of Result and List of results");
            }
            return SingletonUtils.singletonList();
        }

        final BasicDBList result = (BasicDBList) finalResult.get(RESULT_FIELD);
        if (result == null) {
            if (nbResult != 0) {
                throw new InvalidParseOperationException("Invalid number of Result and List of results");
            }
            return SingletonUtils.singletonList();
        }

        final int size = result.size();
        if (size != nbResult) {
            throw new InvalidParseOperationException("Invalid number of Result and List of results");
        }
        final List<MetadataDocument<?>> list = new ArrayList<>(size);
        for (final Object object : result) {
            final MetadataDocument<?> metadataDocument = (MetadataDocument<?>) object;
            MongoDbMetadataResponseFilter.filterFinalResponse(metadataDocument);
            list.add(metadataDocument);
        }
        return list;
    }

    /**
     * Add one document into final result
     *
     * @param document of type MetaDataDocument adding to result 
     */
    public void addFinal(MetadataDocument<?> document) {
        if (finalResult == null) {
            finalResult = new Document();
        }
        BasicDBList result = (BasicDBList) finalResult.get(RESULT_FIELD);
        if (result == null) {
            result = new BasicDBList();
        }
        result.add(document);
        finalResult.append(RESULT_FIELD, result);
    }

    /**
     * Build the array of result
     *
     * @param projection the project of document
     */
    public void setFinal(Bson projection) {
        final List<Document> list = new ArrayList<>(currentIds.size());
        if (type == FILTERARGS.UNITS) {
            for (final String id : currentIds) {
                final Unit unit =
                    (Unit) MetadataCollections.C_UNIT.getCollection().find(new Document(MetadataDocument.ID, id))
                        .projection(projection).first();
                list.add(unit);
            }
        } else if (type == FILTERARGS.OBJECTGROUPS) {
            for (final String id : currentIds) {
                final ObjectGroup og =
                    (ObjectGroup) MetadataCollections.C_OBJECTGROUP.getCollection()
                        .find(new Document(MetadataDocument.ID, id))
                        .projection(projection).first();
                list.add(og);
            }
        }
        finalResult = new Document(RESULT_FIELD, list);
    }

    @Override
    public String toString() {
        if (finalResult == null) {
            return new StringBuilder(this.getClass().getSimpleName()).append(": {")
                .append(IDLIST).append(':').append(currentIds).append(',')
                .append("nb").append(':').append(nbResult).append(',')
                .append("type").append(':').append(type).append('}').toString();
        } else {
            return new StringBuilder(this.getClass().getSimpleName()).append(": {")
                .append(IDLIST).append(':').append(currentIds).append(',')
                .append("nb").append(':').append(nbResult).append(',')
                .append("type").append(':').append(type).append(',')
                .append(finalResult).append('}').toString();
        }
    }
    
    /**
     * @return boolean check if exist finalResult part   
     */
    public boolean hasFinalResult() {
        if (finalResult == null) {
            return false;
        }
        return true;
    }

}

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

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.FacetResult;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Abstract class for Result
 *
 * @param <T> Parameter Type
 */
public abstract class Result<T> {

    private static final String INVALID_NUMBER_OF_RESULT_AND_LIST_OF_RESULTS =
        "Invalid number of Result and List of results";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ElasticsearchAccessMetadata.class);
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
    protected List<String> currentIds = new ArrayList<>();
    /**
     * Current Ids in the result
     */
    protected List<Float> scores = new ArrayList<>();
    /**
     * Number of result (might be different on update/delete than currentUnits)
     */
    protected long nbResult = 0;
    /**
     * Total aproximated results out of limit
     */
    protected long total = 0;
    /**
     * The type of the results (Units, ObjectGroups, Objects)
     */
    protected final FILTERARGS type;
    /**
     * The final Result part
     */
    protected List<T> finalResult;

    /**
     * The FacetResult list
     */
    protected List<FacetResult> facetResult;

    /**
     * The scrollId
     */
    protected String scrollId;

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
        for (int i = 0; i < collection.size(); i++) {
            scores.add(1f);
        }
        total = nbResult;
    }

    /**
     * Clear the Result
     *
     * @return this
     */
    public Result<T> clear() {
        currentIds.clear();
        scores.clear();
        nbResult = 0;
        total = 0;
        finalResult = null;
        return this;
    }

    /**
     * Put from argument
     *
     * @param from the Result for creating another
     * @return Result created
     */
    public Result<T> putFrom(final Result<T> from) {
        for (int i = 0; i < from.currentIds.size(); i++) {
            if (!currentIds.contains(from.currentIds.get(i))) {
                currentIds.add(from.currentIds.get(i));
                scores.add(from.scores.get(i));
            }
        }
        nbResult = currentIds.size();
        total += from.total;
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
    public List<String> getCurrentIds() {
        return currentIds;
    }

    /**
     * @return the current scores
     */
    public List<Float> getCurrentScores() {
        return scores;
    }

    /**
     * @return the type of Results
     */
    public FILTERARGS getType() {
        return type;
    }

    /**
     * Ad one Id to CurrentIds
     *
     * @param id the id as String adding to current result
     * @param score the associated score
     * @return this
     */
    public Result<T> addId(String id, float score) {
        if (id != null && !currentIds.contains(id)) {
            currentIds.add(id);
            scores.add(score);
            nbResult = currentIds.size();
        }
        return this;
    }

    /**
     * Add ScrollId to Result
     *
     * @param scrollId the scrollid as String adding to current result
     * @return this
     */
    public Result<T> setScrollId(String scrollId) {
        this.scrollId = scrollId;
        return this;
    }

    /**
     * Return ScrollId of Result
     *
     * @return ScrollId
     */
    public String getScrollId() {
        return this.scrollId;
    }

    /**
     * @return the nbResult
     */
    public final long getNbResult() {
        return nbResult;
    }

    /**
     * @param nb the number of updated elements
     * @return this
     */
    public final Result<T> setUpdatedResult(long nb) {
        this.nbResult = nb;
        this.total = nb;
        return this;
    }

    /**
     * @return the approximative total of responses possible, out of limit
     */
    public long getTotal() {
        return total;
    }

    /**
     * @param total the approximative total of responses possible, out of limit
     * @return this
     */
    public Result<T> setTotal(long total) {
        this.total = total;
        return this;
    }

    /**
     * @return the final Result
     */
    public List<T> getFinal() {
        if (finalResult == null) {
            finalResult = new ArrayList<>();
        }
        return finalResult;
    }

    /**
     * @return the list of FacetResult
     */
    public List<FacetResult> getFacet() {
        if (facetResult == null) {
            facetResult = new ArrayList<>();
        }
        return facetResult;
    }

    /**
     * @return the filtered list for Select operation
     * @throws InvalidParseOperationException if exception occurred when getting the filter list
     */
    public List<T> getListFiltered() throws InvalidParseOperationException {
        LOGGER.debug(toString());
        if (finalResult == null || finalResult.isEmpty()) {
            if (nbResult != 0) {
                throw new InvalidParseOperationException(INVALID_NUMBER_OF_RESULT_AND_LIST_OF_RESULTS);
            }
            return Collections.emptyList();
        }

        final int size = finalResult.size();
        if (size != nbResult) {
            throw new InvalidParseOperationException(INVALID_NUMBER_OF_RESULT_AND_LIST_OF_RESULTS);
        }
        if (finalResult.get(0) instanceof MetadataDocument) {
            final List<T> list = new ArrayList<>(size);
            for (T metadataDocument : finalResult) {
                MongoDbMetadataResponseFilter.filterFinalResponse((MetadataDocument<?>) metadataDocument);
                list.add(metadataDocument);
            }
            return list;
        }
        return finalResult;
    }

    /**
     * Add one document into final result
     *
     * @param document of type MetaDataDocument adding to result
     */
    public Result<T> addFinal(T document) {
        if (finalResult == null) {
            finalResult = new ArrayList<>();
        }
        finalResult.add(document);
        nbResult = finalResult.size();
        return this;
    }

    private boolean isScoreIncluded(Bson projection) {
        if (projection == null) {
            return true;
        }
        BsonDocument document = (BsonDocument) projection;
        if (document.isEmpty()) {
            return true;
        }
        BsonValue value = document.get(VitamDocument.SCORE);
        return value == null || ((BsonInt32) value).getValue() > 0;
    }

    /**
     * Build the array of result
     *
     * @param projection the project of document
     */
    @SuppressWarnings("unchecked")
    public void setFinal(Bson projection) {
        final List<T> list = new ArrayList<>(currentIds.size());
        if (type == FILTERARGS.UNITS) {
            for (int i = 0; i < currentIds.size(); i++) {
                String id = currentIds.get(i);
                final Unit unit = MetadataCollections.UNIT.<Unit>getCollection()
                    .find(new Document(MetadataDocument.ID, id))
                    .projection(projection)
                    .first();
                if (unit != null) {
                    if (
                        VitamConfiguration.isExportScore() &&
                        MetadataCollections.UNIT.useScore() &&
                        isScoreIncluded(projection)
                    ) {
                        Float score = 1f;
                        try {
                            score = scores.get(i);
                            if (score.isNaN()) {
                                score = 1f;
                            }
                        } catch (IndexOutOfBoundsException e) {
                            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                        }
                        unit.append(VitamDocument.SCORE, score);
                    }
                    list.add((T) unit);
                }
            }
        } else if (type == FILTERARGS.OBJECTGROUPS) {
            for (int i = 0; i < currentIds.size(); i++) {
                String id = currentIds.get(i);
                final ObjectGroup og = MetadataCollections.OBJECTGROUP.<ObjectGroup>getCollection()
                    .find(new Document(MetadataDocument.ID, id))
                    .projection(projection)
                    .first();
                if (og != null) {
                    if (
                        VitamConfiguration.isExportScore() &&
                        MetadataCollections.OBJECTGROUP.useScore() &&
                        isScoreIncluded(projection)
                    ) {
                        Float score = 1f;
                        try {
                            score = scores.get(i);
                            if (score.isNaN()) {
                                score = 1f;
                            }
                        } catch (IndexOutOfBoundsException e) {
                            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                        }
                        og.append(VitamDocument.SCORE, score);
                    }
                }
                list.add((T) og);
            }
        }
        finalResult = list;
        nbResult = finalResult.size();
    }

    /**
     * Add a FacetResult
     *
     * @param facetResult facetResult
     */
    public void addFacetResult(FacetResult facetResult) {
        getFacet().add(facetResult);
    }

    @Override
    public String toString() {
        if (finalResult == null) {
            return new StringBuilder(this.getClass().getSimpleName())
                .append(": {")
                .append(IDLIST)
                .append(':')
                .append(currentIds)
                .append(',')
                .append("nb")
                .append(':')
                .append(nbResult)
                .append(", total: ")
                .append(total)
                .append(',')
                .append("type")
                .append(':')
                .append(type)
                .append('}')
                .toString();
        } else {
            return new StringBuilder(this.getClass().getSimpleName())
                .append(": {")
                .append(IDLIST)
                .append(':')
                .append(currentIds)
                .append(',')
                .append("nb")
                .append(':')
                .append(nbResult)
                .append(", total: ")
                .append(total)
                .append(',')
                .append("type")
                .append(':')
                .append(type)
                .append(',')
                .append(finalResult)
                .append('}')
                .toString();
        }
    }

    /**
     * @return boolean check if exist finalResult part
     */
    public boolean hasFinalResult() {
        return finalResult != null;
    }
}

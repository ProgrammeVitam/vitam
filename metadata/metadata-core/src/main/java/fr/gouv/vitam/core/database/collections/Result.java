/*******************************************************************************
 * This file is part of Vitam Project.
 *
 * Copyright Vitam (2012, 2015)
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.core.database.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.BasicDBList;

import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.FILTERARGS;
import fr.gouv.vitam.core.database.collections.MongoDbAccess.VitamCollections;

/**
 * Abstract class for Result
 *
 */
public abstract class Result {
    private static final String RESULT = "Result";

    /**
     * Current Units in the result
     */
    public static final String IDLIST = "idList";

    /**
     * Current Ids in the result
     */
    protected Set<String> currentIds = new HashSet<String>();
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
     * @param type
     */
    public Result(FILTERARGS type) {
        this.type = type;
    }

    /**
     * Constructor from a set, setting the nbResult to the size of Set
     * 
     * @param type
     * @param collection
     */
    public Result(FILTERARGS type, Collection<String> collection) {
        this.type = type;
        currentIds.addAll(collection);
        // TODO: I understand why but not the possible reason of such value?
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
     * @param from
     * @return this
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
     * @param id
     * @return this
     */
    public Result addId(String id) {
        currentIds.add(id);
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
            finalResult = new Document(RESULT, null);
        }
        return finalResult;
    }

    /**
     * Add one document into final result
     * 
     * @param document
     */
    public void addFinal(VitamDocument<?> document) {
        if (finalResult == null) {
            finalResult = new Document();
        }
        BasicDBList result = (BasicDBList) finalResult.get(RESULT);
        if (result == null) {
            result = new BasicDBList();
        }
        result.add(document);
        finalResult.append(RESULT, result);
    }

    /**
     * Build the array of result
     * 
     * @param projection
     */
    public void setFinal(Bson projection) {
        final List<Document> list = new ArrayList<Document>(currentIds.size());
        if (type == FILTERARGS.UNITS) {
            for (final String id : currentIds) {
                final Unit unit = (Unit) VitamCollections.Cunit.getCollection().find(new Document(VitamDocument.ID, id))
                    .projection(projection).first();
                list.add(unit);
            }
        } else if (type == FILTERARGS.OBJECTGROUPS) {
            for (final String id : currentIds) {
                final ObjectGroup og =
                    (ObjectGroup) VitamCollections.Cobjectgroup.getCollection().find(new Document(VitamDocument.ID, id))
                        .projection(projection).first();
                list.add(og);
            }
        }
        finalResult = new Document(RESULT, list);
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

}

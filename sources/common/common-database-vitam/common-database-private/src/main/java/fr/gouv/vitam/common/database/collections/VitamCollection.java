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
package fr.gouv.vitam.common.database.collections;

import com.google.common.annotations.VisibleForTesting;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchAccess;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;

import static com.mongodb.client.model.Indexes.hashed;

/**
 * Vitam Collection for mongodb
 */
public class VitamCollection<T> {

    private final Class<T> clasz;
    private final VitamDescriptionResolver vitamDescriptionResolver;
    private String name;
    private MongoCollection<T> collection;
    private ElasticsearchAccess esClient;
    private final boolean isMultiTenant;
    private final boolean useScore;

    protected VitamCollection(
        final Class<T> clasz,
        final boolean isMultiTenant,
        final boolean useScore,
        String prefix,
        VitamDescriptionResolver vitamDescriptionResolver
    ) {
        this.clasz = clasz;
        this.vitamDescriptionResolver = vitamDescriptionResolver;
        name = prefix + clasz.getSimpleName();
        this.isMultiTenant = isMultiTenant;
        this.useScore = useScore;
    }

    /**
     * Initialize the collection
     *
     * @param db mongodb database
     * @param recreate boolean if recreate the database
     */
    public void initialize(final MongoDatabase db, final boolean recreate) {
        collection = db.getCollection(getName(), getClasz());
        if (recreate) {
            collection.createIndex(hashed(VitamDocument.ID));
        }
    }

    /**
     * Initialize the ES Client
     *
     * @param esClient ElasticsearchAccess ES Client
     */
    public void initialize(final ElasticsearchAccess esClient) {
        this.esClient = esClient;
    }

    /**
     * @return the name of the collection
     */
    public String getName() {
        return name;
    }

    @VisibleForTesting
    public void setName(String name) { // NOSONAR
        this.name = name;
    }

    /**
     * @return the associated MongoCollection
     */
    public MongoCollection<T> getCollection() {
        return collection;
    }

    /**
     * @return the associated class
     */
    public Class<T> getClasz() {
        return clasz;
    }

    /**
     * @return the esClient
     */
    public ElasticsearchAccess getEsClient() {
        return esClient;
    }

    /**
     * @return isMultiTenant value
     */
    public boolean isMultiTenant() {
        return isMultiTenant;
    }

    /**
     * @return the useScore
     */
    public boolean isUseScore() {
        return useScore;
    }

    public VitamDescriptionResolver getVitamDescriptionResolver() {
        return vitamDescriptionResolver;
    }
}

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
package fr.gouv.vitam.functional.administration.common.server;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.collections.VitamCollectionHelper;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterSummary;
import fr.gouv.vitam.functional.administration.common.FileFormat;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.IngestContract;

/**
 * All collections in functional admin module
 */
public enum FunctionalAdminCollections {
    /**
     * Formats Collection
     */
    FORMATS(FileFormat.class),

    /**
     * Rules Collection
     */
    RULES(FileRules.class),

    /**
     * Accession Register summary Collection
     */
    ACCESSION_REGISTER_SUMMARY(AccessionRegisterSummary.class),

    /**
     * Accession Register detail Collection
     */
    /**
     * 
     */
    ACCESSION_REGISTER_DETAIL(AccessionRegisterDetail.class),
    
    /**
     * Ingest contract collection
     * 
     */
    INGEST_CONTRACT(IngestContract.class);

    private VitamCollection vitamCollection;
    
    final public static String ID = "_id";

    private FunctionalAdminCollections(final Class<?> clasz) {
        vitamCollection = VitamCollectionHelper.getCollection(clasz);
    }

    /**
     * Initialize the collection
     *
     * @param db database type
     * @param recreate true is as recreate type
     */
    protected void initialize(final MongoDatabase db, final boolean recreate) {
        vitamCollection.initialize(db, recreate);
    }
    
    /**
     * Initialize the collection
     *
     * @param db database type
     * @param recreate true is as recreate type
     */
    protected void initialize(final ElasticsearchAccessFunctionalAdmin esClient) {
        vitamCollection.initialize(esClient);
        esClient.addIndex(this);
    }

    /**
     *
     * @return the name of the collection
     */
    protected String getName() {
        return vitamCollection.getName();
    }

    /**
     *
     * @return the associated MongoCollection
     */
    @SuppressWarnings("rawtypes")
    public MongoCollection getCollection() {
        return vitamCollection.getCollection();
    }


    /**
     *
     * @return the associated class
     */
    protected Class<?> getClasz() {
        return vitamCollection.getClasz();
    }

    /**
     *
     *
     * @return the count of associated MongoCollection
     */

    public long getCount() {
        return vitamCollection.getCollection().count();
    }
    
    
    /**
     * get ElasticSearch Client
     * @return client Es
     */
    public ElasticsearchAccessFunctionalAdmin getEsClient() {
        return (ElasticsearchAccessFunctionalAdmin) vitamCollection.getEsClient();
    }
}

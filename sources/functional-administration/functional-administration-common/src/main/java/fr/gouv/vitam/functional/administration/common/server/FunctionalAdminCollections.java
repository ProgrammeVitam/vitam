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
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.functional.administration.common.AccessContract;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterSummary;
import fr.gouv.vitam.functional.administration.common.Agencies;
import fr.gouv.vitam.functional.administration.common.Context;
import fr.gouv.vitam.functional.administration.common.FileFormat;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.IngestContract;
import fr.gouv.vitam.functional.administration.common.Profile;
import fr.gouv.vitam.functional.administration.common.SecurityProfile;
import fr.gouv.vitam.functional.administration.common.VitamSequence;

/**
 * All collections in functional admin module
 */
public enum FunctionalAdminCollections {
    /**
     * Formats Collection
     */
    FORMATS(FileFormat.class, false, false),

    /**
     * Rules Collection
     */
    RULES(FileRules.class, true, true),

    /**
     * Accession Register summary Collection
     */
    ACCESSION_REGISTER_SUMMARY(AccessionRegisterSummary.class, true, false),

    /**
     * Accession Register detail Collection
     */
    ACCESSION_REGISTER_DETAIL(AccessionRegisterDetail.class, true, false),

    /**
     * Ingest contract collection
     *
     */
    INGEST_CONTRACT(IngestContract.class, true, true),

    /**
     * Access contract collection
     *
     */
    ACCESS_CONTRACT(AccessContract.class, true, true),

    /**
     * Access contract collection
     *
     */
    VITAM_SEQUENCE(VitamSequence.class, false, false),

    /**
     * Profile collection
     */
    PROFILE(Profile.class, true, false),
    /**
     * Agency collection
     */
    AGENCIES(Agencies.class, true, false),

    /**
     * Context collection
     */
    CONTEXT(Context.class, false, false),

    /**
     * Security profile collection
     */
    SECURITY_PROFILE(SecurityProfile.class, false, false);

    final private VitamCollection vitamCollection;
    final private boolean multitenant;
    final private boolean usingScore;

    private FunctionalAdminCollections(final Class<?> clasz, boolean multitenant, boolean usingScore) {
        this.multitenant = multitenant;
        this.usingScore = usingScore;
        vitamCollection = VitamCollectionHelper.getCollection(clasz, multitenant, usingScore);
    }

    /**
     *
     * @return True if this collection is multitenant
     */
    public boolean isMultitenant() {
        return multitenant;
    }

    /**
     * @return the usingScore
     */
    public boolean isUsingScore() {
        return usingScore;
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
     * @param esClient
     */
    protected void initialize(final ElasticsearchAccessFunctionalAdmin esClient) {
        vitamCollection.initialize(esClient);
        esClient.addIndex(this);
    }

    /**
     *
     * @return the name of the collection
     */
    public String getName() {
        return vitamCollection.getName();
    }

    /**
     *
     * @return the type
     */
    public String getType() {
        return "typeunique";
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
     * @return the associated VitamCollection
     */
    public VitamCollection getVitamCollection() {
        return vitamCollection;
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
     *
     * @return the associated VarNameAdapter
     */
    public VarNameAdapter getVarNameAdapater() {
        return new SingleVarNameAdapter();
    }

    /**
     * get ElasticSearch Client
     *
     * @return client Es
     */
    public ElasticsearchAccessFunctionalAdmin getEsClient() {
        return (ElasticsearchAccessFunctionalAdmin) vitamCollection.getEsClient();
}

    /**
     *
     * @param collection
     * @return the corresponding FunctionalAdminCollections
     */
    public static FunctionalAdminCollections getFromVitamCollection(VitamCollection collection) {
        for (FunctionalAdminCollections coll : FunctionalAdminCollections.values()) {
            if (coll.getName().equals(collection.getName())) {
                return coll;
            }
        }
        return null;
    }

    public static FunctionalAdminCollections getFromValue(String collection) {
        for (FunctionalAdminCollections coll : FunctionalAdminCollections.values()) {
            if (coll.name().equalsIgnoreCase(collection)) {
                return coll;
            }
        }
        return null;
    }

}

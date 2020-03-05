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
package fr.gouv.vitam.ihmrecette.appserver.populate;

import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchAccess;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;

/**
 * The Vitam Data Type enum
 */
public enum VitamDataType {
    /**
     * Unit Data Type
     */
    UNIT("Unit", "unit_%d", MetadataCollections.UNIT.getEsClient()),
    /**
     * LogbookLifeCycleUnit Data Type
     */
    LFC_UNIT("LogbookLifeCycleUnit", "", null),
    /**
     * LogbookLifeCycleObjectGroup Data Type
     */
    LFC_GOT("LogbookLifeCycleObjectGroup", "", null),
    /**
     * ObjectGroup Data Type
     */
    GOT("ObjectGroup", "objectgroup_%d", MetadataCollections.OBJECTGROUP.getEsClient()),
    /**
     * FileRules Data Type
     */
    RULES("FileRules", "filerules", FunctionalAdminCollections.RULES.getEsClient()),
    /**
     * AccessContract Data Type
     */
    ACCESS_CONTRACT("AccessContract", "accesscontract", FunctionalAdminCollections.ACCESS_CONTRACT.getEsClient()),
    /**
     * Agencies Data Type
     */
    AGENCIES("Agencies", "agencies", FunctionalAdminCollections.AGENCIES.getEsClient()),
    /**
     * AccessionRegisterSummary Data Type
     */
    ACCESSION_REGISTER_SUMMARY("AccessionRegisterSummary", "accessionregistersummary",
        FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getEsClient());

    private String collectionName;
    private String indexName;
    private final ElasticsearchAccess elasticsearchAccess;

    VitamDataType(String collectionName, String indexName,
        ElasticsearchAccess elasticsearchAccess) {
        this.collectionName = collectionName;
        this.indexName = indexName;
        this.elasticsearchAccess = elasticsearchAccess;
    }

    /**
     * @return the name of the collection
     */
    public String getCollectionName() {
        return collectionName;
    }

    /**
     * @return the index name
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * @param tenantId
     * @return the index name
     */
    String getIndexName(int tenantId) {
        return String.format(indexName, tenantId);
    }

    void setIndexName(String indexName) { // NOSONAR
        this.indexName = indexName;
    }

    void setCollectionName(String collectionName) { // NOSONAR
        this.collectionName = collectionName;
    }

    public ElasticsearchAccess getElasticsearchAccess() {
        return elasticsearchAccess;
    }
}

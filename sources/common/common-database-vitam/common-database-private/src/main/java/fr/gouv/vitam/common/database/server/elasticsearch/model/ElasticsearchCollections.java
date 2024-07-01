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
package fr.gouv.vitam.common.database.server.elasticsearch.model;

import java.io.InputStream;

/**
 * Elasticsearch Collections
 */
public enum ElasticsearchCollections {
    /**
     * Unit Collection
     */
    UNIT("/unit-es-mapping.json", "unit"),
    /**
     * ObjectGroup Collection
     */
    OBJECTGROUP("/og-es-mapping.json", "objectgroup"),
    /**
     * Formats Collection
     */
    FORMATS("/format-es-mapping.json", "fileformat"),

    /**
     * Rules Collection
     */
    RULES("/rule-es-mapping.json", "filerules"),

    /**
     * Accession Register summary Collection
     */
    ACCESSION_REGISTER_SUMMARY("/accessionregistersummary-es-mapping.json", "accessionregistersummary"),

    /**
     * Accession Register detail Collection
     */
    ACCESSION_REGISTER_SYMBOLIC("/accessionregistersymbolic-es-mapping.json", "accessionregistersymbolic"),

    /**
     * Accession Register detail Collection
     */
    ACCESSION_REGISTER_DETAIL("/accessionregisterdetail-es-mapping.json", "accessionregisterdetail"),

    /**
     * Ingest contract collection
     */
    INGEST_CONTRACT("/ingestcontract-es-mapping.json", "ingestcontract"),

    /**
     * Access contract collection
     */
    ACCESS_CONTRACT("/accesscontract-es-mapping.json", "accesscontract"),

    /**
     * Management contract collection
     */
    MANAGEMENT_CONTRACT("/managementcontract-es-mapping.json", "managementcontract"),
    /**
     * Profile collection
     */
    PROFILE("/profile-es-mapping.json", "profile"),

    /**
     * Agency collection
     */
    AGENCIES("/agencies-es-mapping.json", "agencies"),

    /**
     * Context collection
     */
    CONTEXT("/context-es-mapping.json", "context"),

    /**
     * Security profile collection
     */
    SECURITY_PROFILE("/securityprofile-es-mapping.json", "securityprofile"),
    /**
     * Operation Collection
     */
    OPERATION("/logbook-es-mapping.json", "logbookoperation"),
    /**
     * Archive unit profile collection
     */
    ARCHIVE_UNIT_PROFILE("/archiveunitprofile-es-mapping.json", "archiveunitprofile"),

    /**
     * Ontology collection
     */
    ONTOLOGY("/ontology-es-mapping.json", "ontology"),

    /**
     * preservationscenario collection
     */
    PRESERVATION_SCENARIO("/preservationscenario-es-mapping.json", "preservationscenario"),

    GRIFFIN("/griffin-es-mapping.json", "griffin");

    ElasticsearchCollections(String mapping, String indexName) {
        this.mapping = mapping;
        this.indexName = indexName;
    }

    private String mapping;
    private String indexName;

    public String getIndexName() {
        return indexName;
    }

    public String getMapping() {
        return mapping;
    }

    /**
     * Retrieve Mapping as an inputStream
     *
     * @return Mapping as an InputStream
     */
    public InputStream getMappingAsInputStream() {
        return ElasticsearchCollections.class.getResourceAsStream(getMapping());
    }
}

/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.access.external.api;

/**
 * All collections in functional admin module
 */
public enum AdminCollections {
    /**
     * Formats Collection
     */
    FORMATS(AccessExtAPI.FORMATS, AccessExtAPI.FORMATSCHECK),

    /**
     * Rules Collection
     */
    RULES(AccessExtAPI.RULES, AccessExtAPI.RULESCHECK),

    /**
     * Ingest contracts collection
     */
    INGEST_CONTRACTS(AccessExtAPI.INGEST_CONTRACT, null),

    /**
     * Access contracts collection
     */
    ACCESS_CONTRACTS(AccessExtAPI.ACCESS_CONTRACT, null),

    /**
     * Managemnent contracts collection
     */
    MANAGEMENT_CONTRACTS(AccessExtAPI.MANAGEMENT_CONTRACT, null),

    /**
     * Agencies collection
     */
    AGENCIES(AccessExtAPI.AGENCIES, AccessExtAPI.AGENCIESCHECK),

    /**
     * Profile collection
     */
    PROFILE(AccessExtAPI.PROFILES, null),

    /**
     * Context collection
     */
    CONTEXTS(AccessExtAPI.CONTEXTS, null),

    /**
     * Accession register summarry collection
     */
    ACCESSION_REGISTERS(AccessExtAPI.ACCESSION_REGISTERS, null),

    /**
     * Accession register symbolic collection
     */
    ACCESSION_REGISTERS_SYMBOLIC(AccessExtAPI.ACCESSION_REGISTERS_SYMBOLIC, null),

    /**
     * Accession register detail collection
     */
    ACCESSION_REGISTER_DETAILS(AccessExtAPI.ACCESSION_REGISTERS_DETAIL, null),

    /**
     * Archive unit profile collection
     */
    ARCHIVE_UNIT_PROFILE(AccessExtAPI.ARCHIVE_UNIT_PROFILE, null),

    /**
     * Traceability collection
     */
    TRACEABILITY(AccessExtAPI.TRACEABILITY, AccessExtAPI.TRACEABILITYCHECKS),

    /**
     * Security profile collection
     */
    SECURITY_PROFILES(AccessExtAPI.SECURITY_PROFILES, null),

    /**
     * Ontology collection
     */
    ONTOLOGY(AccessExtAPI.ONTOLOGY, null),

    GRIFFIN(AccessExtAPI.GRIFFIN, null),

    PRESERVATION_SCENARIO(AccessExtAPI.PRESERVATION_SCENARIO, null);

    private String name;

    private String checkURI;

    AdminCollections(final String collection, final String checkURI) {
        name = collection;
        this.checkURI = checkURI;
    }

    /**
     * Get the name of the collection
     *
     * @return the name of the collection
     */
    public String getName() {
        return name;
    }

    /**
     * Get the checkUri
     *
     * @return the checkUri
     */
    public String getCheckURI() {
        return checkURI;
    }
}

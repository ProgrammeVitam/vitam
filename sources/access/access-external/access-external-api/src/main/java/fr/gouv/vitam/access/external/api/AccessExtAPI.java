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
 * AccessExtAPI containing different AccessExt uri
 */
public class AccessExtAPI {

    private AccessExtAPI() {}

    /**
     * Accession register base uri
     */
    public static final String ACCESSION_REGISTERS = "accessionregisters";
    /**
     * Accession register base uri
     */
    public static final String ACCESSION_REGISTERS_SYMBOLIC = "accessionregisterssymbolic";
    /**
     * Accession register uri
     */
    public static final String ACCESSION_REGISTERS_API = "/" + ACCESSION_REGISTERS;
    /**
     * Accession register update uri
     */
    public static final String ACCESSION_REGISTERS_API_UPDATE = "/accessionregisters";
    /**
     * Accession register details uri
     */
    public static final String ACCESSION_REGISTERS_DETAIL = "accessionregisterdetails";
    /**
     * Ingest contracts base uri
     */
    public static final String INGEST_CONTRACT = "ingestcontracts";
    /**
     * Agencies base uri
     */
    public static final String AGENCIES = "agencies";
    /**
     * Agencies check uri
     */
    public static final String AGENCIESCHECK = "agenciesfilecheck";

    /**
     * Ingest contracts uri
     */
    public static final String INGEST_CONTRACT_API = "/" + INGEST_CONTRACT;
    /**
     * Ingest contracts update uri
     */
    public static final String INGEST_CONTRACT_API_UPDATE = "/ingestcontracts";
    /**
     * Access contracts base uri
     */
    public static final String ACCESS_CONTRACT = "accesscontracts";
    /**
     * Access contracts uri
     */
    public static final String ACCESS_CONTRACT_API = "/" + ACCESS_CONTRACT;
    /**
     * Access contracts update uri
     */
    public static final String ACCESS_CONTRACT_API_UPDATE = "/accesscontracts";
    /**
     * Management contracts base uri
     */
    public static final String MANAGEMENT_CONTRACT = "managementcontracts";
    /**
     * Management contracts uri
     */
    public static final String MANAGEMENT_CONTRACT_API = "/" + MANAGEMENT_CONTRACT;
    /**
     * Management contracts update uri
     */
    public static final String MANAGEMENT_CONTRACT_API_UPDATE = "/managementcontracts";
    /**
     * Profiles base uri
     */
    public static final String PROFILES = "profiles";
    /**
     * Profiles uri
     */
    public static final String PROFILES_API = "/" + PROFILES;
    /**
     * Profiles update uri
     */
    public static final String PROFILES_API_UPDATE = "profiles";

    /**
     * Contexts base uri
     */
    public static final String CONTEXTS = "contexts";
    /**
     * Contexts uri
     */
    public static final String CONTEXTS_API = "/" + CONTEXTS;
    /**
     * Contexts update
     */
    public static final String CONTEXTS_API_UPDATE = "contexts";

    /**
     * Formats base uri
     */
    public static final String FORMATS = "formats";
    /**
     * Formats check uri
     */
    public static final String FORMATSCHECK = "formatsfilecheck";

    /**
     * Rules uri
     */
    public static final String RULES = "rules";
    /**
     * Rules check uri
     */
    public static final String RULESCHECK = "rulesfilecheck";

    /**
     * Traceability base uri
     */
    public static final String TRACEABILITY = "traceability";
    /**
     * Traceability uri
     */
    public static final String TRACEABILITY_API = "/" + TRACEABILITY;
    /**
     * Traceability check uri
     */
    public static final String TRACEABILITYCHECKS = TRACEABILITY + "checks";

    /**
     * Audits base uri
     */
    public static final String AUDITS = "audits";
    /**
     * Audits uri
     */
    public static final String AUDITS_API = "/" + AUDITS;

    /**
     * Referenial audit uri
     */
    public static final String REFERENTIAL_AUDIT_API = "/" + AUDITS + "/referential";

    /**
     * Operations base uri
     */
    public static final String OPERATIONS = "operations";
    /**
     * Operations uri
     */
    public static final String OPERATIONS_API = "/" + OPERATIONS;
    /**
     * Workflows base uri
     */
    public static final String WORKFLOWS = "workflows";
    /**
     * Workflows uri
     */
    public static final String WORKFLOWS_API = "/" + WORKFLOWS;
    /**
     * DIP base uri
     */
    public static final String DIP = "dipexport";
    /**
     * DIP uri
     */
    public static final String DIP_API = "/" + DIP;

    /**
     * TRANSFER base uri
     */
    public static final String TRANSFER = "transfers";
    /**
     * TRANSFER uri
     */
    public static final String TRANSFER_API = "/" + TRANSFER;

    /**
     * Security profiles uri
     */
    public static final String SECURITY_PROFILES = "securityprofiles";
    /**
     * Rules report base uri
     */
    public static final String RULES_REPORT = "rulesreport";
    /**
     * Rules report uri
     */
    public static final String RULES_REPORT_API = "/" + RULES_REPORT;
    /**
     * Rules report uri
     */
    public static final String DISTRIBUTION_REPORT_API = "/distributionreport";
    /**
     * Preservation report
     */
    public static final String BATCH_REPORT_API = "/batchreport/";
    /**
     * referential base uri
     */
    public static final String RULES_REFERENTIAL = "rulesreferential";
    /**
     * referential csv download uri
     */
    public static final String RULES_REFERENTIAL_CSV_DOWNLOAD = "/" + RULES_REFERENTIAL;
    /**
     * agencies referential base uri
     */
    public static final String AGENCIES_REFERENTIAL = "agenciesreferential";
    /**
     * agencies referential csv download uri
     */
    public static final String AGENCIES_REFERENTIAL_CSV_DOWNLOAD = "/" + AGENCIES_REFERENTIAL;
    /**
     * Unit evidence audit uri
     */
    public static final String UNIT_EVIDENCE_AUDIT_API = "/evidenceaudit";
    /**
     * Unit evidence audit uri
     */
    public static final String RECTIFICATION_AUDIT = "/rectificationaudit";
    /**
     * Archive unit profile base uri
     */
    public static final String ARCHIVE_UNIT_PROFILE = "archiveunitprofiles";

    /**
     * Ontology base uri
     */
    public static final String ONTOLOGY = "ontologies";

    /**
     * Export probative value uri
     */
    public static final String EXPORT_PROBATIVE_VALUE = "/probativevalueexport";

    public static final String GRIFFIN = "griffin";

    public static final String PRESERVATION_SCENARIO = "preservationScenario";

    /**
     * Objects
     */
    public static final String OBJECTS = "/objects";

    /**
     * Access request
     */
    public static final String ACCESS_REQUESTS = "/accessRequests";

    /**
     * Logbook external Operations
     */
    public static final String LOGBOOK_OPERATIONS = "/logbookoperations";

    /**
     * computedinheritedrules external Operations
     */
    public static final String COMPUTEDINHERITEDRULES = "computedInheritedRules";

    public static final int UNAVAILABLE_DATA_FROM_ASYNC_OFFER_STATUS_CODE = 460;
}

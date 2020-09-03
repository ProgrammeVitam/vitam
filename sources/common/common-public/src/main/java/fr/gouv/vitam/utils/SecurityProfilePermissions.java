/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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

package fr.gouv.vitam.utils;

import java.util.Arrays;

public enum SecurityProfilePermissions {

    DIPEXPORT_CREATE("dipexport:create"),
    DIPEXPORTV2_CREATE("dipexportv2:create"),
    DIPEXPORT_ID_DIP_READ("dipexport:id:dip:read"),
    TRANSFERS_CREATE("transfers:create"),
    TRANSFERS_ID_SIP_READ("transfers:id:sip:read"),
    LOGBOOKOBJECTSLIFECYCLES_ID_READ("logbookobjectslifecycles:id:read"),
    LOGBOOKOPERATIONS_READ("logbookoperations:read"),
    LOGBOOKOPERATIONS_ID_READ("logbookoperations:id:read"),
    LOGBOOKUNITLIFECYCLES_ID_READ("logbookunitlifecycles:id:read"),
    UNITS_UPDATE("units:update"),
    UNITS_RULES_UPDATE("units:rules:update"),
    UNITS_READ("units:read"),
    UNITS_ID_READ_JSON("units:id:read:json"),
    UNITS_ID_UPDATE("units:id:update"),
    UNITS_ID_OBJECTS_READ_JSON("units:id:objects:read:json"),
    UNITS_ID_OBJECTS_READ_BINARY("units:id:objects:read:binary"),
    UNITSWITHINHERITEDRULES_READ("unitsWithInheritedRules:read"),
    ACCESSCONTRACTS_READ("accesscontracts:read"),
    ACCESSCONTRACTS_CREATE_JSON("accesscontracts:create:json"),
    ACCESSCONTRACTS_ID_READ("accesscontracts:id:read"),
    ACCESSCONTRACTS_ID_UPDATE("accesscontracts:id:update"),
    ACCESSIONREGISTERS_READ("accessionregisters:read"),
    ACCESSIONREGISTERS_ID_ACCESSIONREGISTERDETAILS_READ("accessionregisters:id:accessionregisterdetails:read"),
    AGENCIES_CREATE("agencies:create"),
    AGENCIES_READ("agencies:read"),
    AGENCIES_ID_READ("agencies:id:read"),
    AGENCIESFILE_CHECK("agenciesfile:check"),
    AGENCIESREFERENTIAL_ID_READ("agenciesreferential:id:read"),
    AUDITS_CREATE("audits:create"),
    CONTEXTS_CREATE_JSON("contexts:create:json"),
    CONTEXTS_READ("contexts:read"),
    CONTEXTS_ID_READ("contexts:id:read"),
    CONTEXTS_ID_UPDATE("contexts:id:update"),
    DISTRIBUTIONREPORT_ID_READ("distributionreport:id:read"),
    FORMATS_READ("formats:read"),
    FORMATS_CREATE("formats:create"),
    FORMATS_ID_READ("formats:id:read"),
    FORMATSFILE_CHECK("formatsfile:check"),
    INGESTCONTRACTS_CREATE_JSON("ingestcontracts:create:json"),
    INGESTCONTRACTS_READ("ingestcontracts:read"),
    INGESTCONTRACTS_ID_READ("ingestcontracts:id:read"),
    INGESTCONTRACTS_ID_UPDATE("ingestcontracts:id:update"),
    OPERATIONS_READ("operations:read"),
    OPERATIONS_ID_READ_STATUS("operations:id:read:status"),
    OPERATIONS_ID_READ("operations:id:read"),
    OPERATIONS_ID_UPDATE("operations:id:update"),
    OPERATIONS_ID_DELETE("operations:id:delete"),
    PROFILES_CREATE_BINARY("profiles:create:binary"),
    PROFILES_CREATE_JSON("profiles:create:json"),
    PROFILES_READ("profiles:read"),
    PROFILES_ID_READ_JSON("profiles:id:read:json"),
    PROFILES_ID_UPDATE_BINAIRE("profiles:id:update:binaire"),
    PROFILES_ID_READ_BINARY("profiles:id:read:binary"),
    PROFILES_ID_UPDATE_JSON("profiles:id:update:json"),
    RULES_READ("rules:read"),
    RULES_CREATE("rules:create"),
    RULES_ID_READ("rules:id:read"),
    RULESFILE_CHECK("rulesfile:check"),
    RULESREPORT_ID_READ("rulesreport:id:read"),
    RULESREFERENTIAL_ID_READ("rulesreferential:id:read"),
    SECURITYPROFILES_CREATE_JSON("securityprofiles:create:json"),
    SECURITYPROFILES_READ("securityprofiles:read"),
    SECURITYPROFILES_ID_READ("securityprofiles:id:read"),
    SECURITYPROFILES_ID_UPDATE("securityprofiles:id:update"),
    TRACEABILITY_ID_READ("traceability:id:read"),
    TRACEABILITYCHECKS_CREATE("traceabilitychecks:create"),
    TRACEABILITYLINKEDCHECKS_CREATE("traceabilitylinkedchecks:create"),
    WORKFLOWS_READ("workflows:read"),
    INGESTS_CREATE("ingests:create"),
    INGESTS_LOCAL_CREATE("ingests:local:create"),
    INGESTS_ID_ARCHIVETRANSFERTREPLY_READ("ingests:id:archivetransfertreply:read"),
    INGESTS_ID_MANIFESTS_READ("ingests:id:manifests:read"),
    SWITCHINDEX_CREATE("switchindex:create"),
    REINDEX_CREATE("reindex:create"),
    EVIDENCEAUDIT_CHECK("evidenceaudit:check"),
    REFERENTIALAUDIT_CHECK("referentialaudit:check"),
    ARCHIVEUNITPROFILES_CREATE_BINARY("archiveunitprofiles:create:binary"),
    ARCHIVEUNITPROFILES_CREATE_JSON("archiveunitprofiles:create:json"),
    ARCHIVEUNITPROFILES_READ("archiveunitprofiles:read"),
    ARCHIVEUNITPROFILES_ID_READ_JSON("archiveunitprofiles:id:read:json"),
    ARCHIVEUNITPROFILES_ID_UPDATE_JSON("archiveunitprofiles:id:update:json"),
    ONTOLOGIES_CREATE_BINARY("ontologies:create:binary"),
    ONTOLOGIES_CREATE_JSON("ontologies:create:json"),
    ONTOLOGIES_READ("ontologies:read"),
    ONTOLOGIES_ID_READ_JSON("ontologies:id:read:json"),
    ONTOLOGIES_ID_READ_BINARY("ontologies:id:read:binary"),
    ONTOLOGIES_ID_UPDATE_JSON("ontologies:id:update:json"),
    RECLASSIFICATION_UPDATE("reclassification:update"),
    RECTIFICATIONAUDIT_CHECK("rectificationaudit:check"),
    OBJECTS_READ("objects:read"),
    ELIMINATION_ANALYSIS("elimination:analysis"),
    ELIMINATION_ACTION("elimination:action"),
    FORCEPAUSE_CHECK("forcepause:check"),
    REMOVEFORCEPAUSE_CHECK("removeforcepause:check"),
    PROBATIVEVALUE_CHECK("probativevalue:check"),
    PROBATIVEVALUE_CREATE("probativevalue:create"),
    GRIFFINS_CREATE("griffins:create"),
    PRESERVATIONSCENARIOS_CREATE("preservationScenarios:create"),
    GRIFFINS_READ("griffins:read"),
    GRIFFIN_READ("griffin:read"),
    PRESERVATIONSCENARIOS_READ("preservationScenarios:read"),
    PRESERVATIONSCENARIO_READ("preservationScenario:read"),
    PRESERVATION_UPDATE("preservation:update"),
    ACCESSIONREGISTERSSYMBOLIC_READ("accessionregisterssymbolic:read"),
    BATCHREPORT_ID_READ("batchreport:id:read"),
    PRESERVATIONREPORT_ID_READ("preservationreport:id:read"),
    LOGBOOKOPERATIONS_CREATE("logbookoperations:create"),
    COMPUTEINHERITEDRULES_ACTION("computeInheritedRules:action"),
    COMPUTEINHERITEDRULES_DELETE("computeInheritedRules:delete"),
    MANAGEMENTCONTRACTS_CREATE_JSON("managementcontracts:create:json"),
    MANAGEMENTCONTRACTS_READ("managementcontracts:read"),
    MANAGEMENTCONTRACTS_ID_READ("managementcontracts:id:read"),
    MANAGEMENTCONTRACTS_ID_UPDATE("managementcontracts:id:update"),
    TRANSFERS_REPLY("transfers:reply"),
    STORAGEACCESSLOG_READ_BINARY("storageaccesslog:read:binary");


    private final String permission;

    SecurityProfilePermissions(String permission) {
        this.permission = permission;
    }

    public String getPermission() {
        return permission;
    }

    public static Boolean isPermissionValid(String permissionToCheck){
        return Arrays.stream(SecurityProfilePermissions.values())
                .anyMatch(elmt -> elmt.getPermission().equalsIgnoreCase(permissionToCheck));
    }
}

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
package fr.gouv.vitam.common.external.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterSummaryModel;
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.common.model.administration.AgenciesModel;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileModel;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileStatus;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.ContextStatus;
import fr.gouv.vitam.common.model.administration.FileFormatModel;
import fr.gouv.vitam.common.model.administration.FileRulesModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ManagementContractModel;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.OntologyOrigin;
import fr.gouv.vitam.common.model.administration.OntologyType;
import fr.gouv.vitam.common.model.administration.PermissionModel;
import fr.gouv.vitam.common.model.administration.ProfileFormat;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.model.administration.ProfileStatus;
import fr.gouv.vitam.common.model.administration.RegisterValueDetailModel;
import fr.gouv.vitam.common.model.administration.RuleMeasurementEnum;
import fr.gouv.vitam.common.model.administration.RuleType;
import fr.gouv.vitam.common.model.administration.StorageDetailModel;
import fr.gouv.vitam.common.model.logbook.LogbookEventOperation;
import fr.gouv.vitam.common.model.logbook.LogbookLifecycle;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Results for client mock
 */
public class ClientMockResultHelper {

    private static final String RESULT =
        "{\"$query\":{}," + "\"$hits\":{\"total\":100,\"offset\":0,\"limit\":100}," + "\"$results\":";

    private static final String UNIT =
        "{\"DescriptionLevel\":\"Item\"," +
        "\"Title\":[\"METADATA ENCODING AND TRANSMISSION STANDARD: PRIMER AND REFERENCE MANUAL\",\"Manuel METS revu et corrigé\"]," +
        "\"Description\":[\"METSPrimerRevised.pdf\",\"Pseudo Archive METSPrimerRevised.pdf\"]," +
        "\"Tag\":[\"METS\",\"norme internationale\"],\"TransactedDate\":\"2012-09-16T10:22:02\"," +
        "\"Event\":[{\"EventType\":\"Création\",\"EventDateTime\":\"2010-01-01T10:22:02\"},{\"EventType\":\"Validation\",\"EventDateTime\":\"2010-02-01T10:22:02\"}]," +
        "\"_uds\":[{\"aeaaaaaaaaaam7mxaa7hcakyq4z6soyaaaaq\":1}],\"#id\":\"aeaaaaaaaaaam7mxaa7hcakyq4z6spqaaaaq\",\"#nbunits\":0,\"#tenant\":0," +
        "\"#object\":\"aeaaaaaaaaaam7mxaa7hcakyq4z6sjqaaaaq\",\"#unitups\":[\"aeaaaaaaaaaam7mxaa7hcakyq4z6soyaaaaq\"],\"#min\":1,\"#max\":2," +
        "\"#allunitups\":[\"aeaaaaaaaaaam7mxaa7hcakyq4z6soyaaaaq\"],\"#operations\":[\"aedqaaaaacaam7mxabhniakyq4z4ewaaaaaq\"]}";

    private static final String LOGBOOK_OPERATION =
        "\"evId\": \"aedqaaaaacaam7mxaaaamakvhiv4rsqaaaaq\"," +
        "\"evType\": \"Process_SIP_unitary\"," +
        "\"evDateTime\": \"2016-06-10T11:56:35.914\"," +
        "\"evIdProc\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq\"," +
        "\"evTypeProc\": \"INGEST\"," +
        "\"outcome\": \"STARTED\"," +
        "\"outDetail\": null," +
        "\"outMessg\": \"SIP entry : SIP.zip\"," +
        "\"agId\": \"{\\\"Name\\\":\\\"vitam-iaas-app-01\\\",\\\"Role\\\":\\\"ingest-external\\\",\\\"ServerId\\\":1048375580,\\\"SiteId\\\":1,\\\"GlobalPlatformId\\\":243069212}\"," +
        "\"agIdApp\": null," +
        "\"evIdAppSession\": null," +
        "\"evIdReq\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq\"," +
        "\"agIdExt\": null," +
        "\"obId\": null," +
        "\"obIdReq\": null," +
        "\"obIdIn\": null," +
        "\"events\": []}";

    private static final String ACCESSION_DETAIL =
        "{" +
        "\"_id\": \"aedqaaaaacaam7mxabsakakygeje2uyaaaaq\"," +
        "\"_tenant\": 0," +
        "\"OriginatingAgency\": \"FRAN_NP_005568\"," +
        "\"SubmissionAgency\": \"FRAN_NP_005061\"," +
        "\"EndDate\": \"2016-11-04T21:40:47.912+01:00\"," +
        "\"StartDate\": \"2016-11-04T21:40:47.912+01:00\"," +
        "\"Status\": \"STORED_AND_COMPLETED\"," +
        "\"TotalObjectGroups\": {" +
        "    \"total\": 1," +
        "    \"deleted\": 0," +
        "    \"remained\": 1" +
        "}," +
        "\"TotalUnits\": {" +
        "    \"total\": 1," +
        "    \"deleted\": 0," +
        "    \"remained\": 1" +
        "}," +
        "\"TotalObjects\": {" +
        "    \"total\": 4," +
        "    \"deleted\": 0," +
        "    \"remained\": 4" +
        "}," +
        "\"ObjectSize\": {" +
        "    \"total\": 345042," +
        "    \"deleted\": 0," +
        "    \"remained\": 345042" +
        "}}";

    private static final String TRACEABILITY_OPERATION =
        "{$hint: {'total':'1'},$context:{$query: {$eq: {\"id\" : \"1\" }}, $projection: {}, $filter: {}},$result:" +
        " {\"LogType\":\"OPERATION\",\"StartDate\":\"2017-03-02T12:46:25.618\",\"EndDate\":\"2017-03-02T14:22:34.811\"" +
        ",\"Hash\":\"obu+Z7+M7JlaY5ney0dDNTzmWr4+r6Cf3GtwMrLKpCLIL7Bjqty8kfUNSGNQ9qlzO3YE7+zRdC2o/S+uUOJM4g==\"," +
        "\"TimeStampToken\":\"MIIEZzAVAgEAMBAMDk9wZXJhdGlvbiBPa2F5MIIETAYJKoZIhvcNAQcCoIIEPTCCBDkCAQMxDzANBglghkgBZQMEAgMFADCBgAYLKoZIhvcNAQkQAQSgcQRvMG0CAQEGASkwUTANBglghkgBZQMEAgMFAARAMcCUdvY+87OSgJw3slg+aHgTt+j+UcybWmhG0G9wL7A0NE8eUO8JFJRD81Gb4wTu12AealKzWeUoLczLWRhACQIBARgPMjAxNzAzMDIxNDIyMzVaMYIDnjCCA5oCAQEwTzBKMQswCQYDVQQGEwJGUjEPMA0GA1UECBMGRnJhbmNlMQ4wDAYDVQQHEwVQQXJpczENMAsGA1UEChMERXRhdDELMAkGA1UECxMCRVQCAQEwDQYJYIZIAWUDBAIDBQCgggEgMBoGCSqGSIb3DQEJAzENBgsqhkiG9w0BCRABBDAcBgkqhkiG9w0BCQUxDxcNMTcwMzAyMTQyMjM1WjAtBgkqhkiG9w0BCTQxIDAeMA0GCWCGSAFlAwQCAwUAoQ0GCSqGSIb3DQEBDQUAME8GCSqGSIb3DQEJBDFCBECDgiYpV3Bd/9ckq0Nq+zm/TmPWO3rCt33o+cKA8DI5L9EGiEV2GtjO0z4jnw08/iZ3chanYRrR8hJk4u4Fmqk4MGQGCyqGSIb3DQEJEAIvMVUwUzBRME8wCwYJYIZIAWUDBAIDBEACrDhLC92Gcm2mclJwDuLADobFbVWY5HASuT8FL0/1nNexkoSVnVclwr0VkxJgmVAxa+g8ecGbwLzDs8vbT9AYMA0GCSqGSIb3DQEBDQUABIICABBtgwv5dZJhWu050wXM27HGZF6yFaU6AOPjjUIQccbly1nw/Sebg2xRenMv+jxAm8bAdW3eBPNrSa1iJKGLmgfYZuxIwt5AluzVuvTH2t1j08XSMmDQpilhN6Bx9dOS3aClNm89ZVygmdUz6N9HGmxZh1yis80SxD6jDjhnuK6R+xpmdmL6HIDzHa5mjOCbD1lXtr5tWWR+1K9Axv4qoLADlrUKmpCdbnfUBgVdbWJnJBcW6WoQXDpTD/JbV/m2s/yzK7FDt+IpERwVZGqiZF9WwzgX9AFJ352tFWWj+KhqfFN8UgfPKD5aCo6V5T+vMlkodiV+0XQBPKdcPBoo8PJtiCR5sFlJHbQ0qGwQq6P/06ApvrXOFKz434CkCGTbZLKVEHTs1SqgZuAg54bR6GbifOheVojOhWDFEMEXlnpD7PjCYscL9U83H92Iosr4rJhvYwSYbahmU7W1RlUP4cHy8nUYwvUO54ehYMcKcZ4idG4nbUfVYy8lClYruKG0rVcwWu3TZxeSaLePYRZwaeXJ00p36SttyZTLeN4rZatePQc/ff856jvKqDlDZlXIyhOcYCXot5xHFlv+zKZhB71uOv/XvqqmZFhentmkZFYK+qi2tH+Tahar2f3b4RJo+bm235g/v1rIvKQKe8rjkemgPIG7RzkCnss2SGIpfW5h\"," +
        "\"NumberOfElements\":4," +
        "\"FileName\":\"0_LogbookOperation_20170302_142234.zip\"," +
        "\"Size\":41807}}";

    private static final String TRACEABILITY_LOGBOOK_OPERATION =
        "{\"evId\": \"aedqaaaaacaam7mxaaaamakvhiv4rsqaaaaq\"," +
        "\"evType\": \"CHECK_TRACEABILITY_OPERATION\"," +
        "\"evDateTime\": \"2016-06-10T11:56:35.914\"," +
        "\"evIdProc\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq\"," +
        "\"evTypeProc\": \"TRACEABILITY\"," +
        "\"outcome\": \"STARTED\"," +
        "\"outDetail\": null," +
        "\"outMessg\": \"SIP entry : SIP.zip\"," +
        "\"agId\": \"{\\\"Name\\\":\\\"vitam-iaas-app-01\\\",\\\"Role\\\":\\\"storage\\\",\\\"ServerId\\\":1048375580,\\\"SiteId\\\":1,\\\"GlobalPlatformId\\\":243069212}\"," +
        "\"agIdApp\": null," +
        "\"evIdAppSession\": null," +
        "\"evIdReq\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq\"," +
        "\"agIdExt\": null," +
        "\"obId\": null," +
        "\"obIdReq\": null," +
        "\"obIdIn\": null," +
        "\"events\": [{" +
        "\"evTypeProc\": \"TRACEABILITY\"," +
        "\"evDetData\": {\"LogType\":\"OPERATION\",\"StartDate\":\"2017-04-06T22:50:59.657\"," +
        "\"EndDate\":\"2017-04-06T23:01:03.121\",\"Hash\":\"HASH_TEST\",\"TimeStampToken\":\"TimeStamp_TEST\"," +
        "\"NumberOfElements\":4,\"FileName\":\"0_LogbookOperation_20170406_230103.zip\",\"Size\":4141}}]}";

    public static final String SECURITY_PROFILES =
        "{" +
        "\"_id\":\"aeaaaaaaaaaaaaabaa4ikakyetcaaaabbbcc\"," +
        "\"Identifier\": \"SEC_PROFILE-000001\"," +
        "\"Name\": \"TEST_PROFILE_1\"," +
        "\"FullAccess\": false," +
        "\"Permissions\": [" +
        "\"permission_one:read\"," +
        "\"permission_one:id:write\"" +
        "]" +
        "}";

    private ClientMockResultHelper() {}

    /**
     * @return a default Logbook Result
     * @throws InvalidParseOperationException
     */
    public static List<LogbookOperation> getLogbookOperations() {
        List<LogbookOperation> logbookOperations = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            logbookOperations.add(getLogbookOperationItem());
        }
        return logbookOperations;
    }

    private static LogbookLifecycle getLogbookLifecycleItem() {
        LogbookLifecycle logbookLifecycle = new LogbookLifecycle();
        logbookLifecycle.setId("aedqaaaaacaam7mxaaaamakvhiv4rsqaaaaq");
        logbookLifecycle.setEvId("aedqaaaaacaam7mxaaaamakvhiv4rsqaaaaq");
        logbookLifecycle.setEvType("LFC.LFC_CREATION");
        logbookLifecycle.setEvDateTime("2016-06-10T11:56:35.914");
        logbookLifecycle.setEvIdProc("aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq");
        logbookLifecycle.setEvTypeProc("INGEST");
        logbookLifecycle.setOutcome("OK");
        logbookLifecycle.setOutDetail(null);
        logbookLifecycle.setOutMessg("Création du journal du cycle de vie");
        logbookLifecycle.setAgId(
            "\"{\\\"Name\\\":\\\"vitam-iaas-app-01\\\",\\\"Role\\\":\\\"ingest-external\\\",\\\"ServerId\\\":1048375580,\\\"SiteId\\\":1,\\\"GlobalPlatformId\\\":243069212}\""
        );
        return logbookLifecycle;
    }

    private static LogbookOperation getLogbookOperationItem() {
        LogbookOperation logbookOperation = new LogbookOperation();
        logbookOperation.setId("aedqaaaaacaam7mxaaaamakvhiv4rsqaaaaq");
        logbookOperation.setEvId("aedqaaaaacaam7mxaaaamakvhiv4rsqaaaaq");
        logbookOperation.setEvType("Process_SIP_unitary");
        logbookOperation.setEvDateTime("2016-06-10T11:56:35.914");
        logbookOperation.setEvIdProc("aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq");
        logbookOperation.setEvTypeProc("INGEST");
        LogbookEventOperation startEvent = new LogbookEventOperation();
        startEvent.setEvType("TNR_PERFORMANCE.STARTED");
        startEvent.setEvDateTime("2016-06-10T11:56:35.914");
        LogbookEventOperation endEvent = new LogbookEventOperation();
        endEvent.setEvType("TNR_PERFORMANCE.COMPLETED");
        endEvent.setEvDateTime("2016-06-10T11:58:35.914");
        logbookOperation.setEvents(Arrays.asList(startEvent, endEvent));
        logbookOperation.setOutcome("STARTED");
        logbookOperation.setOutDetail(null);
        logbookOperation.setOutMessg("\"SIP entry : SIP.zip");
        logbookOperation.setAgId(
            "\"{\\\"Name\\\":\\\"vitam-iaas-app-01\\\",\\\"Role\\\":\\\"ingest-external\\\",\\\"ServerId\\\":1048375580,\\\"SiteId\\\":1,\\\"GlobalPlatformId\\\":243069212}\""
        );
        logbookOperation.setAgIdApp(null);
        logbookOperation.setEvIdAppSession(null);
        logbookOperation.setEvIdReq("aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq");
        return logbookOperation;
    }

    private static FileFormatModel getFormatItem() {
        FileFormatModel fileFormatModel = new FileFormatModel();
        fileFormatModel.setId("aeaaaaaaaahwaqr6aabrqak6k3jcp2yaaaka");
        fileFormatModel.setCreatedDate("2016-09-27T15:37:53");
        fileFormatModel.setVersionPronom("88");
        fileFormatModel.setMimeType("application/postscript");
        fileFormatModel.setPuid("x-fmt/20");
        fileFormatModel.setVersionPronom("1.0 / 1.1");
        fileFormatModel.setName("Adobe Illustrator");
        fileFormatModel.setExtensions(
            new ArrayList<String>() {
                {
                    add("ai");
                }
            }
        );
        fileFormatModel.setHasPriorityOverFileFormatIDs(
            new ArrayList<String>() {
                {
                    add("fmt/122");
                    add("fmt/123");
                    add("fmt/124");
                }
            }
        );
        fileFormatModel.setVersion("0");
        fileFormatModel.setAlert(false);
        return fileFormatModel;
    }

    private static FileRulesModel getRuleItem() {
        FileRulesModel rule = new FileRulesModel();
        rule.setId("aeaaaaaaaaaaaaabaa4ikakyetch6mqaaacq");
        rule.setTenant(0);
        rule.setRuleId("APP-00005");
        rule.setRuleType(RuleType.AppraisalRule);
        rule.setRuleValue("Pièces comptables (comptable)");
        rule.setRuleDescription(
            "Durée de conservation des pièces comptables pour le comptable l’échéance est calculée à partir de la date de solde comptable"
        );
        rule.setRuleDuration("6");
        rule.setRuleMeasurement(RuleMeasurementEnum.YEAR);
        rule.setCreationDate("2016-11-02");
        rule.setUpdateDate("2016-11-03");
        return rule;
    }

    private static IngestContractModel getIngestContractItem() {
        IngestContractModel contract = new IngestContractModel();
        contract.setId("aeaaaaaaaaaaaaabaa4ikakyetch6mqaaacq");
        contract.setTenant(0);
        contract.setIdentifier("FAKE_IDENTIFIER");
        contract.setName("Un contrat");
        contract.setDescription("DESCRIPTION D'UN CONTRAT");
        contract.setStatus(ActivationStatus.ACTIVE);
        contract.setCreationdate("2016-11-02");
        contract.setLastupdate("2016-11-05");
        contract.setActivationdate("2016-11-04");
        contract.setDeactivationdate("2016-11-03");
        return contract;
    }

    private static AgenciesModel getAgenciesModel() {
        AgenciesModel model = new AgenciesModel();
        model.setId("aeaaaaaaaaaaaaabaa4ikakyetch6mqaaacq");
        model.setTenant(0);
        model.setIdentifier("FAKE_IDENTIFIER");
        model.setName("Une agency");
        model.setDescription("DESCRIPTION D'UNE AGENCE");
        return model;
    }

    private static AccessContractModel getAccessContractItem() {
        AccessContractModel contract = new AccessContractModel();
        contract.setId("aeaaaaaaaaaaaaabaa4ikakyetch6mqaaacq");
        contract.setTenant(0);
        contract.setIdentifier("FAKE_IDENTIFIER");
        contract.setName("Un contrat");
        contract.setDescription("DESCRIPTION D'UN CONTRAT");
        contract.setStatus(ActivationStatus.ACTIVE);
        contract.setCreationdate("2016-11-02");
        contract.setLastupdate("2016-11-05");
        contract.setActivationdate("2016-11-04");
        contract.setDeactivationdate("2016-11-03");
        contract.setDataObjectVersion(
            new HashSet<String>() {
                {
                    add("PhysicalMaster");
                    add("BinaryMaster");
                }
            }
        );
        contract.setWritingPermission(true);
        contract.setOriginatingAgencies(
            new HashSet<String>() {
                {
                    add("FR_ORG_AGEC");
                    add("OriginatingAgency");
                }
            }
        );
        contract.setEveryDataObjectVersion(false);
        contract.setEveryOriginatingAgency(false);
        return contract;
    }

    private static ManagementContractModel getManagementContractItem() {
        ManagementContractModel contract = new ManagementContractModel();
        contract.setId("aeaaaaaaaaaaaaabaa4ikakyetch6mqaaacq");
        contract.setTenant(0);
        contract.setIdentifier("FAKE_IDENTIFIER");
        contract.setName("Un contrat");
        contract.setDescription("DESCRIPTION D'UN CONTRAT");
        contract.setStatus(ActivationStatus.ACTIVE);
        contract.setCreationdate("2016-11-02");
        contract.setLastupdate("2016-11-05");
        contract.setActivationdate("2016-11-04");
        contract.setDeactivationdate("2016-11-03");
        contract.setStorage(
            new StorageDetailModel()
                .setUnitStrategy("default")
                .setObjectGroupStrategy("default")
                .setObjectStrategy("default")
        );
        return contract;
    }

    private static ContextModel getContextItem() {
        PermissionModel permission = new PermissionModel();
        permission.setTenant(0);
        ContextModel context = new ContextModel();
        context.setId("aeaaaaaaaaaaaaabaa4ikakyetch6mqaaacq");
        context.setIdentifier("FAKE_IDENTIFIER");
        context.setName("My_Context_1");
        context.setDeactivationdate("DESCRIPTION D'UN CONEXTE");
        context.setStatus(ContextStatus.ACTIVE);
        context.setCreationdate("2016-11-02");
        context.setLastupdate("2016-11-05");
        context.setActivationdate("2016-11-04");
        context.setDeactivationdate("2016-11-03");
        context.setPermissions(
            new ArrayList<PermissionModel>() {
                {
                    add(permission);
                }
            }
        );
        return context;
    }

    private static ProfileModel getProfileItem() {
        ProfileModel profile = new ProfileModel();
        profile.setId("aeaaaaaaaaaaaaabaa4ikakyetch6mqaaacq");
        profile.setTenant(0);
        profile.setIdentifier("FAKE_IDENTIFIER");
        profile.setName("Un Profile");
        profile.setDeactivationdate("DESCRIPTION D'UN PROFILE");
        profile.setStatus(ProfileStatus.ACTIVE);
        profile.setFormat(ProfileFormat.XSD);
        profile.setCreationdate("2016-11-02");
        profile.setLastupdate("2016-11-05");
        profile.setActivationdate("2016-11-04");
        profile.setDeactivationdate("2016-11-03");
        return profile;
    }

    private static ArchiveUnitProfileModel getArchiveUnitItem() {
        ArchiveUnitProfileModel profile = new ArchiveUnitProfileModel();

        profile.setId("aeaaaaaaaaaaaaabaa4ikakyetch6mqaaacq");
        profile.setTenant(0);
        profile.setIdentifier("FAKE_IDENTIFIER");
        profile.setName("Archive Unit Name");
        profile.setDeactivationdate("Description complète d'un Document Type");
        profile.setStatus(ArchiveUnitProfileStatus.ACTIVE);
        profile.setCreationdate("2016-11-02");
        profile.setLastupdate("2016-11-05");
        profile.setActivationdate("2016-11-04");
        profile.setDeactivationdate("2016-11-03");

        return profile;
    }

    private static OntologyModel getOntologyItem() {
        OntologyModel ontology = new OntologyModel();
        ontology.setId("aeaaaaaaaaaaaaabaa4ikakyetch6mqaaacq");
        ontology.setTenant(1);
        ontology.setIdentifier("_sps");
        ontology.setApiField("#originating_agencies");
        ontology.setSedaField("OriginatingAgencyIdentifier");
        ontology.setDescription("Internal ontology sample");
        ontology.setOrigin(OntologyOrigin.INTERNAL);
        ontology.setType(OntologyType.KEYWORD);
        ontology.setShortName("Originating Agency Identifier");
        ontology.setCollections(
            new ArrayList<String>() {
                {
                    add("Unit");
                    add("ObjectGroup");
                }
            }
        );
        ontology.setVersion(0);
        ontology.setCreationdate("2017-04-09");
        ontology.setLastupdate("2017-04-09");
        return ontology;
    }

    private static AccessionRegisterSummaryModel getAccessionRegisterSummaryItem() {
        AccessionRegisterSummaryModel accessionRegister = new AccessionRegisterSummaryModel();
        accessionRegister.setId("aedqaaaaacaam7mxabsakakygeje2uyaaaaq");
        accessionRegister.setTenant(0);
        accessionRegister.setOriginatingAgency("FRAN_NP_005568");
        accessionRegister.setCreationDate("22016-11-04T20:40:49.030");
        accessionRegister.setTotalObjectsGroups(
            new RegisterValueDetailModel().setIngested(3).setDeleted(0).setRemained(3)
        );
        accessionRegister.setTotalUnits(new RegisterValueDetailModel().setIngested(3).setDeleted(0).setRemained(3));
        accessionRegister.setTotalObjects(new RegisterValueDetailModel().setIngested(12).setDeleted(0).setRemained(12));
        accessionRegister.setObjectSize(
            new RegisterValueDetailModel().setIngested(1035126).setDeleted(0).setRemained(1035126)
        );
        return accessionRegister;
    }

    private static ObjectNode getUnitSimpleItem() {
        ObjectNode node = JsonHandler.createObjectNode();
        node.put("#id", "aedqaaaaacaam7mxaaaamakvhiv4rsqaaaaq");
        node.put("Title", "Archive 1");
        node.put("DescriptionLevel", "Archive Mock");
        return node;
    }

    private static ObjectNode getGotSimpleItem() {
        ObjectNode got = JsonHandler.createObjectNode();
        ArrayNode versions = JsonHandler.createArrayNode();
        ObjectNode binaryMaster1 = JsonHandler.createObjectNode();
        binaryMaster1.put("#id", "aedqaaaaacaam7mxaaaamakvhiv4rsqaaaaq");
        binaryMaster1.put("DataObjectVersion", "BinaryMaster_1");
        binaryMaster1.put("Size", "6");
        binaryMaster1.put("LastModified", "2017-04-04T08:07:06.487+02:00");
        binaryMaster1.put("FormatLitteral", "Plain Text File");
        binaryMaster1.put("FileName", "Stalingrad.txt");
        binaryMaster1.put("LastModified", "2017-04-04T08:07:06.487+02:00");
        binaryMaster1.put("LastModified", "2017-04-04T08:07:06.487+02:00");
        binaryMaster1.put("LastModified", "2017-04-04T08:07:06.487+02:00");
        ObjectNode metadatas = JsonHandler.createObjectNode();
        metadatas.put("_id", "aedqaaaaacaam7mxaaaamakvhiv4rsqaaaaq");
        metadatas.put("DataObjectGroupId", "aebaaaaaaagc44lgabqraak63u6iq2aaaabq");
        metadatas.put("DataObjectVersion", "BinaryMaster_1");
        metadatas.put("Uri", "Content/ID55.txt");
        metadatas.put(
            "MessageDigest",
            "86c0bc701ef6b5dd21b080bc5bb2af38097baa6237275da83a52f092c9eae3e4e4b0247391620bd732fe824d18bd3bb6c37e62ec73a8cf3585c6a799399861b1"
        );
        metadatas.put("Algorithm", "SHA-512");
        ObjectNode formatIdentification = JsonHandler.createObjectNode();
        formatIdentification.put("FormatLitteral", "Plain Text File");
        formatIdentification.put("MimeType", "text/plain");
        formatIdentification.put("FormatId", "x-fmt/111");
        metadatas.set("FormatIdentification", formatIdentification);
        ObjectNode fileInfo = JsonHandler.createObjectNode();
        fileInfo.put("Filename", "Stalingrad.txt");
        fileInfo.put("LastModified", "2017-04-04T08:07:06.487+02:00");
        metadatas.set("FileInfo", fileInfo);
        binaryMaster1.set("metadatas", metadatas);
        versions.add(binaryMaster1);
        got.put("nbObjects", 1);
        got.set("versions", versions);
        return got;
    }

    /**
     * FIXME to remove in 2905
     *
     * @return a default Logbook Result
     * @throws InvalidParseOperationException
     */
    @Deprecated
    public static JsonNode getLogbookResults() throws InvalidParseOperationException {
        final StringBuilder result = new StringBuilder(RESULT).append("[");
        for (int i = 0; i < 100; i++) {
            result
                .append("{\"_id\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaa")
                .append(i)
                .append("\",")
                .append(LOGBOOK_OPERATION);
            if (i < 99) {
                result.append(",");
            }
        }
        result.append("]}");
        return JsonHandler.getFromString(result.toString());
    }

    /**
     * FIXME to remove in 2905
     *
     * @return a default Logbook operations response Result
     * @throws InvalidParseOperationException
     */
    @Deprecated
    public static RequestResponse getLogbooksRequestResponseJsonNode() throws InvalidParseOperationException {
        return RequestResponseOK.getFromJsonNode(getLogbookResults());
    }

    /**
     * @return a default Logbook operations response Result
     */
    public static RequestResponse<LogbookOperation> getLogbookOperationsRequestResponse() {
        return new RequestResponseOK<LogbookOperation>()
            .addAllResults(getLogbookOperations())
            .setHttpCode(Status.OK.getStatusCode());
    }

    /**
     * @return one default Logbook operation response
     */
    public static RequestResponse<LogbookOperation> getLogbookOperationRequestResponse() {
        return new RequestResponseOK<LogbookOperation>()
            .addResult(getLogbookOperationItem())
            .setHttpCode(Status.OK.getStatusCode());
    }

    /**
     * @return one default Logbook lifecycle response
     */
    public static RequestResponse<LogbookLifecycle> getLogbookLifecycleRequestResponse() {
        return new RequestResponseOK<LogbookLifecycle>()
            .addResult(getLogbookLifecycleItem())
            .setHttpCode(Status.OK.getStatusCode());
    }

    public static JsonNode getMetaDataResult() throws InvalidParseOperationException {
        return getArchiveUnitResult().toJsonNode();
    }

    /**
     * @param s the original object to be included in response
     * @return a default response
     * @throws InvalidParseOperationException
     */
    public static RequestResponse createReponse(Object s) {
        return new RequestResponseOK().addResult(s);
    }

    /**
     * @param s the original object to be included in response
     * @return a default response
     * @throws InvalidParseOperationException
     */
    public static RequestResponse<JsonNode> createReponse(String s) throws InvalidParseOperationException {
        RequestResponseOK<JsonNode> responseOK = new RequestResponseOK<>();
        if (null != s) responseOK.addResult(JsonHandler.getFromString(s));
        return responseOK.setHttpCode(Status.OK.getStatusCode());
    }

    /**
     * @param s the original object to be included in response
     * @param statuscode status to be returned
     * @return a default response
     * @throws InvalidParseOperationException
     */
    public static RequestResponse createReponse(String s, int statuscode) throws InvalidParseOperationException {
        RequestResponseOK responseOK = new RequestResponseOK();
        if (null != s) responseOK.addResult(JsonHandler.getFromString(s));
        return responseOK.setHttpCode(statuscode);
    }

    /**
     * @return a default Access Register Summary
     */
    public static RequestResponse<AccessionRegisterSummaryModel> getAccessionRegisterSummary() {
        return new RequestResponseOK<AccessionRegisterSummaryModel>()
            .addResult(getAccessionRegisterSummaryItem())
            .setHttpCode(Status.OK.getStatusCode());
    }

    /**
     * @return a default Access Register Detail
     * @throws InvalidParseOperationException
     */
    public static RequestResponse getAccessionRegisterDetail() throws InvalidParseOperationException {
        return createReponse(ACCESSION_DETAIL);
    }

    /**
     * @return a default Format
     */
    public static RequestResponse<FileFormatModel> getFormat() {
        return new RequestResponseOK<FileFormatModel>()
            .addResult(getFormatItem())
            .setHttpCode(Status.OK.getStatusCode());
    }

    /**
     * @param statusCode
     * @return a default Format
     */
    public static RequestResponse<FileFormatModel> getFormat(int statusCode) {
        return new RequestResponseOK<FileFormatModel>().addResult(getFormatItem()).setHttpCode(statusCode);
    }

    /**
     * @return a default Rule
     */
    public static RequestResponse<FileRulesModel> getRule() {
        return new RequestResponseOK<FileRulesModel>().addResult(getRuleItem()).setHttpCode(Status.OK.getStatusCode());
    }

    /**
     * @param statusCode
     * @return a default Rule
     */
    public static RequestResponse<FileRulesModel> getRule(int statusCode) {
        return new RequestResponseOK<FileRulesModel>().addResult(getRuleItem()).setHttpCode(statusCode);
    }

    /**
     * @return a RequestResponse containing contracts json
     */
    public static RequestResponse<IngestContractModel> getIngestContracts() {
        return new RequestResponseOK<IngestContractModel>()
            .addResult(getIngestContractItem())
            .setHttpCode(Status.OK.getStatusCode());
    }

    /**
     * @return a RequestResponse containing access contracts json
     */
    public static RequestResponse<AccessContractModel> getAccessContracts() {
        return new RequestResponseOK<AccessContractModel>()
            .addResult(getAccessContractItem())
            .setHttpCode(Status.OK.getStatusCode());
    }

    /**
     * @return a RequestResponse containing access contracts json
     */
    public static RequestResponse<ManagementContractModel> getManagementContracts() {
        return new RequestResponseOK<ManagementContractModel>()
            .addResult(getManagementContractItem())
            .setHttpCode(Status.OK.getStatusCode());
    }

    /**
     * @return a RequestResponse containing agencies json
     */
    public static RequestResponse<AgenciesModel> getAgencies() {
        return new RequestResponseOK<AgenciesModel>()
            .addResult(getAgenciesModel())
            .setHttpCode(Status.OK.getStatusCode());
    }

    /**
     * @param statusCode
     * @return a RequestResponse containing agencies json
     */
    public static RequestResponse<AgenciesModel> getAgencies(int statusCode) {
        return new RequestResponseOK<AgenciesModel>().addResult(getAgenciesModel()).setHttpCode(statusCode);
    }

    /**
     * @return context json
     */
    public static RequestResponse<ContextModel> getContexts(int statusCode) {
        return new RequestResponseOK<ContextModel>().addResult(getContextItem()).setHttpCode(statusCode);
    }

    public static RequestResponse<ProfileModel> getProfiles(int statusCode) {
        return new RequestResponseOK<ProfileModel>().addResult(getProfileItem()).setHttpCode(statusCode);
    }

    /**
     * @param statusCode
     * @return a RequestResponse containing Ontology json
     */
    public static RequestResponse<OntologyModel> getOntologies(int statusCode) {
        return new RequestResponseOK<OntologyModel>().addResult(getOntologyItem()).setHttpCode(statusCode);
    }

    /**
     * @return a default list of Formats
     * @throws InvalidParseOperationException
     */
    public static RequestResponse<FileFormatModel> getFormatList() {
        return new RequestResponseOK<FileFormatModel>()
            .addResult(getFormatItem())
            .setHttpCode(Status.OK.getStatusCode());
    }

    public static RequestResponse<FileRulesModel> getRuleList() {
        return new RequestResponseOK<FileRulesModel>().addResult(getRuleItem()).setHttpCode(Status.OK.getStatusCode());
    }

    /**
     * @return a default list of Rules
     * @throws InvalidParseOperationException
     */
    public static RequestResponse getEmptyResult() {
        return new RequestResponseOK<>().setHttpCode(Status.OK.getStatusCode());
    }

    /**
     * @return a default ArchiveUnit result
     * @throws InvalidParseOperationException
     */
    public static RequestResponse getArchiveUnitResult() throws InvalidParseOperationException {
        return createReponse(UNIT);
    }

    /**
     * @return a simple ArchiveUnit result
     */
    public static RequestResponse<JsonNode> getArchiveUnitSimpleResult(JsonNode query) {
        return new RequestResponseOK<JsonNode>(query)
            .addResult(getUnitSimpleItem())
            .setHttpCode(Status.OK.getStatusCode());
    }

    /**
     * @return a simple Operation result
     * @throws VitamClientException
     */
    public static RequestResponse<JsonNode> getOperationSimpleResult(String itemId) throws VitamClientException {
        try {
            ItemStatus itemStatus = new ItemStatus(itemId);
            itemStatus.setGlobalState(ProcessState.RUNNING);
            itemStatus.increment(StatusCode.STARTED);
            return new RequestResponseOK<JsonNode>()
                .addResult(JsonHandler.toJsonNode(itemStatus))
                .setHttpCode(Status.OK.getStatusCode());
        } catch (InvalidParseOperationException e) {
            throw new VitamClientException(e);
        }
    }

    /**
     * @return a simple GOT result
     */
    public static RequestResponse<JsonNode> getGotSimpleResult(JsonNode query) {
        return new RequestResponseOK<JsonNode>(query)
            .addResult(getGotSimpleItem())
            .setHttpCode(Status.OK.getStatusCode());
    }

    /**
     * @return a default ArchiveUnit result
     * @throws InvalidParseOperationException
     */
    public static Response getObjectStream() {
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add("Content-Disposition", "filename=\"test.txt\"");
        return new AbstractMockClient.FakeInboundResponse(
            Status.OK,
            new ByteArrayInputStream("test".getBytes()),
            MediaType.APPLICATION_OCTET_STREAM_TYPE,
            headers
        );
    }

    /**
     * @param id
     * @return a default ArchiveUnit result
     * @throws InvalidParseOperationException
     */
    public static ItemStatus getItemStatus(String id) throws InvalidParseOperationException {
        return new ItemStatus(id);
    }

    /**
     * @throws InvalidParseOperationException
     */
    public static RequestResponse<JsonNode> checkOperationTraceability() throws InvalidParseOperationException {
        return createReponse(TRACEABILITY_OPERATION);
    }

    /**
     * @return
     * @throws VitamClientException
     */
    public static RequestResponse getSecurityProfiles() throws VitamClientException {
        try {
            return createReponse(SECURITY_PROFILES);
        } catch (InvalidParseOperationException e) {
            throw new VitamClientException(e);
        }
    }

    /**
     * @param statusCode
     * @return
     * @throws VitamClientException
     */
    public static RequestResponse getSecurityProfiles(int statusCode) throws VitamClientException {
        try {
            return createReponse(SECURITY_PROFILES, statusCode);
        } catch (InvalidParseOperationException e) {
            throw new VitamClientException(e);
        }
    }

    public static RequestResponse<JsonNode> getDIPSimpleResult(JsonNode dslRequest) {
        return new RequestResponseOK<JsonNode>(dslRequest)
            .addResult(getUnitSimpleItem())
            .setHttpCode(Status.OK.getStatusCode());
    }

    public static RequestResponse<JsonNode> getEvidenceAudit(int statusCode) {
        return new RequestResponseOK<JsonNode>().addResult(JsonHandler.createObjectNode()).setHttpCode(statusCode);
    }

    public static RequestResponse<ProfileModel> getProbativeValue(int statusCode) {
        return new RequestResponseOK<ProfileModel>().addResult(getProfileItem()).setHttpCode(statusCode);
    }

    /**
     * Get archive unit profiles mock with the given status code
     *
     * @param statusCode
     * @return a mock of archive unit profiles
     */
    public static RequestResponse<ArchiveUnitProfileModel> getArchiveUnitProfiles(int statusCode) {
        return new RequestResponseOK<ArchiveUnitProfileModel>().addResult(getArchiveUnitItem()).setHttpCode(statusCode);
    }
}

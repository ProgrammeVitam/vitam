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
package fr.gouv.vitam.common.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.OntologyOrigin;
import fr.gouv.vitam.common.model.administration.OntologyType;
import fr.gouv.vitam.common.stream.StreamUtils;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Arrays;
import java.util.Collections;
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

    public static final String UNIT_RAW =
        "{   \"_id\": \"aeaqaaaabufhszkyabon6albarqq7qiaaakq\"," +
        "    \"DescriptionLevel\": \"Item\"," +
        "    \"Title\": \"Title test\"," +
        "    \"Titles\": {\"fr\": \"Title test\"}," +
        "    \"Description\": \"Pseudo Archive Unit du fichier.pdf\"," +
        "    \"Descriptions\": {\"fr\": \"Pseudo ArchiveE.pdf\"}," +
        "    \"TransactedDate\": \"2016-09-22T12:33:02\"," +
        "    \"_og\": \"aebaaaaabufhszkyabon6albarqq7qiaaala\"," +
        "    \"_ops\":[\"aedqaaaaacfe6vjwab36ealbbrqw2uyaaaaq\"]," +
        "    \"_opi\":\"aedqaaaaacfe6vjwab36ealbbrqw2uyaaaaq\"," +
        "    \"_up\": [\"aeaqaaaaaaghay2jabzuaalbaregxuiaaaba\"]," +
        "    \"_us\": [\"aeaqaaaaaaghay2jabzuaalbaregxuiaaaba\"]," +
        "    \"_sp\": \"vitam\"," +
        "    \"_sps\": [\"vitam\"]," +
        "    \"_uds\": {\"aeaqaaaaaaghay2jabzuaalbaregxuiaaaba\": 1}," +
        "    \"_min\": 1," +
        "    \"_max\": 2," +
        "    \"_tenant\": 0," +
        "    \"_v\": 1,\n" +
        "    \"_storage\": {\"strategyId\": \"default\"}" +
        "}";

    public static final String GOT_RAW =
        "{\"_id\": \"aebaaaaabufhszkyabon6albarqq7qiaaala\"," +
        "    \"_profil\": \"\"," +
        "    \"FileInfo\": {\"Filename\": \"jeux_test.ods\",\"LastModified\": \"2016-11-21T18:28:41.510+01:00\"}," +
        "    \"_qualifiers\": [{" +
        "            \"qualifier\": \"BinaryMaster\"," +
        "            \"_nbc\": 1," +
        "            \"versions\": [{" +
        "                    \"_id\": \"aeaaaaaaaagwky22aboqialbbrqyglqaaaaq\"," +
        "                    \"DataObjectGroupId\": \"aebaaaaaaagwky22aboqialbbrqygmaaaaaq\"," +
        "                    \"DataObjectVersion\": \"BinaryMaster_1\"," +
        "                    \"FormatIdentification\": {\"FormatLitteral\": \"OpenDocument Spreadsheet\",\"MimeType\": \"application/vnd.oasis.opendocument.spreadsheet\",\"FormatId\": \"fmt/295\"}," +
        "                    \"FileInfo\": {\"Filename\": \"jeux_test.ods\",\"LastModified\": \"2016-11-21T18:28:41.510+01:00\"}," +
        "                    \"Size\": 16532," +
        "                    \"Uri\": \"Content/ID7.ods\"," +
        "                    \"MessageDigest\": \"c117854cbca3e51ea94c4bd2bcf4a6756209e6c65ddbf696313e1801b2235ff33d44b2bb272e714c335a44a3b4f92d399056b94dff4dfe6b7038fa56f23b438e\"," +
        "                    \"Algorithm\": \"SHA-512\"," +
        "                    \"_storage\": {\"strategyId\": \"default\"}" +
        "            }]" +
        "    }]," +
        "    \"_up\": [\"aeaqaaaabufhszkyabon6albarqq7qiaaakq\"]," +
        "    \"_nbc\": 1," +
        "    \"_ops\": [\"aedqaaaaacfe6vjwab36ealbbrqw2uyaaaaq\"]," +
        "    \"_sp\": \"Vitam\"," +
        "    \"_sps\": [\"Vitam\"]," +
        "    \"_storage\": {\"strategyId\": \"default\"}," +
        "    \"_tenant\": 0," +
        "    \"_v\": 1 }";

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
        "\"agIdPers\": null," +
        "\"evIdReq\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq\"," +
        "\"agIdExt\": null," +
        "\"obId\": null," +
        "\"obIdReq\": null," +
        "\"obIdIn\": null," +
        "\"events\": []}";

    private static final String LOGBOOK_LIFECYCLE =
        "\"evId\": \"aedqaaaaacggo2kdaamz6ak5xswm3uiaaaaq\"," +
        "\"evType\": \"LFC.LFC_CREATION\"," +
        "\"evDateTime\": \"2017-08-07T12:30:27.793\"," +
        "\"evIdProc\": \"aedqaaaaacgcs7j2aa3oeak5xswlyaqaaaaq\"," +
        "\"evTypeProc\": \"INGEST\"," +
        "\"outcome\": \"OK\"," +
        "\"outDetail\": \"LFC.LFC_CREATION.OK\"," +
        "\"outMessg\": \"Création du journal du cycle de vie\"," +
        "\"agId\": \"{\\\"Name\\\":\\\"vitam-iaas-app-01\\\",\\\"Role\\\":\\\"ingest-external\\\",\\\"ServerId\\\":1048375580,\\\"SiteId\\\":1,\\\"GlobalPlatformId\\\":243069212}\"," +
        "\"obId\": \"aeaqaaaaaaggo2kdaamz6ak5xswm3jaaaaba\"," +
        "\"evDetData\": null," +
        "\"events\": []}";

    private static final String LOGBOOK_OPERATION_WITH_OBID =
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
        "\"obId\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq\"," +
        "\"obIdReq\": null," +
        "\"obIdIn\": null," +
        "\"events\": []}";

    private static final String RULE =
        "{\"_id\":\"aeaaaaaaaaaaaaabaa4ikakyetch6mqaaacq\", " +
        "\"_tenant\":\"0\", " +
        "\"RuleId\":\"APP-00005\", " +
        "\"RuleType\":\"AppraisalRule\", " +
        "\"RuleValue\":\"Pièces comptables (comptable)\", " +
        "\"RuleDescription\":\"Durée de conservation des pièces comptables pour le comptable l’échéance est calculée à partir de la date de solde comptable\", " +
        "\"RuleDuration\":\"6\", " +
        "\"RuleMeasurement\":\"year\", " +
        "\"CreationDate\":\"2016-11-02\", " +
        "\"UpdateDate\":\"2016-11-02\"}";

    private static final String AGENCY =
        "{\"_id\":\"aeaaaaaaaaaaaaabaa4ikakyetch6mqaaacq\", " +
        "\"_tenant\":\"0\", " +
        "\"Identifier\":\"AG-0000001\", " +
        "\"Name\":\"agency\", " +
        "\"Description\":\"Une description\"}";
    public static final String INGEST_CONTRACTS =
        "{\"_id\":\"aeaaaaaaaaaaaaabaa4ikakyetch6mqaaacq\", " +
        "\"_tenant\":\"0\", " +
        "\"Name\":\"ArchivalAgreement0\", " +
        "\"Identifier\":\"ArchivalAgreement0\", " +
        "\"Description\":\"DESCRIPTION D'UN CONTRAT\", " +
        "\"Status\":\"ACTIVE\", " +
        "\"CreationDate\":\"2016-11-02\", " +
        "\"ArchiveProfiles\":[\"FakeProfile\"], " +
        "\"LastUpdate\":\"2016-11-02\", " +
        "\"ActivationDate\":\"2016-11-02\", " +
        "\"DeactivationDate\":\"2016-11-02\"}";

    public static final String ACCESS_CONTRACTS =
        "{\"_id\":\"aeaaaaaaaaaaaaabaa4ikakyetch6mqaaacq\", " +
        "\"_tenant\":0, " +
        "\"DataObjectVersion\":[\"PhysicalMaster\", \"BinaryMaster\"], " +
        "\"WritingPermission\": \"true\", " +
        "\"Name\":\"Un contrat\", " +
        "\"Description\":\"DESCRIPTION D'UN CONTRAT\", " +
        "\"Status\":\"ACTIVE\", " +
        "\"OriginatingAgencies\": [\"FR_ORG_AGEC\", \"OriginatingAgency\", \"Identifier0\", \"Identifier1\", \"Identifier2\", \"Identifier3\", \"Identifier4\"], " +
        "\"EveryDataObjectVersion\": false, " +
        "\"CreationDate\":\"2016-11-02\", " +
        "\"LastUpdate\":\"2016-11-02\", " +
        "\"ActivationDate\":\"2016-11-02\", " +
        "\"EveryOriginatingAgency\": \"false\", " +
        "\"DeactivationDate\":\"2016-11-02\"}";

    public static final String MANAGEMENT_CONTRACTS =
        "{\"_id\":\"aeaaaaaaaaaaaaabaa4ikakyetch6mqaaacq\", " +
        "\"_tenant\":0, " +
        "\"Name\":\"Un contrat\", " +
        "\"Description\":\"DESCRIPTION D'UN CONTRAT\", " +
        "\"Status\":\"ACTIVE\", " +
        "\"Storage\": {\"UnitStrategy\" : \"default\", \"ObjectGroupStrategy\": \"default\", \"ObjectStrategy\": \"default\"}, " +
        "\"EveryDataObjectVersion\": false, " +
        "\"CreationDate\":\"2016-11-02\", " +
        "\"LastUpdate\":\"2016-11-02\", " +
        "\"ActivationDate\":\"2016-11-02\", " +
        "\"DeactivationDate\":\"2016-11-02\"}";

    public static final String PROFILES =
        "{\"_id\":\"aeaaaaaaaaaaaaabaa4ikakyetch6mqaaacq\", " +
        "\"_tenant\":0, " +
        "\"Identifier\":\"FAKE_IDENTIFIER\", " +
        "\"Name\":\"Un Profile\", " +
        "\"Description\":\"DESCRIPTION D'UN PROFILE\", " +
        "\"Status\":\"ACTIVE\", " +
        "\"Format\":\"XSD\", " +
        "\"CreationDate\":\"2016-11-02\", " +
        "\"LastUpdate\":\"2016-11-02\", " +
        "\"ActivationDate\":\"2016-11-02\", " +
        "\"DeactivationDate\":\"2016-11-02\"}";

    public static final String ARCHIVE_UNIT_PROFILES =
        "{" +
        "\"_id\":\"aeaaaaaaaaaaaaabaa4ikakyetch6mqaaacq\", " +
        "\"_tenant\":0, " +
        "\"Identifier\":\"FAKE_IDENTIFIER\", " +
        "\"Name\":\"Archive Unit Ptofile\", " +
        "\"Description\":\"Description d'un document type\", " +
        "\"Status\":\"ACTIVE\", " +
        "\"CreationDate\":\"2016-11-02\", " +
        "\"LastUpdate\":\"2016-11-02\", " +
        "\"ActivationDate\":\"2016-11-02\", " +
        "\"DeactivationDate\":\"2016-11-02\"" +
        "}";

    public static final String CONTEXTS =
        "{\"Name\": \"My_Context_1\", " +
        "\"Status\":\"ACTIVE\", " +
        "\"SecurityProfile\" : \"SEC_PROFILE-000001\"," +
        "\"Permissions\": [{" +
        "\"_tenant\": 0," +
        "\"AccessContracts\": []," +
        "\"IngestContracts\": [\"ArchivalAgreement0\"]" +
        "}]}";

    private static final String FORMAT =
        "{\"_id\":\"aeaaaaaaaaaaaaabaa44qakyetenaeyaaawq\", " +
        "\"CreatedDate\":\"2016-01-21T10:36:46\", " +
        "\"VersionPronom\":\"84\", " +
        "\"Version\":\"1.12\", " +
        "\"HasPriorityOverFileFormatID\":[], " +
        "\"MimeType\":[], " +
        "\"Name\":\"Microsoft Works Word Processor for DOS\", " +
        "\"Alert\":\"false\", " +
        "\"Extension\":[\"wps\"], " +
        "\"PUID\":\"fmt/164\", " +
        "\"_tenant\":\"0\"}";

    private static final String ACCESSION_SUMMARY =
        "{\"_id\": \"aefaaaaaaaaam7mxaa2gyakygejizayaaaaq\"," +
        "\"_tenant\": 0," +
        "\"OriginatingAgency\": \"FRAN_NP_005568\"," +
        "    \"TotalObjects\": {" +
        "    \"Total\": 12," +
        "    \"Deleted\": 0," +
        "    \"Remained\": 12" +
        "}," +
        "\"TotalObjectGroups\": {" +
        "    \"Total\": 3," +
        "    \"Deleted\": 0," +
        "    \"Remained\": 3" +
        "}," +
        "\"TotalUnits\": {" +
        "    \"Total\": 3," +
        "    \"Deleted\": 0," +
        "    \"Remained\": 3" +
        "}," +
        "\"ObjectSize\": {" +
        "    \"Total\": 1035126," +
        "    \"Deleted\": 0," +
        "    \"Remained\": 1035126" +
        "}," +
        "\"creationDate\": \"2016-11-04T20:40:49.030\"}";

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

    public static final List<OntologyModel> ONTOLOGIES = Collections.singletonList(
        new OntologyModel()
            .setId("aeaaaaaaaaaaaaabaa4ikakyetch6mqaaacq")
            .setIdentifier("_sps")
            .setId("aeaaaaaaaaaaaaabaa4ikakyetcaaaabbbcc")
            .setTenant(1)
            .setIdentifier("_sps")
            .setApiField("#originating_agencies")
            .setSedaField("OriginatingAgencyIdentifier")
            .setDescription("Internal ontology sample")
            .setOrigin(OntologyOrigin.INTERNAL)
            .setShortName("Originating Agency Identifier")
            .setCollections(Arrays.asList("Unit", "ObjectGroup"))
            .setType(OntologyType.KEYWORD)
    );

    private static List<OntologyModel> ontologies = ONTOLOGIES;

    private static final String DIP_RESULTS = "{}";

    private ClientMockResultHelper() {}

    /**
     * @return a default Logbook Result
     * @throws InvalidParseOperationException
     */
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
     * @return a default Logbook response Result
     * @throws InvalidParseOperationException
     */
    public static RequestResponse getLogbooksRequestResponse() throws InvalidParseOperationException {
        return RequestResponseOK.getFromJsonNode(getLogbookResults());
    }

    /**
     * @return one default Logbook response
     * @throws InvalidParseOperationException
     */
    public static RequestResponse getLogbookRequestResponse() throws InvalidParseOperationException {
        return RequestResponseOK.getFromJsonNode(getLogbookOperation());
    }

    /**
     * @return one default Logbook response
     * @throws InvalidParseOperationException
     */
    public static RequestResponse getLogbookRequestResponseWithObId() throws InvalidParseOperationException {
        return RequestResponseOK.getFromJsonNode(getLogbookOperationWithObId());
    }

    /**
     * @return a default Logbook Operation
     * @throws InvalidParseOperationException
     */
    public static JsonNode getLogbookOperation() throws InvalidParseOperationException {
        return JsonHandler.getFromString(
            RESULT + "[{\"_id\": \"aeaqaaaaaefex4j4aao2qalmjv7h24yaaaaq\"," + LOGBOOK_OPERATION + "]}"
        );
    }

    /**
     * @return a default Logbook Operation
     * @throws InvalidParseOperationException
     */
    public static JsonNode getLogbookLifecycle() throws InvalidParseOperationException {
        return JsonHandler.getFromString(
            RESULT + "[{\"_id\": \"aeaqaaaaaefex4j4aao2qalmjv7h24yaaaaq\"," + LOGBOOK_LIFECYCLE + "]}"
        );
    }

    public static RequestResponse<JsonNode> getMetaDataResult() throws InvalidParseOperationException {
        return getArchiveUnitResult();
    }

    public static RequestResponse<JsonNode> getArchiveUnitRawResult() throws InvalidParseOperationException {
        return createResponse(UNIT_RAW);
    }

    public static RequestResponse<JsonNode> getObjectGroupRawResult() throws InvalidParseOperationException {
        return createResponse(GOT_RAW);
    }

    /**
     * @return a default Logbook Operation
     * @throws InvalidParseOperationException
     */
    public static JsonNode getLogbookOperationWithObId() throws InvalidParseOperationException {
        return JsonHandler.getFromString(
            RESULT + "[{\"_id\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaa1\"," + LOGBOOK_OPERATION_WITH_OBID + "]}"
        );
    }

    /**
     * @param response the original object to be included in response
     * @return a default response
     */
    public static <T> RequestResponse<T> createResponse(T response) {
        return new RequestResponseOK<T>().addResult(response);
    }

    public static RequestResponse createEmptyReponse() {
        return new RequestResponseOK();
    }

    public static RequestResponse createVitamError() {
        return new VitamError("Fake");
    }

    /**
     * @param s the original object to be included in response
     * @return a default response
     * @throws InvalidParseOperationException
     */
    public static RequestResponse createResponse(String s) throws InvalidParseOperationException {
        RequestResponseOK responseOK = new RequestResponseOK();
        if (null != s) responseOK.addResult(JsonHandler.getFromString(s));
        return responseOK.setHttpCode(Status.OK.getStatusCode());
    }

    /**
     * @param <T>
     * @param s the original object to be included in response
     * @return a default response
     * @throws InvalidParseOperationException
     */
    public static <T> RequestResponse<T> createResponse(String s, Class<T> clasz)
        throws InvalidParseOperationException {
        RequestResponseOK responseOK = new RequestResponseOK();
        if (null != s) responseOK.addResult(JsonHandler.getFromString(s, clasz));
        return responseOK.setHttpCode(Status.OK.getStatusCode());
    }

    /**
     * @param <T>
     * @param entries the original object to be included in response
     * @return a default response
     * @throws InvalidParseOperationException
     */
    public static <T> RequestResponse<T> createResponse(List<T> entries) throws InvalidParseOperationException {
        RequestResponseOK responseOK = new RequestResponseOK();
        responseOK.addAllResults(entries);
        return responseOK.setHttpCode(Status.OK.getStatusCode());
    }

    /**
     * @param s the original object to be included in response
     * @param statuscode status to be returned
     * @return a default response
     * @throws InvalidParseOperationException
     */
    public static RequestResponse createResponse(String s, int statuscode, Class clazz)
        throws InvalidParseOperationException {
        RequestResponseOK responseOK = new RequestResponseOK();
        if (null != s) responseOK.addResult(JsonHandler.getFromString(s, clazz));
        return responseOK.setHttpCode(statuscode);
    }

    /**
     * @return a default Access Register Summary
     * @throws InvalidParseOperationException
     */
    public static RequestResponse getAccessionRegisterSummary() throws InvalidParseOperationException {
        return createResponse(ACCESSION_SUMMARY);
    }

    /**
     * @return a default Access Register Detail
     * @throws InvalidParseOperationException
     */
    public static RequestResponse getAccessionRegisterDetail() throws InvalidParseOperationException {
        return createResponse(ACCESSION_DETAIL);
    }

    /**
     * @return a default Format
     * @throws InvalidParseOperationException
     */
    public static RequestResponse getFormat() throws InvalidParseOperationException {
        return createResponse(FORMAT);
    }

    /**
     * @return a default Rule
     * @throws InvalidParseOperationException
     */
    public static RequestResponse getRule() throws InvalidParseOperationException {
        return createResponse(RULE);
    }

    /**
     * @return a default agency
     * @throws InvalidParseOperationException
     */
    public static RequestResponse getAgency() throws InvalidParseOperationException {
        return createResponse(AGENCY);
    }

    /**
     * @return a RequestResponse containing contracts json
     * @throws InvalidParseOperationException
     */
    public static RequestResponse<IngestContractModel> getIngestContracts() throws InvalidParseOperationException {
        return createResponse(INGEST_CONTRACTS, IngestContractModel.class);
    }

    /**
     * @return a RequestResponse containing access contracts json
     * @throws InvalidParseOperationException
     */
    public static RequestResponse getAccessContracts() throws InvalidParseOperationException {
        return createResponse(ACCESS_CONTRACTS);
    }

    /**
     * @return a RequestResponse containing management contracts json
     * @throws InvalidParseOperationException
     */
    public static RequestResponse getManagementContracts() throws InvalidParseOperationException {
        return createResponse(MANAGEMENT_CONTRACTS);
    }

    /**
     * @return context json
     * @throws InvalidParseOperationException
     */
    public static RequestResponse getContexts(int statusCode) throws InvalidParseOperationException {
        return createResponse(CONTEXTS, statusCode, ContextModel.class);
    }

    public static RequestResponse getProfiles(int statusCode) throws InvalidParseOperationException {
        return createResponse(PROFILES, statusCode, ObjectNode.class);
    }

    /**
     * @return a default list of Formats
     * @throws InvalidParseOperationException
     */
    public static RequestResponse getFormatList() throws InvalidParseOperationException {
        return createResponse(FORMAT);
    }

    /**
     * @return a default list of Formats
     * @throws InvalidParseOperationException
     */
    public static RequestResponse getAgenciesList() throws InvalidParseOperationException {
        return createResponse(AGENCY);
    }

    /**
     * @return a default list of Rules
     * @throws InvalidParseOperationException
     */
    public static RequestResponse getRuleList(String ruleId) throws InvalidParseOperationException {
        ObjectNode rule = (ObjectNode) JsonHandler.getFromString(RULE);
        String oldRuleId = rule.get("RuleId").asText();
        rule.put("RuleId", ruleId);
        switch (ruleId.substring(0, 3)) {
            case "STO":
                rule.put("RuleType", "StorageRule");
                break;
            case "CLA":
                rule.put("RuleType", "ClassificationRule");
                break;
            case "ACC":
                rule.put("RuleType", "AccessRule");
                break;
            case "APP":
                rule.put("RuleType", "AppraisalRule");
                break;
            case "DIS":
                rule.put("RuleType", "DisseminationRule");
                break;
            case "REU":
                rule.put("RuleType", "ReuseRule");
                break;
            default:
                rule.put("RuleId", oldRuleId);
        }

        return createResponse(rule);
    }

    public static RequestResponse getRuleList() throws InvalidParseOperationException {
        ObjectNode rule = (ObjectNode) JsonHandler.getFromString(RULE);
        return createResponse(rule);
    }

    /**
     * @return a default list of Rules
     * @throws InvalidParseOperationException
     */
    public static RequestResponse getEmptyResult() throws InvalidParseOperationException {
        return createResponse(RESULT + "{}}");
    }

    /**
     * @return a default ArchiveUnit result
     * @throws InvalidParseOperationException
     */
    public static RequestResponse getArchiveUnitResult() throws InvalidParseOperationException {
        return createResponse(UNIT);
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
            StreamUtils.toInputStream("test"),
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
     * @return a default operation traceability
     * @throws InvalidParseOperationException
     */
    public static RequestResponse getOperationTraceabilityDetails() throws InvalidParseOperationException {
        return createResponse(TRACEABILITY_OPERATION);
    }

    /**
     * @throws InvalidParseOperationException
     */
    public static RequestResponse checkOperationTraceability() throws InvalidParseOperationException {
        return createResponse(TRACEABILITY_OPERATION);
    }

    /**
     * @throws InvalidParseOperationException
     */
    public static RequestResponse getTraceabilityOperationMock() throws InvalidParseOperationException {
        return createResponse(TRACEABILITY_LOGBOOK_OPERATION);
    }

    public static RequestResponse getSecurityProfiles() throws InvalidParseOperationException {
        return createResponse(SECURITY_PROFILES);
    }

    public static RequestResponse getDipInfo() throws InvalidParseOperationException {
        return createResponse(DIP_RESULTS);
    }

    /**
     * method to get mocked archive unit profiles with the givel status code
     *
     * @param statusCode
     * @return a mock of response for ArchiveUnitProfiles
     * @throws InvalidParseOperationException
     */
    public static RequestResponse getArchiveUnitProfiles(int statusCode) throws InvalidParseOperationException {
        return createResponse(ARCHIVE_UNIT_PROFILES);
    }

    /**
     * method to get mocked ontologies with the given status code
     *
     * @param statusCode
     * @return a mock of response for Ontologies
     * @throws InvalidParseOperationException
     */
    public static RequestResponse getOntologies(int statusCode) throws InvalidParseOperationException {
        return createResponse(ontologies);
    }

    public static void setOntologies(List<OntologyModel> ontologies) {
        // We really need to delete mock clients from production...
        ClientMockResultHelper.ontologies = ontologies;
    }

    public static void resetOntologies() {
        // We really need to delete mock clients from production...
        ClientMockResultHelper.ontologies = ONTOLOGIES;
    }
}

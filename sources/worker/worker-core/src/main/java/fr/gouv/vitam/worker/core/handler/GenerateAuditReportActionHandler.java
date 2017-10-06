package fr.gouv.vitam.worker.core.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.CharsetUtils;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.AccessUnauthorizedException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessionRegisterSummaryModel;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterSummary;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.StorageCollectionType;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.CheckExistenceObjectPlugin;
import fr.gouv.vitam.worker.core.plugin.CheckIntegrityObjectPlugin;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;

public class GenerateAuditReportActionHandler extends ActionHandler {

    private static final String ID_GOT = "IdGOT";

    private static final String EV_TYPE = "evType";

    private static final String OUT_DETAIL = "OutDetail";

    private static final String USAGE = "Usage";

    private static final String _TENANT = "_tenant";

    private static final String AUDIT = "AUDIT";

    private static final String OUTCOME = "outcome";

    private static final String ID_OBJ = "IdObj";

    private static final String ORIGINATING_AGENCY = "OriginatingAgency";

    private static final String EVENT = "events";

    private static final String EV_TYPE_PROC = "evTypeProc";

    private static final String AUDIT_KO = "auditKO";

    private static final String AUDIT_WARNING = "auditWarning";

    private static final String OBJECT_ID = "objectId";

    private static final String SOURCE = "source";

    private static final String AUDIT_TYPE = "auditType";

    private static final String AUDIT_OPERATION_ID = "auditOperationId";

    private static final String TENANT = "tenant";

    private static final String STATUS = "Status";

    private static final String OUT_MESSAGE = "outMessage";

    private static final String EV_ID_PROC = "evIdProc";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(GenerateAuditReportActionHandler.class);

    private static final String HANDLER_ID = "REPORT_AUDIT";
    private static final String JSON = ".json";
    private static final String FOLDERNAME = "REPORT/";
    private static final String DEFAULT_STRATEGY = "default";

    private String auditType;
    private ObjectNode report = JsonHandler.createObjectNode();
    private Map<String, Integer> serviceProducteur = new HashMap<String, Integer>();
    private Map<String, Integer> serviceProducteurKO = new HashMap<String, Integer>();
    private ArrayNode serviceProducteurWarning = JsonHandler.createArrayNode();

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler)
        throws ProcessingException, ContentAddressableStorageServerException {

        checkMandatoryParameters(param);

        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);

        Map<WorkerParameterName, String> mapParameters = param.getMapParameters();
        auditType = mapParameters.get(WorkerParameterName.auditType);

        String actions = mapParameters.get(WorkerParameterName.auditActions);
        List<String> auditActions = Arrays.asList(actions.split("\\s*,\\s*"));

        if (auditType.toLowerCase().equals(TENANT)) {
            auditType = BuilderToken.PROJECTIONARGS.TENANT.exactToken();
        } else if (auditType.toLowerCase().equals("originatingagency")) {
            auditType = BuilderToken.PROJECTIONARGS.ORIGINATING_AGENCY.exactToken();
        }

        List<String> originatingAgency = new ArrayList<>();

        try (LogbookOperationsClient jopClient = LogbookOperationsClientFactory.getInstance().getClient();
            LogbookLifeCyclesClient lfcClient = LogbookLifeCyclesClientFactory.getInstance().getClient()) {

            String objectId = mapParameters.get(WorkerParameterName.objectId);
            String auditTypeString = "";

            if (auditType.equals(BuilderToken.PROJECTIONARGS.TENANT.exactToken())) {
                auditTypeString = "tenant";
                originatingAgency = listOriginatingAgency(null);
            } else if (auditType.equals(BuilderToken.PROJECTIONARGS.ORIGINATING_AGENCY.exactToken())){
                originatingAgency.add(objectId);
                auditTypeString = "originatingagency";
            }

            String[] arrayOriginatingAgency = new String[originatingAgency.size()];
            originatingAgency.toArray(arrayOriginatingAgency);

            report.put(TENANT, VitamThreadUtils.getVitamSession().getTenantId());
            report.put(AUDIT_OPERATION_ID, param.getContainerName());
            report.put(AUDIT_TYPE, auditTypeString);
            report.put(OBJECT_ID, objectId);
            getStatusAndDateTime(report, jopClient, param.getContainerName(), auditActions);
            report.set(SOURCE, createSource(jopClient, arrayOriginatingAgency));
            report.set(AUDIT_KO, createReportKOPart(lfcClient, param.getContainerName()));
            report.set(AUDIT_WARNING, serviceProducteurWarning);

            String reportString = report.toString();
            InputStream auditReportFile = new ByteArrayInputStream(reportString.getBytes(CharsetUtils.UTF_8));
            storeAuditReport(param.getContainerName(), auditReportFile);
        } catch (Exception e) {
            LOGGER.error("UnKnow error ", e);
            itemStatus.increment(StatusCode.FATAL);
            return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
        }

        itemStatus.setMasterData(LogbookParameterName.eventDetailData.name(), createResultDetailByOriginatingAgency());
        itemStatus.increment(StatusCode.OK);
        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // TODO Auto-generated method stub

    }

    private void getStatusAndDateTime(ObjectNode report, LogbookOperationsClient jopClient, String auditOperationId,
        List<String> auditActions)
        throws InvalidCreateOperationException, LogbookClientException, InvalidParseOperationException,
        UnsupportedEncodingException {
        Select select = new Select();
        select.setQuery(QueryHelper.and().add(QueryHelper.eq(EV_ID_PROC, auditOperationId),
            QueryHelper.eq(EV_TYPE_PROC, AUDIT)));

        JsonNode result = jopClient.selectOperation(select.getFinalSelect());
        JsonNode res = result.get(RequestResponseOK.TAG_RESULTS).get(0);
        report.put("DateTime", res.get("evDateTime").textValue());

        ArrayNode events = (ArrayNode) res.get(EVENT);
        JsonNode event = events.get(0);
        report.put(STATUS, event.get(OUTCOME).textValue());
        report.put(OUT_MESSAGE, event.get("outMessg").textValue());
        if (auditActions.contains(CheckIntegrityObjectPlugin.getId())) {
            report.put("LastEvent", CheckIntegrityObjectPlugin.getId());
        } else {
            report.put("LastEvent", CheckExistenceObjectPlugin.getId());
        }

    }

    private ArrayNode createSource(LogbookOperationsClient jopClient, String[] originatingAgency)
        throws InvalidCreateOperationException, LogbookClientException, InvalidParseOperationException {
        ArrayNode source = JsonHandler.createArrayNode();
        Select selectQuery = new Select();
        selectQuery.setQuery(
            and().add(
                QueryHelper.or().add(
                    QueryHelper.eq("events.outDetail", "PROCESS_SIP_UNITARY.OK"),
                    QueryHelper.eq("events.outDetail", "PROCESS_SIP_UNITARY.WARNING")),
                QueryHelper.in("events.agIdExt.originatingAgency", originatingAgency)));

        try {
            JsonNode result = jopClient.selectOperation(selectQuery.getFinalSelect());
            for (JsonNode res : result.get(RequestResponseOK.TAG_RESULTS)) {
                if (res.get("agIdExt") != null) {
                    final String agIdExt = res.get("agIdExt").asText();
                    final JsonNode agIdExtNode = JsonHandler.getFromString(agIdExt);
                    source.add(JsonHandler.createObjectNode().put(_TENANT, res.get(_TENANT).asText())
                        .put(ORIGINATING_AGENCY, agIdExtNode.get("originatingAgency").asText())
                        .put(EV_ID_PROC, res.get(EV_ID_PROC).asText()));
                }
            }
        } catch (LogbookClientNotFoundException e) {
            LOGGER.error("Logbook error, can not create source ", e);
            return source;
        }

        return source;
    }

    /**
     * @param lfcClient
     * @return
     * @throws InvalidCreateOperationException
     * @throws LogbookClientException
     * @throws InvalidParseOperationException
     */
    private ArrayNode createReportKOPart(LogbookLifeCyclesClient lfcClient, String auditOperationId)
        throws InvalidCreateOperationException, LogbookClientException, InvalidParseOperationException {
        ArrayNode reportKO = JsonHandler.createArrayNode();
        Select selectQuery = new Select();
        selectQuery.setQuery(QueryHelper.and()
            .add(QueryHelper.eq("events.evTypeProc", AUDIT), QueryHelper.eq("events.outcome", "KO")));
        try {
            JsonNode result = lfcClient.selectObjectGroupLifeCycle(selectQuery.getFinalSelect());

            for (JsonNode res : result.get(RequestResponseOK.TAG_RESULTS)) {
                JsonNode events = res.get(EVENT);
                for (JsonNode event : events) {
                    if (event.get(EV_TYPE).asText().equals("LFC.AUDIT_CHECK_OBJECT") &&
                        event.get(OUTCOME).asText().equals("KO") &&
                        event.get(EV_ID_PROC).asText().equals(auditOperationId)) {
                        JsonNode evDetData = JsonHandler.getFromString(event.get("evDetData").asText());
                        final String originatingAgency = evDetData.get(ORIGINATING_AGENCY).asText();
                        for (JsonNode error : evDetData.get("errors")) {
                            reportKO.add(JsonHandler.createObjectNode().put("IdOp", event.get(EV_ID_PROC).asText())
                                .put(ID_GOT, event.get("obId").asText())
                                .put(ID_OBJ, error.get(ID_OBJ).asText())
                                .put(USAGE, error.get(USAGE).asText())
                                .put(ORIGINATING_AGENCY, originatingAgency)
                                .put(OUT_DETAIL, event.get("outDetail").asText()));
                        }

                        int nb = evDetData.get("nbKO").asInt();
                        if (serviceProducteurKO.containsKey(originatingAgency)) {
                            int nbKoTotal = serviceProducteurKO.get(originatingAgency) + nb;
                            serviceProducteurKO.put(originatingAgency, nbKoTotal);
                        } else {
                            serviceProducteurKO.put(originatingAgency, nb);
                        }
                        break;
                    }
                }
            }
        } catch (LogbookClientNotFoundException e) {
            // no Audit KO
            return reportKO;
        }
        return reportKO;
    }

    private List<String> listOriginatingAgency(String id)
        throws AccessUnauthorizedException, InvalidParseOperationException, ReferentialException,
        InvalidCreateOperationException {
        List<String> originatingAgency = new ArrayList<String>();

        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            Select selectQuery = new Select();
            if (id != null) {
                selectQuery.setQuery(QueryHelper.eq(ORIGINATING_AGENCY, id));
            }
            RequestResponse<AccessionRegisterSummaryModel> results =
                client.getAccessionRegister(selectQuery.getFinalSelect());

            JsonNode searchResults = results.toJsonNode().get(RequestResponseOK.TAG_RESULTS);
            if (searchResults.isArray()) {
                for (JsonNode og : searchResults) {
                    String registerName = og.get(ORIGINATING_AGENCY).asText();
                    final int total = og.get(AccessionRegisterSummary.TOTAL_OBJECTS)
                        .get(AccessionRegisterSummary.INGESTED).intValue();
                    originatingAgency.add(registerName);
                    serviceProducteur.put(registerName,
                        total);
                    if (total == 0) {
                        serviceProducteurWarning.add(registerName);
                    }
                }
            }
        }

        return originatingAgency;
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }

    public void storeAuditReport(String guid, InputStream report)
        throws InvalidParseOperationException, ContentAddressableStorageServerException,
        StorageAlreadyExistsClientException,
        StorageNotFoundClientException, StorageServerClientException, ContentAddressableStorageNotFoundException {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient();
            WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient()) {
            SanityChecker.checkParameter(guid);
            try {
                workspaceClient.createContainer(guid);
            } catch (ContentAddressableStorageAlreadyExistException e) {
                LOGGER.debug(e);
            }
            workspaceClient.putObject(guid, FOLDERNAME + guid + JSON, report);

            final ObjectDescription description = new ObjectDescription();
            description.setWorkspaceContainerGUID(guid);
            description.setWorkspaceObjectURI(FOLDERNAME + guid + JSON);

            storageClient.storeFileFromWorkspace(DEFAULT_STRATEGY,
                StorageCollectionType.REPORTS, guid + JSON, description);
            workspaceClient.deleteContainer(guid, true);

        }
    }

    private String createResultDetailByOriginatingAgency() {
        ObjectNode result = JsonHandler.createObjectNode();
        for (Entry<String, Integer> sp : serviceProducteur.entrySet()) {
            int nbKO = serviceProducteurKO.containsKey(sp.getKey()) ? serviceProducteurKO.get(sp.getKey()) : 0;
            result.set(sp.getKey(),
                JsonHandler.createObjectNode().put("OK", sp.getValue() - nbKO)
                    .put("KO", nbKO));
        }
        return JsonHandler.unprettyPrint(result);
    }

}

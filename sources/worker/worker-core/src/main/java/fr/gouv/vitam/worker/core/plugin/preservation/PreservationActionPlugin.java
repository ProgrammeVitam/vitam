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

package fr.gouv.vitam.worker.core.plugin.preservation;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.batch.report.model.PreservationReportModel;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.stream.VitamAsyncInputStream;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineIterator;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.preservation.service.PreservationReportService;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.io.FileUtils;

import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.LocalDateUtil.now;
import static fr.gouv.vitam.common.accesslog.AccessLogUtils.getNoLogAccessLog;
import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.OBJECT;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.concurrent.TimeUnit.MINUTES;

public class PreservationActionPlugin extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PreservationActionPlugin.class);

    public static final String DEFAULT_STORAGE_STRATEGY = "default";
    public static final String DISTRIBUTION_FILE = "distributionFile.jsonl";

    private static final String PRESERVATION_ACTION = "PRESERVATION_ACTION";
    private static final String TIMEOUT = "timeout";
    private static final String FORMAT_ID = "formatId";
    private static final String INPUT_FILES = "input-files";
    private static final String DEBUG = "debug";
    private static final String ACTION = "actions";
    private static final String PARAMETERS_JSON = "parameters.json";
    private static final String RESULT_JSON = "result.json";
    private static final String UNIT_ID = "unitId";
    private static final String OBJECT_ID = "objectId";
    private static final String GRIFFIN_ID = "griffinId";
    private static final String EXECUTABLE_FILE_NAME = "griffin";

    private final String griffinInputFolder;
    private final String execFolder;

    private final WorkspaceClientFactory workspaceClientFactory;
    private final StorageClientFactory storageClientFactory;
    private final PreservationReportService preservationReportService;

    public PreservationActionPlugin() {
        this(WorkspaceClientFactory.getInstance(),
            StorageClientFactory.getInstance(),
            new PreservationReportService(),
            VitamConfiguration.getVitamGriffinInputFilesFolder(),
            VitamConfiguration.getVitamGriffinExecFolder()
        );
    }

    @VisibleForTesting
    public PreservationActionPlugin(WorkspaceClientFactory workspace, StorageClientFactory storage,
        PreservationReportService report, String inputFolder, String execFolder) {
        this.workspaceClientFactory = workspace;
        this.storageClientFactory = storage;
        this.preservationReportService = report;
        this.griffinInputFolder = inputFolder;
        this.execFolder = execFolder;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        List<JsonLineModel> entries;
        try {
            entries = loadEntriesToPreserve(param.getContainerName());
        } catch (ContentAddressableStorageException e) {
            LOGGER.error(String.format("Preservation action failed with status [%s]", FATAL), e);
            return buildItemStatus(PRESERVATION_ACTION, FATAL, eventDetails(e));
        }
        String griffinId = entries.get(0).getParams().get(GRIFFIN_ID).asText();
        String batchId = generateBatchId();

        try {
            Path batchDirectory = createBatchDirectory(griffinId, batchId);

            copyInputFiles(batchDirectory, entries);
            createParametersBatchFile(entries, batchDirectory, param.getRequestId(), batchId);

            int timeout = entries.get(0).getParams().get(TIMEOUT).asInt();
            ResultPreservation result = launchGriffin(griffinId, batchDirectory, timeout);

            createReport(result, entries, VitamThreadUtils.getVitamSession().getTenantId());
            mapResultAction(result);
        } catch (Exception e) {
            LOGGER.error(String.format("Preservation action failed with status [%s]", KO), e);
            return buildItemStatus(PRESERVATION_ACTION, KO, eventDetails(e));
        } finally {
            deleteLocalFiles(griffinId, batchId);
        }
        return buildItemStatus(PRESERVATION_ACTION, OK, null);
    }

    private void deleteLocalFiles(String griffinId, String batchId) throws ProcessingException {
        try {
            FileUtils.deleteDirectory(Paths.get(griffinInputFolder, griffinId, batchId).toFile());
        } catch (IOException e) {
            throw new ProcessingException("Something bad happens", e);
        }
    }

    private ObjectNode eventDetails(Throwable e) {
        return JsonHandler.createObjectNode().put("error", e.getMessage());
    }

    private Path createBatchDirectory(String griffinId, String batchId) throws Exception {
        Path griffinDirectory = Paths.get(griffinInputFolder, griffinId);
        if (Files.notExists(griffinDirectory)) {
            Files.createDirectory(griffinDirectory);
        }
        return Files.createDirectory(griffinDirectory.resolve(batchId));
    }

    private void copyInputFiles(Path batchDirectory, List<JsonLineModel> jsonLineModels) throws Exception {
        try (StorageClient storageClient = storageClientFactory.getClient()) {
            Path inputFilesDirectory = Files.createDirectory(batchDirectory.resolve(INPUT_FILES));
            for (JsonLineModel jsonLineModel : jsonLineModels) {
                copyBinaryFile(jsonLineModel, storageClient, inputFilesDirectory);
            }
        }
    }

    private List<JsonLineModel> loadEntriesToPreserve(String processId) throws ContentAddressableStorageException {
        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            Response reportResponse = workspaceClient.getObject(processId, DISTRIBUTION_FILE);

            JsonLineIterator jsonLineIterator = new JsonLineIterator(new VitamAsyncInputStream(reportResponse));
            ArrayList<JsonLineModel> jsonLineModels = new ArrayList<>();
            jsonLineIterator.forEachRemaining(jsonLineModels::add);

            return jsonLineModels;
        }
    }

    private void copyBinaryFile(JsonLineModel jsonLineModel, StorageClient storageClient, Path inputFilesDirectory) throws Exception {
        final Response response = storageClient.getContainerAsync(DEFAULT_STORAGE_STRATEGY, jsonLineModel.getId(), OBJECT, getNoLogAccessLog());
        try (InputStream src = response.readEntity(InputStream.class)) {
            Path target = inputFilesDirectory.resolve(jsonLineModel.getId());
            Files.copy(src, target, REPLACE_EXISTING);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private void mapResultAction(ResultPreservation resultPreservation) {
        resultPreservation.getOutputs()
            .forEach(((inputName, outputPreservations) -> outputPreservations
                .forEach(outputPreservation -> {
                    switch (outputPreservation.getAction()) {
                        case IDENTIFY:
                        case ANALYSE:
                            LOGGER.info("{} {}", inputName, outputPreservation);
                            break;
                        case EXTRACT:
                        case GENERATE:
                            break;
                        default:
                            throw new RuntimeException("action must be of type: IDENTIFY | EXTRACT | ANALYSE | GENERATE");
                    }
                })));
    }

    private void createReport(ResultPreservation resultPreservation, List<JsonLineModel> request, int tenant)
        throws VitamClientInternalException {
        List<PreservationReportModel> reportModels = toReportModel(resultPreservation, request, tenant, now());
        preservationReportService.appendPreservationEntries(resultPreservation.getRequestId(), reportModels);
    }

    private List<PreservationReportModel> toReportModel(ResultPreservation outputs, List<JsonLineModel> requests,
        int tenant, LocalDateTime now) {
        return outputs.getOutputs().entrySet()
            .stream()
            .flatMap(entry -> entry.getValue()
                .stream()
                .map(value -> getPreservationReportModel(outputs, tenant, now, value, requests))
            ).collect(Collectors.toList());
    }

    private PreservationReportModel getPreservationReportModel(ResultPreservation outputs, int tenant,
        LocalDateTime now, OutputPreservation value,
        List<JsonLineModel> requests) {
        JsonLineModel model =
            IterableUtils.find(requests, j -> j.getId().equals(value.getInputPreservation().getName()));

        return new PreservationReportModel(
            outputs.getRequestId(),
            outputs.getId(),
            tenant,
            now.toString(),
            value.getStatus(),
            model.getParams().get(UNIT_ID).asText(),
            model.getParams().get(OBJECT_ID).asText(),
            value.getAction().toString(),
            value.getAnalyseResult().toString(),
            value.getInputPreservation().getName(),
            value.getOutputName()
        );
    }

    private String generateBatchId() {
        return GUIDFactory.newGUID()
            .getId();
    }

    private void createParametersBatchFile(List<JsonLineModel> jsonLineModels, Path batchDirectory, String requestId,
        String batchId) throws Exception {
        List<InputPreservation> inputPreservations = jsonLineModels.stream()
            .map(this::mapToInput)
            .collect(Collectors.toList());
        List<ActionPreservation> actionPreservations = jsonLineModels.stream().map(this::mapToActions).collect(Collectors.toList());

        boolean debug = jsonLineModels.get(0).getParams().get(DEBUG).asBoolean();
        ParametersPreservation parametersPreservation = new ParametersPreservation(requestId, batchId, inputPreservations, actionPreservations, debug);
        Path parametersPath = batchDirectory.resolve(PARAMETERS_JSON);
        JsonHandler.writeAsFile(parametersPreservation, parametersPath.toFile());
    }


    private ActionPreservation mapToActions(JsonLineModel jsonLineModel) throws RuntimeException {
        try {
            return JsonHandler
                .getFromString(jsonLineModel.getParams().get(ACTION).toString(), ActionPreservation.class);
        } catch (InvalidParseOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private InputPreservation mapToInput(JsonLineModel jsonLineModel) {
        return new InputPreservation(jsonLineModel.getId(), jsonLineModel.getParams().get(FORMAT_ID).asText());
    }

    private ResultPreservation launchGriffin(String griffinId, Path batchDirectory, int timeout) throws Exception {
        Path griffinExecutable = Paths.get(execFolder, griffinId, EXECUTABLE_FILE_NAME);

        List<String> command = Arrays.asList(griffinExecutable.toString(), batchDirectory.toString());
        ProcessBuilder processBuilder = new ProcessBuilder(command);

        Process griffin = processBuilder.start();
        griffin.waitFor(timeout, MINUTES);

        if (griffin.exitValue() > 0) {
            LOGGER.error(
                "Griffin {} exited with value {}, stdErr: {}, stdOut: {}.",
                griffinId,
                griffin.exitValue(),
                stdToString(griffin.getErrorStream()),
                stdToString(griffin.getInputStream())
            );
        }

        return JsonHandler.getFromFile(batchDirectory.resolve(RESULT_JSON).toFile(), ResultPreservation.class);
    }

    private static String stdToString(InputStream std) throws IOException {
        StringBuilder textBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(std, UTF_8))) {
            String c;
            while ((c = reader.readLine()) != null) {
                textBuilder.append(c);
            }
        }
        return textBuilder.toString();
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // NOP.
    }
}

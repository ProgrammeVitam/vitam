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
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.common.model.administration.ActionPreservation;
import fr.gouv.vitam.worker.core.plugin.preservation.model.InputPreservation;
import fr.gouv.vitam.worker.core.plugin.preservation.model.OutputPreservation;
import fr.gouv.vitam.worker.core.plugin.preservation.model.ParametersPreservation;
import fr.gouv.vitam.worker.core.plugin.preservation.model.PreservationDistributionLine;
import fr.gouv.vitam.worker.core.plugin.preservation.model.ResultPreservation;
import fr.gouv.vitam.worker.core.plugin.preservation.service.PreservationReportService;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static fr.gouv.vitam.common.LocalDateUtil.now;
import static fr.gouv.vitam.common.accesslog.AccessLogUtils.getNoLogAccessLog;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.OBJECT;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildBulkItemStatus;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.concurrent.TimeUnit.MINUTES;

public class PreservationActionPlugin extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PreservationActionPlugin.class);

    public static final String DEFAULT_STORAGE_STRATEGY = "default";

    private static final String PRESERVATION_ACTION = "PRESERVATION_ACTION";
    private static final String INPUT_FILES = "input-files";
    private static final String PARAMETERS_JSON = "parameters.json";
    private static final String RESULT_JSON = "result.json";
    private static final String EXECUTABLE_FILE_NAME = "griffin";

    private final String griffinInputFolder;
    private final String execFolder;

    private final StorageClientFactory storageClientFactory;
    private final PreservationReportService preservationReportService;

    public PreservationActionPlugin() {
        this(StorageClientFactory.getInstance(),
            new PreservationReportService(),
            VitamConfiguration.getVitamGriffinInputFilesFolder(),
            VitamConfiguration.getVitamGriffinExecFolder()
        );
    }

    @VisibleForTesting
    public PreservationActionPlugin(StorageClientFactory storage,
        PreservationReportService report, String inputFolder, String execFolder) {
        this.storageClientFactory = storage;
        this.preservationReportService = report;
        this.griffinInputFolder = inputFolder;
        this.execFolder = execFolder;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        throw new ProcessingException("No need to implements method");
    }

    @Override
    public List<ItemStatus> executeList(WorkerParameters workerParameters, HandlerIO handler)
        throws ProcessingException, ContentAddressableStorageServerException {
        List<PreservationDistributionLine> entries =
            IntStream.range(0, workerParameters.getObjectNameList().size())
                .mapToObj(index -> mapToParamsPreservationDistributionFile(workerParameters, index))
                .collect(Collectors.toList());

        String griffinId = entries.get(0).getGriffinId();

        String batchId = generateBatchId();

        try {
            Path batchDirectory = createBatchDirectory(griffinId, batchId);

            copyInputFiles(batchDirectory, entries);
            createParametersBatchFile(entries, batchDirectory, workerParameters.getRequestId(), batchId);

            int timeout = entries.get(0).getTimeout();
            ResultPreservation result = launchGriffin(griffinId, batchDirectory, timeout);

            createReport(result, entries, VitamThreadUtils.getVitamSession().getTenantId());
            mapResultAction(result);
        } catch (Exception e) {
            LOGGER.error(String.format("Preservation action failed with status [%s]", KO), e);
            return buildBulkItemStatus(workerParameters, PRESERVATION_ACTION, KO);
        } finally {
            deleteLocalFiles(griffinId, batchId);
        }
        return buildBulkItemStatus(workerParameters, PRESERVATION_ACTION, OK);
    }

    private PreservationDistributionLine mapToParamsPreservationDistributionFile(
        WorkerParameters workerParameters, int index) {
        PreservationDistributionLine preservationDistributionLine;
        try {
            preservationDistributionLine = JsonHandler
                .getFromJsonNode(workerParameters.getObjectMetadataList().get(index),
                    PreservationDistributionLine.class);
            preservationDistributionLine.setId(workerParameters.getObjectNameList().get(index));
        } catch (InvalidParseOperationException e) {
            throw new RuntimeException(e);
        }
        return preservationDistributionLine;
    }

    private void deleteLocalFiles(String griffinId, String batchId) throws ProcessingException {
        try {
            FileUtils.deleteDirectory(Paths.get(griffinInputFolder, griffinId, batchId).toFile());
        } catch (IOException e) {
            throw new ProcessingException("Something bad happens", e);
        }
    }

    private Path createBatchDirectory(String griffinId, String batchId) throws Exception {
        Path griffinDirectory = Paths.get(griffinInputFolder, griffinId);
        if (Files.notExists(griffinDirectory)) {
            Files.createDirectory(griffinDirectory);
        }
        return Files.createDirectory(griffinDirectory.resolve(batchId));
    }

    private void copyInputFiles(Path batchDirectory, List<PreservationDistributionLine> entries)
        throws Exception {
        try (StorageClient storageClient = storageClientFactory.getClient()) {
            Path inputFilesDirectory = Files.createDirectory(batchDirectory.resolve(INPUT_FILES));
            for (PreservationDistributionLine entryParams : entries) {
                copyBinaryFile(entryParams, storageClient, inputFilesDirectory);
            }
        }
    }

    private void copyBinaryFile(PreservationDistributionLine entryParams, StorageClient storageClient,
        Path inputFilesDirectory)
        throws Exception {
        final Response response = storageClient
            .getContainerAsync(DEFAULT_STORAGE_STRATEGY, entryParams.getId(), OBJECT, getNoLogAccessLog());
        try (InputStream src = response.readEntity(InputStream.class)) {
            Path target = inputFilesDirectory.resolve(entryParams.getId());
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
                            throw new RuntimeException(
                                "action must be of type: IDENTIFY | EXTRACT | ANALYSE | GENERATE");
                    }
                })));
    }

    private void createReport(ResultPreservation resultPreservation, List<PreservationDistributionLine> request,
        int tenant)
        throws VitamClientInternalException {
        List<PreservationReportModel> reportModels = toReportModel(resultPreservation, request, tenant, now());
        preservationReportService.appendPreservationEntries(resultPreservation.getRequestId(), reportModels);
    }

    private List<PreservationReportModel> toReportModel(ResultPreservation outputs,
        List<PreservationDistributionLine> requests,
        int tenant, LocalDateTime now) {
        return outputs.getOutputs().entrySet()
            .stream()
            .flatMap(entry -> entry.getValue()
                .stream()
                .map(value -> getPreservationReportModel(outputs, tenant, now, value, requests))
            ).collect(Collectors.toList());
    }

    private PreservationReportModel getPreservationReportModel(ResultPreservation outputs, int tenant,
        LocalDateTime now, OutputPreservation value, List<PreservationDistributionLine> requests) {
        PreservationDistributionLine model =
            IterableUtils.find(requests, j -> j.getId().equals(value.getInputPreservation().getName()));

        return new PreservationReportModel(
            outputs.getRequestId(),
            outputs.getId(),
            tenant,
            now.toString(),
            value.getStatus(),
            model.getUnitId(),
            model.getObjectId(),
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

    private void createParametersBatchFile(List<PreservationDistributionLine> entryParams, Path batchDirectory,
        String requestId, String batchId) throws Exception {
        List<InputPreservation> inputPreservations = entryParams.stream()
            .map(this::mapToInput)
            .collect(Collectors.toList());
        List<ActionPreservation> preservationActions =
            entryParams.stream().flatMap(this::mapToActions).collect(Collectors.toList());

        boolean debug = entryParams.get(0).isDebug();
        ParametersPreservation parametersPreservation =
            new ParametersPreservation(requestId, batchId, inputPreservations, preservationActions, debug);
        Path parametersPath = batchDirectory.resolve(PARAMETERS_JSON);
        JsonHandler.writeAsFile(parametersPreservation, parametersPath.toFile());
    }


    private Stream<ActionPreservation> mapToActions(PreservationDistributionLine entryParams) {
        return entryParams.getActionPreservationList().stream();

    }

    private InputPreservation mapToInput(PreservationDistributionLine entryParams) {
        return new InputPreservation(entryParams.getId(), entryParams.getFormatId());
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
                IOUtils.toString(griffin.getErrorStream(), UTF_8),
                IOUtils.toString(griffin.getInputStream(), UTF_8)

            );
        }

        return JsonHandler.getFromFile(batchDirectory.resolve(RESULT_JSON).toFile(), ResultPreservation.class);
    }
}

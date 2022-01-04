package fr.gouv.vitam.worker.core.plugin.evidence;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.MetadataType;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.async.AccessRequestContext;
import fr.gouv.vitam.processing.common.async.ProcessingRetryAsyncException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.common.CheckResourceAvailability;
import fr.gouv.vitam.worker.core.plugin.evidence.exception.EvidenceStatus;
import fr.gouv.vitam.worker.core.plugin.evidence.report.EvidenceAuditReportLine;
import fr.gouv.vitam.worker.core.plugin.evidence.report.EvidenceAuditReportObject;
import fr.gouv.vitam.worker.core.utils.PluginHelper;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;
import static java.util.stream.Collectors.toList;

public class DataRectificationCheckResourceAvailability extends CheckResourceAvailability {
    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(DataRectificationCheckResourceAvailability.class);
    public static final String PLUGIN_NAME = "CORRECTIVE_AUDIT_CHECK_RESOURCE_AVAILABILITY";
    private static final String ALTER = "alter";

    public DataRectificationCheckResourceAvailability() {
        this(StorageClientFactory.getInstance());
    }

    @VisibleForTesting
    public DataRectificationCheckResourceAvailability(StorageClientFactory storage) {
        super(storage);
    }

    @Override
    public List<ItemStatus> executeList(WorkerParameters workerParameters, HandlerIO handler) throws
        ProcessingException {

        try {
            Map<DataCategory, Map<AccessRequestContext, List<String>>> entriesByCategories =
                extractResources(workerParameters, handler);

            checkResourcesAvailabilityByTypes(entriesByCategories);

            return IntStream.range(0, workerParameters.getObjectNameList().size()).
                mapToObj(index -> buildItemStatus(PLUGIN_NAME, StatusCode.OK,
                    PluginHelper.EventDetails.of(String.format("%s executed", PLUGIN_NAME))))
                .collect(toList());
        } catch (ProcessingRetryAsyncException prae) {
            LOGGER.info("Some resources where not available");
            throw prae;
        } catch (Exception e) {
            throw new ProcessingException(e);
        }

    }

    private Map<DataCategory, Map<AccessRequestContext, List<String>>> extractResources(
        WorkerParameters workerParameters, HandlerIO handler) throws
        IOException, InvalidParseOperationException, ContentAddressableStorageNotFoundException,
        ContentAddressableStorageServerException {
        Map<DataCategory, Map<AccessRequestContext, List<String>>> entries = new HashMap<>();
        Map<AccessRequestContext, List<String>> entriesObjectGroup = new HashMap<>();
        Map<AccessRequestContext, List<String>> entriesObject = new HashMap<>();
        Map<AccessRequestContext, List<String>> entriesUnit = new HashMap<>();

        for (String objectId : workerParameters.getObjectNameList()) {
            File file = handler.getFileFromWorkspace(ALTER + File.separator + objectId);
            EvidenceAuditReportLine line =
                JsonHandler.getFromFile(file, EvidenceAuditReportLine.class);
            if (line.getObjectType().equals(MetadataType.OBJECTGROUP)) {
                extractMetadata(line).ifPresent(
                    pair -> entriesObjectGroup.computeIfAbsent(pair.getLeft(), (x -> new ArrayList<>()))
                        .add(pair.getRight()));
                Map<AccessRequestContext, List<String>> objectExtracted = extractObjects(line);
                objectExtracted.keySet().forEach(context -> {
                    entriesObject.computeIfAbsent(context, (x -> new ArrayList<>()))
                        .addAll(objectExtracted.get(context));
                });
            }

            if (line.getObjectType().equals(MetadataType.UNIT)) {
                extractMetadata(line).ifPresent(
                    pair -> entriesUnit.computeIfAbsent(pair.getLeft(), (x -> new ArrayList<>())).add(pair.getRight()));
            }

        }

        entries.put(DataCategory.OBJECTGROUP, entriesObjectGroup);
        entries.put(DataCategory.OBJECT, entriesObject);
        entries.put(DataCategory.UNIT, entriesUnit);
        return entries;
    }

    private Optional<Pair<AccessRequestContext, String>> extractMetadata(EvidenceAuditReportLine line) {
        List<String> goodOffers = new ArrayList<>();
        List<String> badOffers = new ArrayList<>();
        if (DataRectificationHelper.canDoCorrection(line.getOffersHashes(), line.getSecuredHash(), goodOffers,
            badOffers)) {
            return Optional.of(new ImmutablePair<>(new AccessRequestContext(line.getStrategyId(), goodOffers.get(0)),
                line.getIdentifier() + ".json"));
        } else {
            return Optional.empty();
        }
    }

    private Map<AccessRequestContext, List<String>> extractObjects(EvidenceAuditReportLine line) {
        Map<AccessRequestContext, List<String>> entriesObject = new HashMap<>();
        for (EvidenceAuditReportObject object : line.getObjectsReports()) {
            List<String> goodOffers = new ArrayList<>();
            List<String> badOffers = new ArrayList<>();
            if (object.getEvidenceStatus() != EvidenceStatus.OK &&
                DataRectificationHelper.canDoCorrection(object.getOffersHashes(), object.getSecuredHash(), goodOffers,
                    badOffers)) {
                entriesObject.computeIfAbsent(new AccessRequestContext(object.getStrategyId(), goodOffers.get(0)),
                    (x -> new ArrayList<>())).add(object.getIdentifier());
            }
        }
        return entriesObject;

    }
}

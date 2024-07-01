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
package fr.gouv.vitam.functional.administration.core.audit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.CanonicalJsonFormatter;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.AuditVitamException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageUnavailableDataFromAsyncOfferClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import org.apache.commons.collections4.iterators.ArrayIterator;
import org.elasticsearch.common.util.set.Sets;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService.DEFAULT_EXTENSION;
import static fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService.FIELD_COLLECTION;

public class ReferentialAuditService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReferentialAuditService.class);
    private static final AlertService alertService = new AlertServiceImpl();
    private static final String DIGEST = "digest";

    private final StorageClientFactory storageClientFactory;
    private final FunctionalBackupService functionalBackupService;

    public ReferentialAuditService(StorageClientFactory storageClientFactory, VitamCounterService vitamCounterService) {
        this.storageClientFactory = storageClientFactory;
        this.functionalBackupService = new FunctionalBackupService(vitamCounterService);
    }

    @VisibleForTesting
    ReferentialAuditService(
        StorageClientFactory storageClientFactory,
        FunctionalBackupService functionalBackupService
    ) {
        this.storageClientFactory = storageClientFactory;
        this.functionalBackupService = functionalBackupService;
    }

    private static <T> Iterable<T> iteratorToIterable(Iterator<T> iterator) {
        return () -> iterator;
    }

    public void runAudit(String collectionName, int tenant)
        throws StorageServerClientException, StorageNotFoundException, InvalidParseOperationException, StorageNotFoundClientException, BadRequestException, AuditVitamException, StorageUnavailableDataFromAsyncOfferClientException {
        FunctionalAdminCollections collection = FunctionalAdminCollections.getFromValue(collectionName);

        if (Objects.isNull(collection)) {
            throw new BadRequestException("collection not found");
        }

        if (!collection.isMultitenant() && tenant != VitamConfiguration.getAdminTenant()) {
            throw new BadRequestException("admin tenant required");
        }

        Optional<ObjectEntry> objectEntryToAudit = StreamSupport.stream(
            iteratorToIterable(listObjectEntry()).spliterator(),
            false
        )
            .filter(e -> matcherFilter(e, collection))
            .reduce(this::getLastBackupFile);

        ArrayNode documentsInDB = findDocuments(tenant, collection);

        if (objectEntryToAudit.isPresent()) {
            ObjectEntry next = objectEntryToAudit.get();
            List<String> offersIds = getOffers(VitamConfiguration.getDefaultStrategy());
            Map<String, String> hashMap = isHashesEquals(offersIds, next);
            verifyCoherence(offersIds, documentsInDB, next, hashMap, collectionName, tenant);
        } else if (!documentsInDB.isEmpty()) {
            String message = String.format(
                "[KO] collection=%s, tenant=%s file not present in default offer (referent offer)",
                collectionName,
                tenant
            );
            alertService.createAlert(VitamLogLevel.ERROR, message);
            throw new AuditVitamException(message);
        }
    }

    private void verifyCoherence(
        List<String> offerIds,
        ArrayNode documentsInDB,
        ObjectEntry next,
        Map<String, String> mapOfHashes,
        String collectionName,
        int tenant
    )
        throws StorageNotFoundException, StorageServerClientException, AuditVitamException, StorageUnavailableDataFromAsyncOfferClientException {
        try (StorageClient storageClient = storageClientFactory.getClient()) {
            boolean hasUniqueHash = (new HashSet<>(mapOfHashes.values())).size() == 1;
            if (hasUniqueHash && mapOfHashes.keySet().containsAll(offerIds)) { // All offers are synchronized
                Response response = storageClient.getContainerAsync(
                    VitamConfiguration.getDefaultStrategy(),
                    next.getObjectId(),
                    DataCategory.BACKUP,
                    AccessLogUtils.getNoLogAccessLog()
                );
                try {
                    InputStream is = response.readEntity(InputStream.class);
                    ArrayNode documentsInJson = JsonHandler.getFromJsonNode(
                        JsonHandler.getFromInputStream(is).get(FIELD_COLLECTION),
                        new TypeReference<>() {}
                    );

                    List<String> list = diff(documentsInDB, documentsInJson);

                    if (!list.isEmpty()) {
                        String message = String.format(
                            "[KO] collectionName=%s, tenant=%s all offers are incoherent with database",
                            collectionName,
                            tenant
                        );
                        alertService.createAlert(VitamLogLevel.ERROR, message);
                        throw new AuditVitamException(message);
                    }
                } catch (InvalidParseOperationException e) {
                    String message = String.format(
                        "[KO] collectionName=%s, tenant=%s all offers are incoherent with database",
                        collectionName,
                        tenant
                    );
                    alertService.createAlert(VitamLogLevel.ERROR, message);
                    throw new AuditVitamException(message);
                }
            } else {
                Map<String, Response> offersData = new HashMap<>();
                for (String offer : mapOfHashes.keySet()) {
                    offersData.put(
                        offer,
                        storageClient.getContainerAsync(
                            VitamConfiguration.getDefaultStrategy(),
                            offer,
                            next.getObjectId(),
                            DataCategory.BACKUP,
                            AccessLogUtils.getNoLogAccessLog()
                        )
                    );
                }

                Map<String, InputStream> collect = offersData
                    .entrySet()
                    .stream()
                    .map(e -> new SimpleEntry<>(e.getKey(), e.getValue().readEntity(InputStream.class)))
                    .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));

                Map<String, ArrayNode> documentsByOffer = new HashMap<>();
                Set<String> listOffersThatContainsCorruptedFile = new HashSet<>();
                collect.forEach((offerId, is) -> {
                    try {
                        JsonNode fileJson = JsonHandler.getFromInputStream(is);
                        if (fileJson != null && !(fileJson instanceof NullNode) && fileJson.has(FIELD_COLLECTION)) {
                            documentsByOffer.put(
                                offerId,
                                JsonHandler.getFromJsonNode(fileJson.get(FIELD_COLLECTION), new TypeReference<>() {})
                            );
                        } else {
                            listOffersThatContainsCorruptedFile.add(offerId);
                        }
                    } catch (InvalidParseOperationException e) {
                        listOffersThatContainsCorruptedFile.add(offerId);
                    }
                });

                Set<String> offersWhichCoherentWithDB = documentsByOffer
                    .entrySet()
                    .stream()
                    .filter(e -> !listOffersThatContainsCorruptedFile.contains(e.getKey()))
                    .filter(e -> diff(documentsInDB, e.getValue()).isEmpty())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
                if (offersWhichCoherentWithDB.isEmpty()) {
                    String message = String.format(
                        "[KO] collectionName=%s, tenant=%s all offers are incoherent with database",
                        collectionName,
                        tenant
                    );
                    alertService.createAlert(VitamLogLevel.ERROR, message);
                    throw new AuditVitamException(message);
                } else {
                    String message = String.format(
                        "[KO] collectionName=%s, tenant=%s some offers are incoherent with database\n\r" +
                        "coherent offers = %s\n\r" +
                        "incoherent offers = %s\n\r",
                        collectionName,
                        tenant,
                        offersWhichCoherentWithDB,
                        Sets.difference(new HashSet<>(offerIds), offersWhichCoherentWithDB)
                    );
                    alertService.createAlert(VitamLogLevel.ERROR, message);
                    throw new AuditVitamException(message);
                }
            }
        }
    }

    private ArrayNode findDocuments(int tenant, FunctionalAdminCollections collection)
        throws InvalidParseOperationException {
        return this.functionalBackupService.getCollectionInJson(collection, tenant);
    }

    private List<String> diff(ArrayNode source, ArrayNode target) {
        return VitamDocument.getUnifiedDiff(
            JsonHandler.prettyPrint(new String(CanonicalJsonFormatter.serializeToByteArray(source))),
            JsonHandler.prettyPrint(new String(CanonicalJsonFormatter.serializeToByteArray(target)))
        );
    }

    private List<String> getOffers(String strategyId)
        throws StorageNotFoundClientException, StorageServerClientException {
        try (StorageClient storageClient = storageClientFactory.getClient()) {
            return storageClient.getOffers(strategyId);
        }
    }

    private ObjectEntry getLastBackupFile(ObjectEntry objectEntry, ObjectEntry objectEntry1) {
        if (objectEntry.getObjectId().compareTo(objectEntry1.getObjectId()) > 0) return objectEntry;
        else return objectEntry1;
    }

    private boolean matcherFilter(ObjectEntry next, FunctionalAdminCollections collection) {
        Pattern pattern = Pattern.compile("(\\d+)_([A-z0-9]+)_(\\d+)(\\." + DEFAULT_EXTENSION + ")");
        Matcher matcher = pattern.matcher(next.getObjectId());
        return matcher.matches() && matcher.group(2).equals(collection.getName());
    }

    private Iterator<ObjectEntry> listObjectEntry() throws StorageServerClientException {
        try (StorageClient storageClient = storageClientFactory.getClient()) {
            return storageClient.listContainer(VitamConfiguration.getDefaultStrategy(), null, DataCategory.BACKUP);
        } catch (StorageNotFoundClientException e) {
            return new ArrayIterator<>(new ObjectEntry[] {});
        }
    }

    private Map<String, String> isHashesEquals(List<String> offerIds, ObjectEntry next)
        throws StorageServerClientException, StorageNotFoundClientException {
        String strategyId = VitamConfiguration.getDefaultStrategy();

        try (StorageClient storageClient = storageClientFactory.getClient()) {
            JsonNode information = storageClient.getInformation(
                strategyId,
                DataCategory.BACKUP,
                next.getObjectId(),
                offerIds,
                false
            );

            return offerIds
                .stream()
                .map(e -> new SimpleEntry<>(e, information.get(e)))
                .filter(e -> Objects.nonNull(e.getValue()))
                .filter(e -> !e.getValue().isMissingNode())
                .filter(e -> !e.getValue().isNull())
                .filter(e -> e.getValue().has(DIGEST))
                .map(e -> new SimpleEntry<>(e.getKey(), e.getValue().get(DIGEST).textValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }
}

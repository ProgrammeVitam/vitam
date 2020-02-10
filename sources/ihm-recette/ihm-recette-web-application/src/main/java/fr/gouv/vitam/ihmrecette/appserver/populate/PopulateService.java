/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.ihmrecette.appserver.populate;


import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.stream.VitamAsyncInputStreamResponse;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.exception.StorageTechnicalException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.TapeReadRequestReferentialEntity;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.Document;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class PopulateService {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PopulateService.class);
    private static String populateFileDigest;

    public static final String TENANT = "_tenant";
    public static final String CONTRACT_POPULATE = "ContractPopulate";
    public static final File POPULATE_FILE = PropertiesUtils.fileFromTmpFolder("PopulateFile");

    private final AtomicBoolean populateInProgress = new AtomicBoolean(false);
    private final Scheduler io;
    private final MetadataRepository metadataRepository;
    private final MasterdataRepository masterdataRepository;
    private final LogbookRepository logbookRepository;
    private final MetadataStorageService metadataStorageService;
    private final UnitGraph unitGraph;
    private final DigestType digestType;
    private final int nbThreads;

    @VisibleForTesting
    public PopulateService(MetadataRepository metadataRepository, MasterdataRepository masterdataRepository,
        LogbookRepository logbookRepository, UnitGraph unitGraph, int nThreads,
        MetadataStorageService metadataStorageService, Scheduler io) {
        this.metadataRepository = metadataRepository;
        this.masterdataRepository = masterdataRepository;
        this.logbookRepository = logbookRepository;
        this.unitGraph = unitGraph;
        this.nbThreads = nThreads;
        this.digestType = VitamConfiguration.getDefaultDigestType();
        this.metadataStorageService = metadataStorageService;
        this.io = io;
    }

    public PopulateService(MetadataRepository metadataRepository, MasterdataRepository masterdataRepository,
        LogbookRepository logbookRepository, UnitGraph unitGraph, int nThreads,
        MetadataStorageService metadataStorageService) {
        this(metadataRepository, masterdataRepository, logbookRepository, unitGraph, nThreads, metadataStorageService,
            Schedulers.io());
    }

    public void populateVitam(PopulateModel populateModel) {

        if (populateInProgress.get()) {
            return;
        }

        Stopwatch stopwatch = Stopwatch.createStarted();

        populateInProgress.set(true);

        Map<String, String> options = new HashMap<>();
        String identifier = populateModel.getSp();
        int tenantId = populateModel.getTenant();
        options.put("Identifier", identifier);
        options.put(TENANT, populateModel.getTenant() + "");
        Optional<Document> agencyDocuments =
            this.masterdataRepository.findAgency(populateModel.getTenant(), identifier);

        if (!agencyDocuments.isPresent()) {
            this.masterdataRepository.importAgency(identifier, tenantId);
        }

        options.clear();
        options.put("Name", CONTRACT_POPULATE);
        Optional<Document> contractDocument =
            this.masterdataRepository.findAccessContract(CONTRACT_POPULATE);

        if (!contractDocument.isPresent()) {
            this.masterdataRepository.importAccessContract(CONTRACT_POPULATE, tenantId);
        }

        if (populateModel.isWithRules()) {
            Map<String, Integer> ruleMap = populateModel.getRuleTemplatePercent();
            for (String rule : ruleMap.keySet()) {
                options.clear();
                options.put("RuleId", rule);
                options.put(TENANT, populateModel.getTenant() + "");
                Optional<Document> ruleDocuments = this.masterdataRepository.findRule(tenantId, rule);
                if (!ruleDocuments.isPresent()) {
                    this.masterdataRepository.importRule(rule, tenantId);
                }

            }
        }
        if (populateModel.getObjectSize() > 0) {
            String text = RandomStringUtils.random(populateModel.getObjectSize());
            try (RandomAccessFile file = new RandomAccessFile(POPULATE_FILE, "rw")) {
                file.writeChars(text);
                populateFileDigest = new Digest(digestType).update(text).digestHex();
            } catch (IOException e) {
                LOGGER.error(e);
            }

        }
        Flowable.range(0, populateModel.getNumberOfUnit())
            .observeOn(io)
            .map(index -> unitGraph
                .createGraph(index, populateModel))
            .buffer(populateModel.getBulkSize())
            .parallel(nbThreads)
            .map(unitGotList -> bulkPersist(populateModel, unitGotList))
            .sequential()
            .subscribe(t -> {
            }, t -> {
                LOGGER.error(t);
                try {
                    Files.delete(POPULATE_FILE.toPath());
                } catch (IOException e) {
                    LOGGER.error(e);
                }
                populateInProgress.set(false);
            }, () -> {
                long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
                LOGGER.info("save time: {}", elapsed);

                // update accession register if complete with success
                Optional<Document> accessionresgiterSummary =
                    this.masterdataRepository.findAccessionRegitserSummary(tenantId, identifier);

                if (!accessionresgiterSummary.isPresent()) {
                    this.masterdataRepository.createAccessionRegisterSummary(tenantId, identifier,
                        populateModel.getNumberOfUnit(),
                        populateModel.getNumberOfUnit() * populateModel.getObjectSize());
                }

                try {
                    Files.delete(POPULATE_FILE.toPath());
                } catch (IOException e) {
                    LOGGER.error(e);
                }
                populateInProgress.set(false);
            });
    }

    public RequestResponse<TapeReadRequestReferentialEntity> createReadOrderRequest(Integer tenant, String strategyId, String offerId, String objectId, DataCategory dataCategory) {
        return metadataStorageService.createReadOrderRequest(tenant, strategyId, offerId, objectId, dataCategory);
    }

    public RequestResponse<TapeReadRequestReferentialEntity> getReadOrderRequest(Integer tenant, String strategyId, String offerId, String readOrderId) {
        return metadataStorageService.getReadOrderRequest(tenant, strategyId, offerId, readOrderId);
    }

    public VitamAsyncInputStreamResponse download(Integer tenantId, DataCategory dataCategory,
        String strategyId,
        String offerId,
        String objectId) throws StorageTechnicalException, StorageDriverException, StorageNotFoundException {
        return metadataStorageService.download(tenantId, dataCategory, strategyId, offerId, objectId);
    }

    public Collection<StorageStrategy> getStrategies() throws StorageException {
        return metadataStorageService.getStrategies();
    }

    private boolean bulkPersist(PopulateModel populateModel, List<UnitGotModel> unitGotList) {

        metadataRepository.store(populateModel.getTenant(), unitGotList,
            populateModel.isStoreInDb(), populateModel.isIndexInEs());

        logbookRepository.storeLogbookLifecycleUnit(populateModel.getTenant(), unitGotList);

        logbookRepository.storeLogbookLifeCycleObjectGroup(unitGotList);

        metadataStorageService.storeToOffers(populateModel, unitGotList);

        return true;
    }

    public boolean inProgress() {
        return populateInProgress.get();
    }

    static String getPopulateFileDigest() {
        return populateFileDigest;
    }

}

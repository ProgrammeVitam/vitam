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
package fr.gouv.vitam.ihmrecette.appserver.populate;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.json.CanonicalJsonFormatter;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.MetadataStorageHelper;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.TapeReadRequestReferentialEntity;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Handles metadata backup for populate service
 */
public class MetadataStorageService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PopulateService.class);

    private final MetadataRepository metadataRepository;
    private final LogbookRepository logbookRepository;
    private final StoragePopulateImpl storagePopulateService;
    private final ExecutorService storageExecutorService
        = Executors.newFixedThreadPool(16, VitamThreadFactory.getInstance());

    /**
     * Constructor
     */
    public MetadataStorageService(MetadataRepository metadataRepository,
        LogbookRepository logbookRepository,
        StoragePopulateImpl storagePopulateService) {
        this.metadataRepository = metadataRepository;
        this.logbookRepository = logbookRepository;
        this.storagePopulateService = storagePopulateService;
    }

    /**
     * Stores LFC / GOT + LFC into offers
     *
     * @param populateModel
     * @param unitGotList
     * @return
     */
    public boolean storeToOffers(PopulateModel populateModel, List<UnitGotModel> unitGotList) {

        if (populateModel.isStoreMetadataAndLfcInOffers()) {

            List<String> unitIds = unitGotList.stream().map(i -> i.getUnit().getId()).collect(Collectors.toList());
            persistMetadataAndLfcToOffers(populateModel, unitIds, VitamDataType.UNIT, VitamDataType.LFC_UNIT,
                DataCategory.UNIT);

            List<String> gotIds = unitGotList.stream().map(i -> i.getGot().getId()).collect(Collectors.toList());
            persistMetadataAndLfcToOffers(populateModel, gotIds, VitamDataType.GOT, VitamDataType.LFC_GOT,
                DataCategory.OBJECTGROUP);
        }

        return true;
    }

    private void persistMetadataAndLfcToOffers(PopulateModel populateModel, List<String> ids,
        VitamDataType mdDataType, VitamDataType lfcDataType, DataCategory dataCategory) {
        Map<String, JsonNode> metadataByIds = metadataRepository.findRawMetadataByIds(ids, mdDataType);
        Map<String, JsonNode> lfcsByIds = logbookRepository.findRawLfcsByIds(ids, lfcDataType);

        List<CompletableFuture<Void>> completableFutures = new ArrayList<>();

        for (String id : ids) {

            CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> {

                JsonNode docWithLfc;
                if (dataCategory == DataCategory.UNIT) {
                    docWithLfc = MetadataStorageHelper.getUnitWithLFC(metadataByIds.get(id), lfcsByIds.get(id));
                } else {
                    docWithLfc = MetadataStorageHelper.getGotWithLFC(metadataByIds.get(id), lfcsByIds.get(id));
                }

                persistMetadataAndLfcToOffers(populateModel, id, docWithLfc, dataCategory);

            }, storageExecutorService);

            completableFutures.add(completableFuture);
        }

        CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0])).join();
    }

    private void persistMetadataAndLfcToOffers(PopulateModel populateModel, String id, JsonNode docWithLfc,
        DataCategory dataCategory) {

        File file = null;
        FileOutputStream fos = null;
        try {
            file = PropertiesUtils.fileFromTmpFolder(id + ".json");
            fos = new FileOutputStream(file);

            CanonicalJsonFormatter.serialize(docWithLfc, fos);

            storagePopulateService.storeData(VitamConfiguration.getDefaultStrategy(),
                id + ".json", file, dataCategory, populateModel.getTenant());

        } catch (IOException | StorageException e) {
            LOGGER.error("Could not persist Md + Lfc to offers", e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                }
            }
            if (file != null) {
                if (!file.delete()) {
                    LOGGER.warn("Could not delete file " + file.getAbsolutePath());
                }
            }
        }
    }

    public RequestResponse<TapeReadRequestReferentialEntity> createReadOrderRequest(Integer tenant, String strategyId,
        String objectId, DataCategory dataCategory) {
        return storagePopulateService.createReadOrderRequest(tenant, strategyId, objectId, dataCategory);
    }

    public RequestResponse<TapeReadRequestReferentialEntity> getReadOrderRequest(Integer tenant, String strategyId,
        String readOrderId) {
        return storagePopulateService.getReadOrderRequest(tenant, strategyId, readOrderId);
    }

    public StorageOffer getOffer(String offerId) throws StorageException {
        return storagePopulateService.getOffer(offerId);
    }

    public Collection<StorageStrategy> getStrategies() throws StorageException {
        return storagePopulateService.getStrategies();
    }
}

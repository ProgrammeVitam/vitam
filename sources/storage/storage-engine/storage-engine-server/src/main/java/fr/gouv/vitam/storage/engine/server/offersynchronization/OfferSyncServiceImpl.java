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
package fr.gouv.vitam.storage.engine.server.offersynchronization;

import com.google.common.collect.Iterables;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;
import fr.gouv.vitam.storage.engine.server.exception.VitamSyncException;
import fr.gouv.vitam.storage.engine.common.model.response.OfferSyncResponseItem;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Synchronization of a storage offer from another one.
 */
public class OfferSyncServiceImpl implements OfferSyncService {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(OfferSyncServiceImpl.class);

    private static final String STRATEGY_ID = "default";
    private static final String SOURCE_OFFER_ID_PARAMETER_MONDATORY_MSG = "The sourceOfferId parameter is mandatory.";
    private static final String DESTINATION_OFFER_ID_PARAMETER_MONDATORY_MSG =
        "The destinationOfferId parameter is mandatory.";
    public static final String SYNCHRONIZATION_TENANTS_MANDATORY_MSG = "The Vitam tenants are mandatory.";

    private final RestoreOfferBackupService restoreOfferBackupService;
    private final StorageDistribution distribution;

    /**
     * Constructor.
     *
     * @param distribution
     */
    public OfferSyncServiceImpl(StorageDistribution distribution) {
        this(new RestoreOfferBackupService(distribution), distribution);
    }

    /**
     * Constructor.
     *
     * @param restoreOfferBackupService
     * @param distribution
     */
    public OfferSyncServiceImpl(
        RestoreOfferBackupService restoreOfferBackupService,
        StorageDistribution distribution) {
        this.restoreOfferBackupService = restoreOfferBackupService;
        this.distribution = distribution;
    }

    /**
     * Synchronize an offer from anthor using the offset.
     *
     * @param sourceOffer      the identifer of the source offer
     * @param destinationOffer the identifier of the destination offer
     * @param offset           the offset of the process of the synchronisation
     * @param containerToSync
     * @param tenantIdToSync   @return OfferSync response
     * @throws VitamSyncException
     */
    @Override
    public OfferSyncResponseItem synchronize(String sourceOffer, String destinationOffer,
        String containerToSync, Integer tenantIdToSync, Long offset) throws VitamSyncException {

        ParametersChecker.checkParameter(SOURCE_OFFER_ID_PARAMETER_MONDATORY_MSG, sourceOffer);
        ParametersChecker.checkParameter(DESTINATION_OFFER_ID_PARAMETER_MONDATORY_MSG, destinationOffer);

        LOGGER.debug(String
            .format("Start the synchronization process of the new offer {%s} from the source offer {%s}.",
                destinationOffer, sourceOffer));

        // get the list of vitam tenants from the configuration.
        List<Integer> tenants = VitamConfiguration.getTenants();
        Long newOffset = null;

        if (tenants == null || tenants.isEmpty()) {
            throw new VitamSyncException(SYNCHRONIZATION_TENANTS_MANDATORY_MSG);
        }

        DataCategory categoryToSync = null;
        if (containerToSync != null) {
            categoryToSync = DataCategory.getByCollectionName(containerToSync);
        }

        boolean containerAlreadySync = categoryToSync != null;
        boolean tenantIdAlreadySync = tenantIdToSync != null;

        OfferSyncResponseItem response =
            new OfferSyncResponseItem().setOfferSource(sourceOffer).setOfferDestination(destinationOffer);
        Integer originalTenant = VitamThreadUtils.getVitamSession().getTenantId();

        try {
            for (Integer tenant : tenants) {
                // This is a hak, we must set manually the tenant is the VitamSession (used and transmitted in the headers)
                VitamThreadUtils.getVitamSession().setTenantId(tenant);

                if (tenantIdAlreadySync && tenant < tenantIdToSync) {
                    continue;
                }

                if (tenant == tenantIdToSync) {
                    tenantIdAlreadySync = false;
                }

                for (DataCategory category : DataCategory.values()) {
                    if (StringUtils.isBlank(category.getFolder())) {
                        continue;
                    }

                    if (containerAlreadySync && !category.equals(categoryToSync)) {
                        continue;
                    }

                    if (category.equals(categoryToSync)) {
                        containerAlreadySync = false;
                        newOffset = offset;

                    }

                    if (newOffset == null) {
                        // get the latest offset for the given collection and tenant
                        Optional<OfferLog> lastOfferLog =
                            restoreOfferBackupService
                                .getLatestOffsetByContainer(STRATEGY_ID, sourceOffer, category, null, 1);

                        if (!lastOfferLog.isPresent()) {
                            continue;
                        }

                        // set the new offset
                        newOffset = lastOfferLog.get().getSequence();
                    }

                    // get the data to synchronize
                    List<List<OfferLog>> listing = restoreOfferBackupService
                        .getListing(STRATEGY_ID, sourceOffer, category, newOffset,
                            Integer.MAX_VALUE, Order.DESC);

                    for (List<OfferLog> offerLogs : listing) {
                        for (OfferLog offerLog : offerLogs) {
                            try {
                                // check existing of the object on the given offer.
                                boolean exists =
                                    distribution.checkObjectExisting(STRATEGY_ID, offerLog.getFileName(), category,
                                        Arrays.asList(destinationOffer));

                                if (!exists) {
                                    // load the object/file from the given offer
                                    Response resp = null;
                                    try {
                                        resp = distribution
                                            .getContainerByCategory(STRATEGY_ID, offerLog.getFileName(), category,
                                                sourceOffer);
                                        if (resp != null &&
                                            resp.getStatus() == Response.Status.OK.getStatusCode()) {

                                            distribution.storeData(STRATEGY_ID, offerLog.getFileName(),
                                                category, null, destinationOffer, resp);
                                        }
                                    } finally {
                                        StreamUtils.consumeAnyEntityAndClose(resp);
                                    }
                                }
                                continue;
                            } catch (StorageException e) {
                                LOGGER.error(e.getMessage(), e);
                            }
                        }
                        LOGGER
                            .warn("[OfferSync]: successful synchronization of category : {}, tenant : {}, offset : {}",
                                category.getCollectionName(), tenant, Iterables.getLast(offerLogs).getSequence());
                    }
                    newOffset = null;
                }
            }

        } catch (VitamSyncException e) {
            LOGGER.error(String.format(
                "[OfferSync]: An exception has been thrown when synchronizing {%s} offer from {%s} source offer with {%s} offset.",
                destinationOffer, sourceOffer, offset));
            throw e;
        } finally {
            VitamThreadUtils.getVitamSession().setTenantId(originalTenant);
        }
        LOGGER.warn("The offers' synchronization is completed.");
        response.setStatus(StatusCode.OK);

        return response;
    }
}

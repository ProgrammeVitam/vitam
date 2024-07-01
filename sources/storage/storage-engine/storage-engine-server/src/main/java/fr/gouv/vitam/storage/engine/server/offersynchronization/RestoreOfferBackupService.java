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
package fr.gouv.vitam.storage.engine.server.offersynchronization;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;

import java.util.List;

/**
 * Service used to recover Backup copies.<br/>
 */
public class RestoreOfferBackupService {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(RestoreOfferBackupService.class);

    private final StorageDistribution distribution;

    /**
     * constructor.
     */
    public RestoreOfferBackupService(StorageDistribution distribution) {
        this.distribution = distribution;
    }

    /**
     * Retrieve listing of offerLogs defining objects to synchronize
     *
     * @param strategy storage strategy
     * @param offerId offer identifier
     * @param category container category
     * @param offset offset
     * @param limit limit
     * @param order the search order
     * @return list of offerLogs by bulkSize
     */
    public List<OfferLog> getListing(
        String strategy,
        String offerId,
        DataCategory category,
        Long offset,
        int limit,
        Order order
    ) throws StorageException {
        LOGGER.debug(
            String.format(
                "[Offer synchronization]: Retrieve listing of {%s} dataCategory from {%s} offer, with {%s} Vitam strategy from {%s} offset with {%s} limit",
                category.name(),
                offerId,
                strategy,
                offset,
                limit
            )
        );

        RequestResponse<OfferLog> result = distribution.getOfferLogsByOfferId(
            strategy,
            offerId,
            category,
            offset,
            limit,
            order
        );

        if (!result.isOk()) {
            throw new StorageException(
                String.format("ERROR: VitamError has been returned when using storage service: {%s}", result.toString())
            );
        }

        return ((RequestResponseOK<OfferLog>) result).getResults();
    }
}

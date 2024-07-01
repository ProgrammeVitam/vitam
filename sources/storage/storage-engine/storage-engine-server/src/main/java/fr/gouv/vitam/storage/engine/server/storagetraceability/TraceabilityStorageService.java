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
package fr.gouv.vitam.storage.engine.server.storagetraceability;

import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;

import javax.ws.rs.core.Response;
import java.util.Iterator;

/**
 * Service that allow Storage Traceability to use StorageDistribution in order to get some file and information in Offers
 */
public class TraceabilityStorageService {

    private static final Integer GET_LAST_BASE = 100;
    public static final String TRACEABILITY_ORIGIN = "traceability";

    private final StorageDistribution distribution;

    public TraceabilityStorageService(StorageDistribution distribution) {
        this.distribution = distribution;
    }

    /**
     * Get the files of the last storage backup since the last traceability (fromDate)
     *
     * @param strategyId The storage strategy ID
     * @return list of last saved files as iterator
     */
    public Iterator<OfferLog> getLastSavedStorageLogIterator(String strategyId) {
        return new OfferLogIterator(strategyId, Order.DESC, DataCategory.STORAGELOG, this.distribution, GET_LAST_BASE);
    }

    /**
     * Get the last storage traceability zip fileName
     *
     * @param strategyId The storage strategy ID
     * @return the zip's fileName of the last storage traceability operation
     */
    public String getLastTraceabilityZip(String strategyId) {
        Iterator<OfferLog> offerLogIterator = new OfferLogIterator(
            strategyId,
            Order.DESC,
            DataCategory.STORAGETRACEABILITY,
            this.distribution,
            1
        );

        if (!offerLogIterator.hasNext()) {
            return null;
        }
        return offerLogIterator.next().getFileName();
    }

    /**
     * Only direct call to @StorageDistribution.getContainerByCategory
     *
     * @param strategyId strategyID
     * @param objectId file id or name
     * @param category storage category of the file
     * @return the file as stream
     * @throws StorageException if some error technical problem while call StorageDistribution
     */
    public Response getObject(String strategyId, String objectId, DataCategory category) throws StorageException {
        return this.distribution.getContainerByCategory(
                strategyId,
                TRACEABILITY_ORIGIN,
                objectId,
                category,
                AccessLogUtils.getNoLogAccessLog()
            );
    }
}

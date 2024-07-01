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

package fr.gouv.vitam.storage.engine.common.metrics;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.metrics.VitamMetricsNames;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import io.prometheus.client.Summary;

import java.io.InputStream;

public class DownloadCountingInputStreamMetrics extends AbstractCountingInputStreamMetrics {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DownloadCountingInputStreamMetrics.class);

    public static final Summary DOWNLOAD_BYTES = Summary.build()
        .name(VitamMetricsNames.VITAM_STORAGE_DOWNLOAD_SIZE_BYTES)
        .labelNames("tenant", "strategy", "offer_id", "origin", "data_category")
        .help(
            "Vitam storage download objects from offers size in bytes per tenant, strategy, offer_id, origin of request  (normal, traceability, offer_sync) and data_category"
        )
        .register();

    public DownloadCountingInputStreamMetrics(
        Integer tenant,
        String strategy,
        String offerId,
        String origin,
        DataCategory dataCategory,
        InputStream inputStream
    ) {
        super(tenant, strategy, offerId, origin, dataCategory, inputStream);
    }

    @Override
    protected void onEndOfFileReached() {
        try {
            DOWNLOAD_BYTES.labels(super.tenant, strategy, offerId, origin, dataCategory).observe(super.getByteCount());
        } catch (Exception e) {
            LOGGER.warn(e);
        }
    }
}

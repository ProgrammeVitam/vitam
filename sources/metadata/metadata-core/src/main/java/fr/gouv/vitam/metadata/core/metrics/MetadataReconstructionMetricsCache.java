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
package fr.gouv.vitam.metadata.core.metrics;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.metrics.PassiveExpiringCache;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.concurrent.ThreadSafe;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@ThreadSafe
public class MetadataReconstructionMetricsCache {

    private final PassiveExpiringCache<
        Triple<MetadataCollections, Integer, String>,
        LocalDateTime
    > documentReconstructionStatsCache;
    private final PassiveExpiringCache<MetadataCollections, LocalDateTime> graphReconstructionStatsCache;

    public MetadataReconstructionMetricsCache(long cacheDuration, TimeUnit cacheDurationUnit) {
        this.documentReconstructionStatsCache = new PassiveExpiringCache<>(cacheDuration, cacheDurationUnit);
        this.graphReconstructionStatsCache = new PassiveExpiringCache<>(cacheDuration, cacheDurationUnit);
    }

    public void registerLastDocumentReconstructionDate(
        MetadataCollections metadataCollection,
        int tenant,
        String strategy,
        LocalDateTime lastDocumentReconstructionDate
    ) {
        ParametersChecker.checkParameter("Missing metadataCollection", metadataCollection);
        ParametersChecker.checkParameter("Missing strategy", strategy);
        ParametersChecker.checkParameter("Missing lastDocumentReconstructionDate", lastDocumentReconstructionDate);

        Triple<MetadataCollections, Integer, String> metadataKey = new ImmutableTriple<>(
            metadataCollection,
            tenant,
            strategy
        );
        documentReconstructionStatsCache.put(metadataKey, lastDocumentReconstructionDate);
    }

    public Duration getDocumentReconstructionLatency(
        MetadataCollections metadataCollection,
        int tenant,
        String strategy
    ) {
        ParametersChecker.checkParameter("Missing metadataCollection", metadataCollection);
        ParametersChecker.checkParameter("Missing strategy", strategy);

        Triple<MetadataCollections, Integer, String> metadataKey = new ImmutableTriple<>(
            metadataCollection,
            tenant,
            strategy
        );
        LocalDateTime lastDocumentReconstructionDate = this.documentReconstructionStatsCache.get(metadataKey);

        if (lastDocumentReconstructionDate == null) {
            return null;
        }

        return Duration.between(lastDocumentReconstructionDate, LocalDateUtil.now());
    }

    public void registerLastGraphReconstructionDate(
        MetadataCollections metadataCollection,
        LocalDateTime lastGraphReconstructionDate
    ) {
        ParametersChecker.checkParameter("Missing metadataCollection", metadataCollection);
        ParametersChecker.checkParameter("Missing lastGraphReconstructionDate", lastGraphReconstructionDate);

        graphReconstructionStatsCache.put(metadataCollection, lastGraphReconstructionDate);
    }

    public Duration getGraphReconstructionLatency(MetadataCollections metadataCollection) {
        ParametersChecker.checkParameter("Missing metadataCollection", metadataCollection);

        LocalDateTime lastGraphReconstructionDate = this.graphReconstructionStatsCache.get(metadataCollection);

        if (lastGraphReconstructionDate == null) {
            return null;
        }

        return Duration.between(lastGraphReconstructionDate, LocalDateUtil.now());
    }
}

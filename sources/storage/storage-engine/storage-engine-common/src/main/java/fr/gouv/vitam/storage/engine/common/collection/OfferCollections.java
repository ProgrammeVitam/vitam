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
package fr.gouv.vitam.storage.engine.common.collection;

import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.CompactedOfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferSequence;
import fr.gouv.vitam.storage.engine.common.model.QueueMessageEntity;
import fr.gouv.vitam.storage.engine.common.model.TapeArchiveReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.engine.common.model.TapeObjectReferentialEntity;
import fr.gouv.vitam.storage.engine.common.model.TapeReadRequestReferentialEntity;

public enum OfferCollections {
    /*
     * Global collections
     */
    OFFER_LOG(OfferLog.class, OfferLog.class.getSimpleName()),
    COMPACTED_OFFER_LOG(CompactedOfferLog.class, CompactedOfferLog.class.getSimpleName()),
    OFFER_SEQUENCE(OfferSequence.class, OfferSequence.class.getSimpleName()),

    /*
     * Tape storage collections
     */
    TAPE_CATALOG(TapeCatalog.class, "TapeCatalog"),
    TAPE_QUEUE_MESSAGE(QueueMessageEntity.class, "TapeQueueMessage"),
    TAPE_OBJECT_REFERENTIAL(TapeObjectReferentialEntity.class, "TapeObjectReferential"),
    TAPE_ARCHIVE_REFERENTIAL(TapeArchiveReferentialEntity.class, "TapeArchiveReferential"),
    TAPE_READ_REQUEST_REFERENTIAL(TapeReadRequestReferentialEntity.class, "TapeReadRequestReferential");

    private final Class<?> clazz;
    private String name;
    private String baseName;

    OfferCollections(Class<?> clazz, String baseName) {
        this.clazz = clazz;
        this.baseName = baseName;
        this.name = baseName;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public String getName() {
        return this.name;
    }

    public void setPrefix(String prefix) { // NOSONAR
        this.name = prefix + this.baseName;
    }
}

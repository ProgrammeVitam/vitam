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
package fr.gouv.vitam.metadata.core.database.collections;

import com.mongodb.client.model.Updates;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ObjectGroupGraphUpdates {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ObjectGroupGraphUpdates.class);

    private final Set<String> parentUnitsToAdd = new HashSet<>();
    private final Set<String> allParentUnitsToAdd = new HashSet<>();
    private final Set<String> operationsToAdd = new HashSet<>();
    private final Set<String> originatingAgenciesToAdd = new HashSet<>();

    public void buildParentGraph(Unit unit) {
        parentUnitsToAdd.add(unit.getId());
        allParentUnitsToAdd.add(unit.getId());
        allParentUnitsToAdd.addAll(unit.getCollectionOrEmpty(Unit.UNITUPS));
        originatingAgenciesToAdd.addAll(unit.getCollectionOrEmpty(Unit.ORIGINATING_AGENCIES));
        operationsToAdd.add(VitamThreadUtils.getVitamSession().getRequestId());
    }

    public Bson toBsonUpdate() {
        Bson updates = Updates.combine(
            // Update graph
            Updates.addEachToSet(ObjectGroup.UP, new ArrayList<>(this.parentUnitsToAdd)),
            Updates.addEachToSet(ObjectGroup.UNITUPS, new ArrayList<>(this.allParentUnitsToAdd)),
            Updates.addEachToSet(ObjectGroup.ORIGINATING_AGENCIES, new ArrayList<>(this.originatingAgenciesToAdd)),
            Updates.addEachToSet(ObjectGroup.OPS, new ArrayList<>(this.operationsToAdd)),
            // Last graph update date
            Updates.set(ObjectGroup.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.nowFormatted()),
            // Inc version
            Updates.inc(ObjectGroup.VERSION, 1),
            Updates.inc(ObjectGroup.ATOMIC_VERSION, 1)
        );

        // Debug
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("DEBUG: OG {}", toString());
        }

        return updates;
    }

    @Override
    public String toString() {
        return (
            "ObjectGroupGraphUpdates{" +
            "parentUnitsToAdd=" +
            parentUnitsToAdd +
            ", allParentUnitsToAdd=" +
            allParentUnitsToAdd +
            ", operationsToAdd=" +
            operationsToAdd +
            ", originatingAgenciesToAdd=" +
            originatingAgenciesToAdd +
            '}'
        );
    }
}

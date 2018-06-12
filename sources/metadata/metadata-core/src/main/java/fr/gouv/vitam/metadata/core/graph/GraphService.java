/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.metadata.core.graph;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbMetadataRepository;
import fr.gouv.vitam.metadata.core.database.collections.Unit;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

import com.google.common.collect.HashMultimap;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;

import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.ID;
import static fr.gouv.vitam.common.graph.GraphUtils.createGraphRelation;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataDocument.GRAPH_LAST_PERSISTED_DATE;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataDocument.OG;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataDocument.ORIGINATING_AGENCIES;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataDocument.ORIGINATING_AGENCY;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataDocument.UP;
import static fr.gouv.vitam.metadata.core.database.collections.Unit.GRAPH;
import static fr.gouv.vitam.metadata.core.database.collections.Unit.MAXDEPTH;
import static fr.gouv.vitam.metadata.core.database.collections.Unit.MINDEPTH;
import static fr.gouv.vitam.metadata.core.database.collections.Unit.PARENT_ORIGINATING_AGENCIES;
import static fr.gouv.vitam.metadata.core.database.collections.Unit.UNITDEPTHS;
import static fr.gouv.vitam.metadata.core.database.collections.Unit.UNITUPS;

/**
 * service to compute graph information.
 */
public class GraphService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(GraphService.class);

    public static final BasicDBObject UNIT_VITAM_GRAPH_PROJECTION =
        new BasicDBObject(UP, 1)
            .append(UNITUPS, 1)
            .append(GRAPH, 1)
            .append(ORIGINATING_AGENCIES, 1)
            .append(UNITDEPTHS, 1)
            .append(ORIGINATING_AGENCY, 1)
            .append(PARENT_ORIGINATING_AGENCIES, 1)
            .append(ID, 1)
            .append(OG, 1);

    private MongoDbMetadataRepository mongoDbMetadataRepository;

    public GraphService(MongoDbMetadataRepository mongoDbMetadataRepository) {
        this.mongoDbMetadataRepository = mongoDbMetadataRepository;
    }

    public void compute(Unit unit, Collection<String> directParents) throws MetaDataNotFoundException {

        String id = unit.getId();
        Set<String> allParents = new HashSet<>();
        Set<String> graph = new HashSet<>();
        HashMultimap<String, String> parentDepths = HashMultimap.create();
        Set<String> allOriginatingAgencies = new HashSet<>();
        MultiValuedMap<String, String> allParentOriginatingAgencies = new HashSetValuedHashMap<>();
        String originatingAgency = unit.get(ORIGINATING_AGENCY, String.class);
        if (originatingAgency != null) {
            allOriginatingAgencies.add(originatingAgency);
        }

        if (!directParents.isEmpty()) {

            allParents.addAll(directParents);

            for (String directParent : directParents) {
                graph.add(createGraphRelation(id, directParent));
            }

            Collection<? extends VitamDocument> select = mongoDbMetadataRepository.selectByIds(UNIT_VITAM_GRAPH_PROJECTION, directParents);

            final Set<String> notFound = new HashSet<>(directParents);

            for (VitamDocument vitamDocument : select) {
                Unit parentUnit = (Unit) vitamDocument;

                allParents.addAll(parentUnit.getCollectionOrEmpty(UNITUPS));

                graph.addAll(parentUnit.getCollectionOrEmpty(GRAPH));

                allOriginatingAgencies.addAll(parentUnit.getCollectionOrEmpty(ORIGINATING_AGENCIES));

                Map<String, Collection<String>> parentUnitsByOriginatingAgencies =
                    parentUnit.getMapOrEmpty(PARENT_ORIGINATING_AGENCIES);
                parentUnitsByOriginatingAgencies
                    .forEach((key, ids) -> allParentOriginatingAgencies.putAll(key, ids));

                String parentOriginatingAgency = parentUnit.get(ORIGINATING_AGENCY, String.class);
                if (parentOriginatingAgency != null) {
                    allParentOriginatingAgencies.put(parentOriginatingAgency, parentUnit.getId());
                }

                Map<String, List<String>> parentParentDepths = parentUnit.getMapOrEmpty(UNITDEPTHS);
                for (Map.Entry<String, List<String>> entry : parentParentDepths.entrySet()) {
                    Integer key = Integer.parseInt(entry.getKey());
                    parentDepths.putAll(Integer.toString(key + 1), entry.getValue());
                }

                notFound.remove(parentUnit.getId());

            }

            // Set/override direct parents depth to 1
            parentDepths.putAll("1", directParents);

            if (!notFound.isEmpty()) {
                LOGGER.error("Cannot find parent: " + notFound);
                throw new MetaDataNotFoundException("Cannot find parents: " + notFound);
            }
        }

        unit.put(UP, directParents);
        unit.put(UNITUPS, allParents);
        unit.put(GRAPH, graph);
        unit.put(ORIGINATING_AGENCIES, allOriginatingAgencies);
        unit.put(UNITDEPTHS, parentDepths.asMap());
        unit.put(PARENT_ORIGINATING_AGENCIES, allParentOriginatingAgencies.asMap());

        unit.put(MINDEPTH, 1);
        int maxDepth = parentDepths.keys().stream()
            .map(Integer::parseInt)
            .max(Comparator.naturalOrder())
            .orElse(0) + 1;

        unit.put(MAXDEPTH, maxDepth);

        unit.put(GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()));

        // Debug
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("DEBUG: UNIT {}", JSON.serialize(unit));
        }
    }

}

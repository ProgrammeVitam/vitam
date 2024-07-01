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
package fr.gouv.vitam.metadata.core.graph;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mongodb.BasicDBObject;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbMetadataRepository;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.metadata.core.database.collections.UnitGraphModel;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.ID;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataDocument.OG;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataDocument.ORIGINATING_AGENCY;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataDocument.UP;

/**
 * compute graph information with recursive parent load.
 */
public class GraphLoader implements AutoCloseable {

    private static final Pattern pattern = Pattern.compile("loadAll failed to return a value for (.*)");

    private final MongoDbMetadataRepository<Unit> mongoDbMetadataRepository;

    static final BasicDBObject UNIT_VITAM_GRAPH_PROJECTION = new BasicDBObject(UP, 1)
        .append(ORIGINATING_AGENCY, 1)
        .append(ID, 1)
        .append(OG, 1);

    private LoadingCache<String, UnitGraphModel> unitLoadingCache = CacheBuilder.newBuilder()
        .build(
            new CacheLoader<String, UnitGraphModel>() {
                public UnitGraphModel load(String key) throws ExecutionException {
                    return loadAll(Collections.singleton(key)).get(key);
                }

                public Map<String, UnitGraphModel> loadAll(Iterable<? extends String> keys) throws ExecutionException {
                    return computeGraphByIds(keys);
                }
            }
        );

    public GraphLoader(MongoDbMetadataRepository<Unit> mongoDbMetadataRepository) {
        this.mongoDbMetadataRepository = mongoDbMetadataRepository;
    }

    public Map<String, UnitGraphModel> loadGraphInfo(Iterable<? extends String> ids) throws MetaDataNotFoundException {
        try {
            return unitLoadingCache.getAll(ids);
        } catch (CacheLoader.InvalidCacheLoadException e) {
            Matcher matcher = pattern.matcher(e.getMessage());
            if (matcher.matches()) {
                String id = matcher.group(1);
                throw new MetaDataNotFoundException("Cannot find parents: [" + id + "]");
            }
            throw new MetaDataNotFoundException(e);
        } catch (ExecutionException e) {
            throw new VitamRuntimeException(e);
        }
    }

    public void cleanCache() {
        unitLoadingCache.invalidateAll();
    }

    @Override
    public void close() {
        cleanCache();
    }

    private Map<String, UnitGraphModel> computeGraphByIds(Iterable<? extends String> ids) throws ExecutionException {
        Collection<Unit> units = mongoDbMetadataRepository.selectByIds(ids, UNIT_VITAM_GRAPH_PROJECTION);

        Set<String> collectParent = units.stream().flatMap(unit -> unit.getUp().stream()).collect(Collectors.toSet());

        if (collectParent.isEmpty()) {
            return units
                .stream()
                .map(UnitGraphModel::new)
                .collect(Collectors.toMap(UnitGraphModel::id, Function.identity()));
        }

        Map<String, UnitGraphModel> parents = unitLoadingCache.getAll(collectParent);

        return units
            .stream()
            .map(unit -> {
                UnitGraphModel unitGraphModel = new UnitGraphModel(unit);
                for (String up : unit.getUp()) {
                    UnitGraphModel graphParent = parents.get(up);
                    unitGraphModel.addParent(graphParent);
                }
                return unitGraphModel;
            })
            .collect(Collectors.toMap(UnitGraphModel::id, Function.identity()));
    }
}

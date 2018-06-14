package fr.gouv.vitam.metadata.core.database.collections;

import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.ID;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataDocument.OG;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataDocument.ORIGINATING_AGENCIES;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataDocument.ORIGINATING_AGENCY;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataDocument.UP;
import static fr.gouv.vitam.metadata.core.database.collections.Unit.GRAPH;
import static fr.gouv.vitam.metadata.core.database.collections.Unit.PARENT_ORIGINATING_AGENCIES;
import static fr.gouv.vitam.metadata.core.database.collections.Unit.UNITDEPTHS;
import static fr.gouv.vitam.metadata.core.database.collections.Unit.UNITUPS;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.mongodb.BasicDBObject;

import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;

public class GraphLoader implements AutoCloseable {

    private static final Pattern pattern = Pattern.compile("loadAll failed to return a value for (.*)");

    private final MongoDbMetadataRepository<Unit> mongoDbMetadataRepository;

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

    private LoadingCache<String, Unit> unitLoadingCache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .refreshAfterWrite(1, TimeUnit.MINUTES)
        .build(
            new CacheLoader<String, Unit>() {
                public Unit load(String key) {
                    return loadAll(Collections.singleton(key)).get(key);
                }

                public Map<String, Unit> loadAll(Iterable<? extends String> keys) {
                    Collection<Unit> units = mongoDbMetadataRepository.selectByIds(keys, UNIT_VITAM_GRAPH_PROJECTION);
                    return units.stream().collect(Collectors.toMap(Unit::getId, Function.identity()));
                }
            }
        );


    public GraphLoader(MongoDbMetadataRepository<Unit> mongoDbMetadataRepository) {
        this.mongoDbMetadataRepository = mongoDbMetadataRepository;
    }

    public Iterable<Unit> loadGraphs(Iterable<? extends String> ids) throws MetaDataNotFoundException {
        try {
            ImmutableMap<String, Unit> all = unitLoadingCache.getAll(ids);
            return all.values();
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

}

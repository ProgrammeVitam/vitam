package fr.gouv.vitam.ihmrecette.appserver.populate;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.objectgroup.FileInfoModel;
import fr.gouv.vitam.common.model.unit.DescriptiveMetadataModel;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class UnitGraph {

    private final LoadingCache<String, UnitModel> cache;

    public UnitGraph(MetadataRepository metadataRepository) {

        cache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(new CacheLoader<String, UnitModel>() {
                @Override
                public UnitModel load(String key) {
                    Optional<UnitModel> unitById = metadataRepository.findUnitById(key);
                    return unitById.orElseThrow(() -> new RuntimeException("rootId not present in database: " + key));
                }
            });
    }

    /**
     * Create a graph
     * @param i used to generate dynamic metadata
     * @param rootId the id of a root unit to use for attachment (can be null) 
     * @param tenantId tenant identifier
     * @param originatingAgency the origination agency
     * @param withGot if true, one got is created for every unit
     * @return new UnitGotModel (unitModel, gotModel)
     */
    public UnitGotModel createGraph(int i, String rootId, int tenantId, String originatingAgency, boolean withGot) {
        
        // id of the unit.
        String uuid = GUIDFactory.newUnitGUID(tenantId).toString();
        
        // get rootUnit if any
        UnitModel rootUnit = null;
        if(rootId != null) {
            rootUnit = cache.getUnchecked(rootId);
        }
        
        // create unitModel
        UnitModel unitModel = createUnitModel(uuid, originatingAgency, rootId, tenantId, 
                DescriptiveMetadataGenerator.generateDescriptiveMetadataModel(i), rootUnit);
        
        if(!withGot) {
            return new UnitGotModel(unitModel);  
        }

        // id of the got.
        String guid = GUIDFactory.newObjectGroupGUID(tenantId).toString();

        // update unitModel
        unitModel.setOg(guid);
                
        // create gotModel
        ObjectGroupModel gotModel = createObjectGroupModel(guid, tenantId, 
                DescriptiveMetadataGenerator.generateFileInfoModel(i), unitModel); 

        // return (unit, got) as UnitGotModel
        return new UnitGotModel(unitModel, gotModel);
    }

    /**
     * Create a unitModel
     * 
     * @param uuid Guid
     * @param originatingAgency originating agency
     * @param rootId to use for up
     * @param tenantId tenant identifier
     * @param descriptiveMetadataModel MetadataModel
     * @param rootUnit parentUnit Model
     * @return a UnitModel
     */
    private UnitModel createUnitModel(String uuid, String originatingAgency, String rootId, int tenantId,
                                      DescriptiveMetadataModel descriptiveMetadataModel, UnitModel rootUnit) {
        UnitModel unitModel = new UnitModel();

        unitModel.setId(uuid);
        unitModel.setSp(originatingAgency);
        unitModel.getSps().add(originatingAgency);

        unitModel.getUp().add(rootId);
        unitModel.setTenant(tenantId);
        unitModel.setDescriptiveMetadataModel(descriptiveMetadataModel);

        // setup graph info
        if(rootUnit != null) {
            unitModel.getSps().addAll(rootUnit.getSps());

            unitModel.getUs().addAll(rootUnit.getUs());
            unitModel.getUs().add(rootId);

            // calculate uds
            for (String s : rootUnit.getUds().keySet()) {
                unitModel.getUds().merge(s, rootUnit.getUds().get(s) + 1, Math::min);
            }

            unitModel.getUds().put(rootId, 1);
        }
        
        return unitModel;
    }

    /**
     * Create a GotModel
     * 
     * @param guid GUID 
     * @param tenantId tenant identifier
     * @param fileInfoModel fileInfo
     * @param parentUnit unitModel of the parent AU
     * @return a ObjectGroupModel
     */
    private ObjectGroupModel createObjectGroupModel(String guid, int tenantId, FileInfoModel fileInfoModel, 
                                                    UnitModel parentUnit){
        ObjectGroupModel gotModel = new ObjectGroupModel();
        
        gotModel.setId(guid);
        gotModel.setTenant(tenantId);
        gotModel.getUp().add(parentUnit.getId());
        gotModel.setFileInfoModel(fileInfoModel);
        gotModel.setQualifiers(null); // Use to inject BDO and Physical Object metadatas

        gotModel.setSp(parentUnit.getSp());
        gotModel.getSps().addAll(parentUnit.getSps());
        
        return gotModel;
    }

}

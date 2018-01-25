package fr.gouv.vitam.ihmrecette.appserver.populate;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.objectgroup.FileInfoModel;
import fr.gouv.vitam.common.model.unit.DescriptiveMetadataModel;
import fr.gouv.vitam.common.model.unit.ManagementModel;
import fr.gouv.vitam.common.model.unit.RuleCategoryModel;
import fr.gouv.vitam.common.model.unit.RuleModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
     * @param populateModel model of populate service
     * @return new UnitGotModel (unitModel, gotModel)
     */
    public UnitGotModel createGraph(int i, PopulateModel populateModel) {

        final int tenantId = populateModel.getTenant();
        final boolean withGot = populateModel.isWithGots();
        // id of the unit.
        String uuid = GUIDFactory.newUnitGUID(tenantId).toString();
        
        // create unitModel
        UnitModel unitModel = createUnitModel(uuid, DescriptiveMetadataGenerator.generateDescriptiveMetadataModel(i),
            populateModel, i);
        
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
     * @param descriptiveMetadataModel MetadataModel
     * @param populateModel populate Model
     * @return a UnitModel
     */
    private UnitModel createUnitModel(String uuid, DescriptiveMetadataModel descriptiveMetadataModel,
        PopulateModel populateModel, int unitNumber) {

        final int tenantId = populateModel.getTenant();
        final String rootId = populateModel.getRootId();
        final String originatingAgency = populateModel.getSp();

        // get rootUnit if any
        UnitModel rootUnit = null;
        if(rootId != null) {
            rootUnit = cache.getUnchecked(rootId);
        }

        UnitModel unitModel = new UnitModel();

        unitModel.setId(uuid);
        unitModel.setSp(originatingAgency);
        unitModel.getSps().add(originatingAgency);

        unitModel.getUp().add(rootId);
        unitModel.setTenant(tenantId);
        unitModel.setDescriptiveMetadataModel(descriptiveMetadataModel);

        if (populateModel.isWithRules()) {
            ManagementModel management = new ManagementModel();
            Map<String, Integer> ruleMap = populateModel.getRuleTemplatePercent();
            for (String rule : ruleMap.keySet()) {
                int numberUnitWithRUle = ruleMap.get(rule)*populateModel.getBulkSize()/100;
                if (unitNumber < numberUnitWithRUle) {
                    RuleModel ruleModel = new RuleModel(rule, "2017-01-01");
                    // Set default rule duration 50 years
                    // TODO search rule duration and calculate end date ??
                    ruleModel.setEndDate("2067-01-01");
                    List<RuleModel> rules = new ArrayList<>();
                    rules.add(ruleModel);
                    RuleCategoryModel ruleCategory = new RuleCategoryModel();
                    ruleCategory.setRules(rules);
                    management.setRuleCategoryModel(ruleCategory, MetadataRepository.getRuleCategoryByRuleId(rule));
                }
            }
            unitModel.setManagementModel(management);
        }

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

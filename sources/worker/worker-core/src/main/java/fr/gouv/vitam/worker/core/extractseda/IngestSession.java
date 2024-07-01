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
package fr.gouv.vitam.worker.core.extractseda;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.unit.GotObj;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParameters;
import fr.gouv.vitam.worker.common.utils.DataObjectDetail;
import fr.gouv.vitam.worker.common.utils.DataObjectInfo;
import fr.gouv.vitam.worker.core.utils.FastValueAccessMap;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IngestSession {

    private final ObjectNode archiveUnitTree = JsonHandler.createObjectNode();
    private final Map<String, String> unitIdToGuid = new HashMap<>();
    private final Map<String, String> guidToUnitId = new HashMap<>();
    private final Map<String, String> unitIdToGroupId = new HashMap<>();
    private final Map<String, List<String>> objectGroupIdToUnitId = new HashMap<>();
    private final Map<String, String> dataObjectIdToObjectGroupId = new FastValueAccessMap<>();
    private final Map<String, GotObj> dataObjectIdWithoutObjectGroupId = new HashMap<>();
    private final Map<String, LogbookLifeCycleParameters> guidToLifeCycleParameters = new HashMap<>();
    private final Set<String> existingUnitGuids = new HashSet<>();
    private final Map<String, String> objectGroupIdToGuid = new HashMap<>();
    private final Map<String, String> dataObjectIdToGuid = new HashMap<>();
    private final Map<String, Set<String>> unitIdToSetOfRuleId = new HashMap<>();
    private final Map<String, StringWriter> mngtMdRuleIdToRulesXml = new HashMap<>();
    private final List<String> originatingAgencies = new ArrayList<>();
    private final Map<String, JsonNode> existingGOTs = new HashMap<>();
    private final Map<String, String> existingUnitIdWithExistingObjectGroup = new HashMap<>();
    private final Map<String, Boolean> isThereManifestRelatedReferenceRemained = new HashMap<>();
    private final Map<String, String> existingGOTGUIDToNewGotGUIDInAttachment = new HashMap<>();
    private final Map<String, List<String>> objectGroupIdToDataObjectId = new HashMap<>();
    private final Map<String, DataObjectInfo> objectGuidToDataObject = new HashMap<>();
    private final Map<String, DataObjectDetail> dataObjectIdToDetailDataObject = new HashMap<>();
    private final Map<String, Boolean> dataObjectGroupMasterMandatory = new HashMap<>();
    private final Set<String> physicalDataObjetsGuids = new HashSet<>();
    private final Map<String, Long> fileWithParmsFromFolder = new HashMap<>();

    private final Multimap<String, String> usageToObjectGroupId = HashMultimap.create();

    public ObjectNode getArchiveUnitTree() {
        return archiveUnitTree;
    }

    public Map<String, String> getUnitIdToGuid() {
        return unitIdToGuid;
    }

    public Map<String, String> getGuidToUnitId() {
        return guidToUnitId;
    }

    public Map<String, String> getUnitIdToGroupId() {
        return unitIdToGroupId;
    }

    public Map<String, List<String>> getObjectGroupIdToUnitId() {
        return objectGroupIdToUnitId;
    }

    public Map<String, String> getDataObjectIdToObjectGroupId() {
        return dataObjectIdToObjectGroupId;
    }

    public Map<String, GotObj> getDataObjectIdWithoutObjectGroupId() {
        return dataObjectIdWithoutObjectGroupId;
    }

    public Map<String, LogbookLifeCycleParameters> getGuidToLifeCycleParameters() {
        return guidToLifeCycleParameters;
    }

    public Set<String> getExistingUnitGuids() {
        return existingUnitGuids;
    }

    public Map<String, String> getObjectGroupIdToGuid() {
        return objectGroupIdToGuid;
    }

    public Map<String, String> getDataObjectIdToGuid() {
        return dataObjectIdToGuid;
    }

    public Map<String, Set<String>> getUnitIdToSetOfRuleId() {
        return unitIdToSetOfRuleId;
    }

    public Map<String, StringWriter> getMngtMdRuleIdToRulesXml() {
        return mngtMdRuleIdToRulesXml;
    }

    public List<String> getOriginatingAgencies() {
        return originatingAgencies;
    }

    public Map<String, JsonNode> getExistingGOTs() {
        return existingGOTs;
    }

    public Map<String, String> getExistingUnitIdWithExistingObjectGroup() {
        return existingUnitIdWithExistingObjectGroup;
    }

    public Map<String, Boolean> getIsThereManifestRelatedReferenceRemained() {
        return isThereManifestRelatedReferenceRemained;
    }

    public Map<String, String> getExistingGOTGUIDToNewGotGUIDInAttachment() {
        return existingGOTGUIDToNewGotGUIDInAttachment;
    }

    public Map<String, List<String>> getObjectGroupIdToDataObjectId() {
        return objectGroupIdToDataObjectId;
    }

    public Map<String, DataObjectInfo> getObjectGuidToDataObject() {
        return objectGuidToDataObject;
    }

    public Map<String, DataObjectDetail> getDataObjectIdToDetailDataObject() {
        return dataObjectIdToDetailDataObject;
    }

    public Map<String, Boolean> getDataObjectGroupMasterMandatory() {
        return dataObjectGroupMasterMandatory;
    }

    public Set<String> getPhysicalDataObjetsGuids() {
        return physicalDataObjetsGuids;
    }

    public Map<String, Long> getFileWithParmsFromFolder() {
        return fileWithParmsFromFolder;
    }

    public Multimap<String, String> getUsageToObjectGroupId() {
        return usageToObjectGroupId;
    }
}

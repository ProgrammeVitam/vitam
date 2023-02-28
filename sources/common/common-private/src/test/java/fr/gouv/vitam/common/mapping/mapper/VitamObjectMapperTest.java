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
package fr.gouv.vitam.common.mapping.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.culture.archivesdefrance.seda.v2.LevelType;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.unit.ArchiveUnitInternalModel;
import fr.gouv.vitam.common.model.unit.DescriptiveMetadataModel;
import fr.gouv.vitam.common.model.unit.ManagementModel;
import fr.gouv.vitam.common.model.unit.RuleCategoryModel;
import fr.gouv.vitam.common.model.unit.RuleModel;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class VitamObjectMapperTest {

    @Test
    public void buildDeserializationObjectMapper() throws Exception {
        ObjectMapper objectMapper = VitamObjectMapper.buildDeserializationObjectMapper();
        try (InputStream is = PropertiesUtils.getResourceAsStream("objectmapper/unit.json")) {
            JsonNode json = JsonHandler.getFromInputStream(is);
            ArchiveUnitInternalModel archiveUnitModel = objectMapper.treeToValue(json, ArchiveUnitInternalModel.class);
            assertThat(archiveUnitModel.getId()).isEqualTo("aeaqaaaaaahk64ngaaudyamgscsjluqaaabq");
            assertThat(archiveUnitModel.getManagement().getDissemination().getRules()).extracting(RuleModel::getRule)
                .contains("DIS-00001");
        } catch (InvalidParseOperationException | JsonProcessingException e) {
            fail("Cannot deserialize json", e);
        }
    }

    @Test
    public void buildSerializationObjectMapper() throws Exception {
        ArchiveUnitInternalModel archiveUnitRoot = new ArchiveUnitInternalModel();
        archiveUnitRoot.setId("aeaqaaaaaahk64ngaaudyamgscsjluqaaabq");
        DescriptiveMetadataModel descriptiveMetadataModel = new DescriptiveMetadataModel();
        descriptiveMetadataModel.setDescriptionLevel(LevelType.RECORD_GRP);
        descriptiveMetadataModel.setTitle("Oxford Street");
        descriptiveMetadataModel.setStartDate("2014-12-07T09:52:56");
        descriptiveMetadataModel.setEndDate("2014-12-07T09:52:56");
        archiveUnitRoot.setDescriptiveMetadataModel(descriptiveMetadataModel);
        ManagementModel managementModel = new ManagementModel();
        RuleCategoryModel dissemination = new RuleCategoryModel();
        RuleModel rule1 = new RuleModel();
        rule1.setRule("DIS-00001");
        rule1.setStartDate("2008-07-14");
        rule1.setEndDate("2033-07-14");
        dissemination.setRules(List.of(rule1));
        managementModel.setDissemination(dissemination);
        archiveUnitRoot.setManagement(managementModel);

        try (InputStream is = PropertiesUtils.getResourceAsStream("objectmapper/unit.json")) {
            ObjectMapper objectMapper = VitamObjectMapper.buildSerializationObjectMapper();
            JsonNode jsonNode = objectMapper.convertValue(archiveUnitRoot, JsonNode.class);
            JsonNode fromInputStream = JsonHandler.getFromInputStream(is);
            JsonAssert.assertJsonEquals(jsonNode, fromInputStream);
        }
    }
}
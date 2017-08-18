package fr.gouv.vitam.access.internal.core;

import static org.assertj.core.api.Assertions.assertThat;

import fr.gouv.culture.archivesdefrance.seda.v2.ArchiveUnitType;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.common.model.unit.DescriptiveMetadataModel;
import fr.gouv.vitam.common.model.unit.RuleCategoryModel;
import org.junit.Test;

public class ArchiveUnitMapperTest {

    @Test
    public void should_map_unit_with_empty_fields() throws Exception {
        //Given
        ArchiveUnitMapper archiveUnitMapper = new ArchiveUnitMapper();
        ArchiveUnitModel archiveUnitModel = new ArchiveUnitModel();
        archiveUnitModel.setId("1234564");
        archiveUnitModel.setDescriptiveMetadataModel(new DescriptiveMetadataModel());
        archiveUnitModel.getManagement().setStorage(new RuleCategoryModel());

        // When
        ArchiveUnitType archiveUnitType = archiveUnitMapper.map(archiveUnitModel);

        // Then
        assertThat(archiveUnitType.getId()).isEqualTo("1234564");
    }

}
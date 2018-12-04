package fr.gouv.vitam.worker.core.plugin.preservation;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.model.objectgroup.FormatIdentificationModel;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupModel;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Optional;

import static fr.gouv.vitam.common.json.JsonHandler.getFromFile;
import static fr.gouv.vitam.common.json.JsonHandler.getFromString;
import static fr.gouv.vitam.worker.core.plugin.preservation.PreservationPreparationHelper.getFormatModelFromObjectGroupModelGivenQualifierAndVersion;
import static org.assertj.core.api.Assertions.assertThat;


public class PreservationPreparationHelperTest {

    private ObjectGroupResponse objectGroupModel;

    @Before
    public void setUp() throws Exception {
        File resourceFile = PropertiesUtils.getResourceFile("preservation/objectGoup.json");
        objectGroupModel = getFromFile(resourceFile, ObjectGroupResponse.class);

    }

    @Test
    public void shouldGetBinaryMasterVersion1Model() {

        Optional<FormatIdentificationModel> formatIdentification =
            getFormatModelFromObjectGroupModelGivenQualifierAndVersion(objectGroupModel, "BinaryMaster", "FIRST");

        assertThat(formatIdentification).isPresent();
        FormatIdentificationModel formatIdentificationModel = formatIdentification.get();

        assertThat(formatIdentificationModel.getFormatId()).isEqualTo("fmt/354");
    }

    @Test
    public void shouldGetDisseminationLastVersion() {

        Optional<FormatIdentificationModel> formatIdentification =
            getFormatModelFromObjectGroupModelGivenQualifierAndVersion(objectGroupModel, "Dissemination", "LAST");

        assertThat(formatIdentification).isPresent();
        FormatIdentificationModel formatIdentificationModel = formatIdentification.get();

        assertThat(formatIdentificationModel.getFormatId()).isEqualTo("fmt/355");
    }

}

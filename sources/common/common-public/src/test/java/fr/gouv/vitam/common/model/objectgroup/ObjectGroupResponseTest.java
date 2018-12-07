package fr.gouv.vitam.common.model.objectgroup;

import fr.gouv.vitam.common.PropertiesUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static fr.gouv.vitam.common.json.JsonHandler.getFromFile;
import static org.assertj.core.api.Assertions.assertThat;

public class ObjectGroupResponseTest {

    private ObjectGroupResponse objectGroupModel;

    @Before
    public void setUp() throws Exception {
        File resourceFile = PropertiesUtils.getResourceFile("preservation/objectGoup.json");
        objectGroupModel = getFromFile(resourceFile, ObjectGroupResponse.class);
    }

    @Test
    public void shouldGetBinaryMasterVersion1Model() {

       FormatIdentificationModel formatIdentification =
            objectGroupModel.getFirstVersionsModel("BinaryMaster").get().getFormatIdentification();

        assertThat(formatIdentification.getFormatId()).isEqualTo("fmt/354");
    }

    @Test
    public void shouldGetDisseminationLastVersion() {

        FormatIdentificationModel formatIdentification =
            objectGroupModel.getLastVersionsModel("Dissemination").get().getFormatIdentification();

        assertThat(formatIdentification.getFormatId()).isEqualTo("fmt/355");
    }

}

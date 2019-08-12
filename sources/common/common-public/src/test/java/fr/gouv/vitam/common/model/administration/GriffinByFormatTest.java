package fr.gouv.vitam.common.model.administration;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.model.administration.preservation.GriffinByFormat;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;

import static fr.gouv.vitam.common.json.JsonHandler.getFromFile;
import static fr.gouv.vitam.common.json.JsonHandler.getFromString;
import static fr.gouv.vitam.common.model.administration.ActionTypePreservation.ANALYSE;
import static fr.gouv.vitam.common.model.administration.ActionTypePreservation.GENERATE;
import static org.assertj.core.api.Assertions.assertThat;

public class GriffinByFormatTest {

    @Test
    public void shouldGenerateGriffinByFormat() throws InvalidParseOperationException, FileNotFoundException {

        File resourceFile = PropertiesUtils.getResourceFile("preservation/preservation.json");

        GriffinByFormat griffinByFormat = getFromFile(resourceFile, GriffinByFormat.class);

        assertThat(griffinByFormat.getMaxSize()).isEqualTo(10000000L);
        assertThat(griffinByFormat.getTimeOut()).isEqualTo(20);
        assertThat(griffinByFormat.getActionDetail().get(0).getType()).isEqualTo(ANALYSE);
        assertThat(griffinByFormat.getActionDetail().get(1).getType()).isEqualTo(GENERATE);

    }
}

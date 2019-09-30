package fr.gouv.vitam.common.database.collections;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class VitamDescriptionResolverTest {

    @Test
    public void test() {

        // Given
        VitamDescriptionType title = new VitamDescriptionType("Title", null, VitamDescriptionType.VitamType.text,
            VitamDescriptionType.VitamCardinality.one, true);
        VitamDescriptionType description =
            new VitamDescriptionType("Description", null, VitamDescriptionType.VitamType.text,
                VitamDescriptionType.VitamCardinality.one, true);
        VitamDescriptionType title_ = new VitamDescriptionType("Title_", null, VitamDescriptionType.VitamType.object,
            VitamDescriptionType.VitamCardinality.one, true);
        VitamDescriptionType title_Pattern =
            new VitamDescriptionType(null, "Title_.[a-z]+", VitamDescriptionType.VitamType.text,
                VitamDescriptionType.VitamCardinality.one, true);

        List<VitamDescriptionType> vitamDescriptionTypes = Arrays.asList(title, description, title_, title_Pattern);

        // When
        VitamDescriptionResolver vitamDescriptionResolver = new VitamDescriptionResolver(vitamDescriptionTypes);
        VitamDescriptionType expectedTitle = vitamDescriptionResolver.resolve("Title");
        VitamDescriptionType expectedTitle_ = vitamDescriptionResolver.resolve("Title_");
        VitamDescriptionType expectedDescription = vitamDescriptionResolver.resolve("Description");
        VitamDescriptionType expectedTitle_Fr = vitamDescriptionResolver.resolve("Title_.fr");
        VitamDescriptionType expectedUnknown = vitamDescriptionResolver.resolve("Unknown");
        VitamDescriptionType expectedUnknown2 = vitamDescriptionResolver.resolve("Title_.1234");
        VitamDescriptionType expectedUnknown3 = vitamDescriptionResolver.resolve("Title_.fr.toto");

        // Then
        assertThat(expectedTitle).isEqualTo(title);
        assertThat(expectedTitle_).isEqualTo(title_);
        assertThat(expectedDescription).isEqualTo(description);
        assertThat(expectedTitle_Fr).isEqualTo(title_Pattern);
        assertThat(expectedUnknown).isNull();
        assertThat(expectedUnknown2).isNull();
        assertThat(expectedUnknown3).isNull();
    }

}

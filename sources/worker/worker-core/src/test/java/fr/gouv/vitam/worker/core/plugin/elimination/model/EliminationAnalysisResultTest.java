package fr.gouv.vitam.worker.core.plugin.elimination.model;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;

public class EliminationAnalysisResultTest {

    @Test
    public void testSerializationWithClassInheritance() throws Exception {

        // Given
        EliminationAnalysisResult eliminationAnalysisResult = new EliminationAnalysisResult();
        eliminationAnalysisResult.setOperationId("opi");
        eliminationAnalysisResult.setGlobalStatus(EliminationGlobalStatus.KEEP);
        eliminationAnalysisResult.setDestroyableOriginatingAgencies(newHashSet("sp1", "sp2"));
        eliminationAnalysisResult.setNonDestroyableOriginatingAgencies(newHashSet("sp3"));
        eliminationAnalysisResult.setExtendedInfo(Arrays.asList(
            new EliminationExtendedInfoKeepAccessSp(),
            new EliminationExtendedInfoAccessLinkInconsistency(
                new EliminationExtendedInfoAccessLinkInconsistencyDetails(
                    "parentUnitId",
                    newHashSet("sp1", "sp2"),
                    newHashSet("sp3")
                ))
        ));

        // When
        String actual = JsonHandler.unprettyPrint(eliminationAnalysisResult);

        // Then
        String expected = IOUtils.toString(PropertiesUtils.getResourceAsStream(
            "EliminationAnalysis/SerializationAndDeserialization.json"), StandardCharsets.UTF_8);
        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    public void testDeserializationWithClassInheritance() throws Exception {

        // Given / When
        EliminationAnalysisResult eliminationAnalysisResult = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("EliminationAnalysis/SerializationAndDeserialization.json"),
            EliminationAnalysisResult.class);

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo("opi");
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.KEEP);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies())
            .containsExactlyInAnyOrder("sp1", "sp2");
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp3");
        assertThat(eliminationAnalysisResult.getExtendedInfo()).hasSize(2);
        assertThat(eliminationAnalysisResult.getExtendedInfo().get(0))
            .isInstanceOf(EliminationExtendedInfoKeepAccessSp.class);
        assertThat(eliminationAnalysisResult.getExtendedInfo().get(1))
            .isInstanceOf(EliminationExtendedInfoAccessLinkInconsistency.class);
        assertThat(eliminationAnalysisResult.getExtendedInfo().get(1))
            .isInstanceOf(EliminationExtendedInfoAccessLinkInconsistency.class);
        EliminationExtendedInfoAccessLinkInconsistency accessLinkInconsistency =
            (EliminationExtendedInfoAccessLinkInconsistency) eliminationAnalysisResult.getExtendedInfo().get(1);
        assertThat(accessLinkInconsistency.getDetails().getParentUnitId()).isEqualTo("parentUnitId");
        assertThat(accessLinkInconsistency.getDetails().getDestroyableOriginatingAgencies())
            .containsExactlyInAnyOrder("sp1", "sp2");
        assertThat(accessLinkInconsistency.getDetails().getNonDestroyableOriginatingAgencies())
            .containsExactlyInAnyOrder("sp3");
    }
}

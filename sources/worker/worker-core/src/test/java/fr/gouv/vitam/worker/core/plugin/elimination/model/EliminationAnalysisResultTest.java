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
        eliminationAnalysisResult.setExtendedInfo(
            Arrays.asList(
                new EliminationExtendedInfoKeepAccessSp(),
                new EliminationExtendedInfoAccessLinkInconsistency(
                    new EliminationExtendedInfoAccessLinkInconsistencyDetails(
                        "parentUnitId",
                        newHashSet("sp1", "sp2"),
                        newHashSet("sp3")
                    )
                ),
                new EliminationExtendedInfoFinalActionInconsistency(newHashSet("sp4")),
                new EliminationExtendedInfoHoldRule(new EliminationExtendedInfoHoldRuleDetails(newHashSet("HOL-00001")))
            )
        );

        // When
        String actual = JsonHandler.unprettyPrint(eliminationAnalysisResult);

        // Then
        String expected = IOUtils.toString(
            PropertiesUtils.getResourceAsStream("EliminationAnalysis/SerializationAndDeserialization.json"),
            StandardCharsets.UTF_8
        );
        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
    }

    @Test
    public void testDeserializationWithClassInheritance() throws Exception {
        // Given / When
        EliminationAnalysisResult eliminationAnalysisResult = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("EliminationAnalysis/SerializationAndDeserialization.json"),
            EliminationAnalysisResult.class
        );

        // Then
        assertThat(eliminationAnalysisResult.getOperationId()).isEqualTo("opi");
        assertThat(eliminationAnalysisResult.getGlobalStatus()).isEqualTo(EliminationGlobalStatus.KEEP);
        assertThat(eliminationAnalysisResult.getDestroyableOriginatingAgencies()).containsExactlyInAnyOrder(
            "sp1",
            "sp2"
        );
        assertThat(eliminationAnalysisResult.getNonDestroyableOriginatingAgencies()).containsExactlyInAnyOrder("sp3");
        assertThat(eliminationAnalysisResult.getExtendedInfo()).hasSize(4);
        assertThat(eliminationAnalysisResult.getExtendedInfo().get(0)).isInstanceOf(
            EliminationExtendedInfoKeepAccessSp.class
        );
        assertThat(eliminationAnalysisResult.getExtendedInfo().get(1)).isInstanceOf(
            EliminationExtendedInfoAccessLinkInconsistency.class
        );
        assertThat(eliminationAnalysisResult.getExtendedInfo().get(2)).isInstanceOf(
            EliminationExtendedInfoFinalActionInconsistency.class
        );
        assertThat(eliminationAnalysisResult.getExtendedInfo().get(3)).isInstanceOf(
            EliminationExtendedInfoHoldRule.class
        );
        EliminationExtendedInfoAccessLinkInconsistency accessLinkInconsistency =
            (EliminationExtendedInfoAccessLinkInconsistency) eliminationAnalysisResult.getExtendedInfo().get(1);
        assertThat(accessLinkInconsistency.getDetails().getParentUnitId()).isEqualTo("parentUnitId");
        assertThat(accessLinkInconsistency.getDetails().getDestroyableOriginatingAgencies()).containsExactlyInAnyOrder(
            "sp1",
            "sp2"
        );
        assertThat(
            accessLinkInconsistency.getDetails().getNonDestroyableOriginatingAgencies()
        ).containsExactlyInAnyOrder("sp3");
        EliminationExtendedInfoFinalActionInconsistency finalActionInconsistency =
            (EliminationExtendedInfoFinalActionInconsistency) eliminationAnalysisResult.getExtendedInfo().get(2);
        assertThat(finalActionInconsistency.getDetails().getOriginatingAgencies()).containsExactlyInAnyOrder("sp4");
        EliminationExtendedInfoHoldRule extendedInfoHoldRule =
            (EliminationExtendedInfoHoldRule) eliminationAnalysisResult.getExtendedInfo().get(3);
        assertThat(extendedInfoHoldRule.getDetails().getHoldRuleIds()).containsExactlyInAnyOrder("HOL-00001");
    }
}

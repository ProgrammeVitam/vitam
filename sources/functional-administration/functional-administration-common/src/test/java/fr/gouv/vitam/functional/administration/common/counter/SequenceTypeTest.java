package fr.gouv.vitam.functional.administration.common.counter;

import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class SequenceTypeTest {

    @Test
    public void fromFunctionalAdminCollections() {

        List<FunctionalAdminCollections> collectionsWithoutSequence =  Arrays.asList(
            FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY,
            FunctionalAdminCollections.VITAM_SEQUENCE);

        List<FunctionalAdminCollections> collectionsWithSequence =
            Arrays.stream(FunctionalAdminCollections.values()).filter(i -> !collectionsWithoutSequence.contains(i)).collect(
                Collectors.toList());

        for (FunctionalAdminCollections functionalAdminCollection : collectionsWithSequence) {
            assertThat(SequenceType.fromFunctionalAdminCollections(functionalAdminCollection).getCollection()).isEqualTo(functionalAdminCollection);
        }

        for (FunctionalAdminCollections functionalAdminCollection : collectionsWithoutSequence) {
            assertThatThrownBy
                (() -> SequenceType.fromFunctionalAdminCollections(functionalAdminCollection))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}

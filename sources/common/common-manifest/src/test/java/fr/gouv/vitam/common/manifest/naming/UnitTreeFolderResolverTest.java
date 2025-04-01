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

package fr.gouv.vitam.common.manifest.naming;

import fr.gouv.vitam.common.exception.ExportException;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UnitTreeFolderResolverTest {

    private static final String GUID1 = "aeeaaaaaaceclx6vabrwyamsac5oaciaaaaq";
    private static final String GUID2 = "aeaqaaaaaaectvzfabamqamsac5wjaqaaada";
    private static final String GUID3 = "aebqaaaaacec3hn6abc5gamr7y4omzyaaaba";
    private static final String GUID4 = "aebaaaaaaaec3hn6abc5gamr7y4omzyaaaaq";
    private static final String GUID5 = "aeaqaaaaaaec3hn6abc5gamr7y4onfyaaaaq";
    private static final String GUID6 = "aeaqaaaaaaec3hn6abc5gamr7y4om3aaaaaq";
    private static final String GUID7 = "aedqaaaaacec3hn6abc5gamr7y4onxiaaaca";
    private static final String GUID8 = "aeeaaaaaacec3hn6ab74mamr7y4ntuiaaaaq";
    private static final String GUID9 = "aedqaaaaacec3hn6abc5gamr7y4onxqaaaaq";
    private static final String GUID10 = "aedqaaaaacec3hn6abc5gamr7y4ovwyaaaaq";
    private static final String GUID11 = "aebaaaaaaaeclkqgabamwamsac5qn7aaaaaq";

    @Test
    public void testInitializeEmptyDotNotThrowAnyException() {
        assertThatCode(() -> new UnitTreeFolderResolver(emptyList())).doesNotThrowAnyException();
    }

    @Test
    public void testInitializeWithMultipleParentUnitsThenKO() {
        assertThatThrownBy(
            () ->
                new UnitTreeFolderResolver(
                    List.of(
                        givenUnit("unit1", "MyUnit1", List.of(), null),
                        givenUnit("unit2", "MyUnit2", List.of(), null),
                        givenUnit("unit3", "MyUnit3", List.of("unit1", "unit2"), "og1")
                    )
                )
        ).isInstanceOf(ExportException.class);
    }

    @Test
    public void testInitializeOKWithOnlyOneParentUnitInExportScopeThenOK() {
        assertThatCode(
            () ->
                new UnitTreeFolderResolver(
                    List.of(
                        givenUnit("unit1", "MyUnit1", List.of(), null),
                        givenUnit("unit3", "MyUnit3", List.of("unit1", "unit2"), "og1")
                    )
                )
        ).doesNotThrowAnyException();
    }

    @Test
    public void testInitializeWithMultipleParentsForGotThenKO() {
        assertThatThrownBy(
            () ->
                new UnitTreeFolderResolver(
                    List.of(
                        givenUnit("unit1", "MyUnit1", List.of(), null),
                        givenUnit("unit2", "MyUnit2", List.of(), null),
                        givenUnit("unit3", "MyUnit3", List.of("unit1", "unit2"), "og1")
                    )
                )
        ).isInstanceOf(ExportException.class);
    }

    @Test
    public void testResolveBasicUnit() throws ExportException {
        UnitTreeFolderResolver resolver = new UnitTreeFolderResolver(
            List.of(givenUnit("unit1", "MyUnit1", emptyList(), "og1"))
        );
        assertThat(resolver.resolve("og1")).isEqualTo("MyUnit1");
    }

    @Test
    public void testResolveBasicUnitIgnoreParentOutOfExportPerimeter() throws ExportException {
        UnitTreeFolderResolver resolver = new UnitTreeFolderResolver(
            List.of(givenUnit("unit1", "MyUnit1", List.of("MyParentUnit1", "MyParentUnit2"), "og1"))
        );
        assertThat(resolver.resolve("og1")).isEqualTo("MyUnit1");
    }

    @Test
    public void testResolveWithUnknownObjectGroupId() throws ExportException {
        UnitTreeFolderResolver resolver = new UnitTreeFolderResolver(
            List.of(givenUnit("unit1", "MyUnit1", List.of("MyParentUnit1", "MyParentUnit2"), "og1"))
        );
        assertThat(resolver.resolve("og1")).isEqualTo("MyUnit1");
    }

    @Test
    public void testTruncateTitles() throws ExportException {
        UnitTreeFolderResolver resolver = new UnitTreeFolderResolver(
            List.of(
                givenUnit(GUID1, "Small title", List.of(), "og1"),
                givenUnit(
                    GUID2,
                    "My unit title that is really long, but does not goes over the 100 character limit",
                    List.of(GUID1),
                    "og2"
                ),
                givenUnit(
                    GUID3,
                    "My unit title that is really very very long so that it goes over max the 100 character limit and with be truncated",
                    List.of(GUID1),
                    "og3"
                ),
                givenUnit(
                    GUID4,
                    "My root unit title that is really very very long so that it goes over max the 100 character limit and with be truncated",
                    List.of(),
                    "og4"
                )
            )
        );
        assertThat(resolver.resolve("og1")).isEqualTo("Small_title");
        assertThat(resolver.resolve("og2")).isEqualTo(
            "Small_title/My_unit_title_that_is_really_long__but_does_not_goes_over_the_100_character_limit"
        );
        assertThat(resolver.resolve("og3")).isEqualTo(
            "Small_title/My_unit_title_that_is_really_very_very_long_so_that_it_goes_ove_aebqaaaaacec3hn6abc5gamr7y4omzyaaaba"
        );
        assertThat(resolver.resolve("og4")).isEqualTo(
            "My_root_unit_title_that_is_really_very_very_long_so_that_it_goe_aebaaaaaaaec3hn6abc5gamr7y4omzyaaaaq"
        );
    }

    @Test
    public void testResolveDetectDuplicates() throws ExportException {
        UnitTreeFolderResolver resolver = new UnitTreeFolderResolver(
            List.of(
                givenUnit(GUID1, "A", List.of("SomeOtherUnit1"), "og1"),
                givenUnit(GUID2, "A", List.of(), "og2"),
                givenUnit(GUID3, "B", List.of(GUID1), "og3"),
                givenUnit(GUID4, "B", List.of(GUID2), "og4"),
                givenUnit(GUID5, "A", List.of(GUID3), "og5"),
                givenUnit(GUID6, "C", List.of(GUID3), "og6"),
                givenUnit(GUID7, "C", List.of(GUID3), "og7"),
                givenUnit(GUID8, "Ç", List.of(GUID3), "og8"),
                givenUnit(GUID9, "A", List.of(), "og9"),
                givenUnit(GUID10, "B", List.of(GUID9), "og10")
            )
        );
        assertThat(resolver.resolve("og1")).isEqualTo("A");
        assertThat(resolver.resolve("og2")).isEqualTo("A_aeaqaaaaaaectvzfabamqamsac5wjaqaaada");
        assertThat(resolver.resolve("og3")).isEqualTo("A/B");
        assertThat(resolver.resolve("og4")).isEqualTo("A_aeaqaaaaaaectvzfabamqamsac5wjaqaaada/B");
        assertThat(resolver.resolve("og5")).isEqualTo("A/B/A");
        assertThat(resolver.resolve("og6")).isEqualTo("A/B/C");
        assertThat(resolver.resolve("og7")).isEqualTo("A/B/C_aedqaaaaacec3hn6abc5gamr7y4onxiaaaca");
        assertThat(resolver.resolve("og8")).isEqualTo("A/B/C_aeeaaaaaacec3hn6ab74mamr7y4ntuiaaaaq");
        assertThat(resolver.resolve("og9")).isEqualTo("A_aedqaaaaacec3hn6abc5gamr7y4onxqaaaaq");
        assertThat(resolver.resolve("og10")).isEqualTo("A_aedqaaaaacec3hn6abc5gamr7y4onxqaaaaq/B");
    }

    @Test
    public void testResolveComplexTree() throws ExportException {
        UnitTreeFolderResolver resolver = new UnitTreeFolderResolver(
            List.of(
                givenUnit(GUID1, "MyUnit1", List.of("SomeOtherUnit1"), null),
                givenUnit(GUID2, "MyUnit2", List.of("SomeOtherUnit2"), "og2"),
                givenUnit(GUID3, "MyUnit3.", List.of(), null),
                givenUnit(
                    GUID4,
                    "My unit title that is really very very long so that it goes over max the 100 character limit and with be truncated",
                    List.of(GUID3, "SomeOtherUnit3"),
                    "og4"
                ),
                givenUnit(
                    GUID5,
                    "My unit title that is really very very long so that it goes over max the 100 character limit and with also be truncated",
                    List.of(GUID3),
                    "og5"
                ),
                givenUnit(GUID6, "MyDuplicateUnitTitle", List.of(GUID4), "og6"),
                givenUnit(GUID7, "MÿDüplïcätëÜnïtTïtlë", List.of(GUID4), "og7"),
                givenUnit(GUID8, "وحدة أرشيفية 1", List.of(GUID3), "og8"),
                givenUnit(GUID9, "L'hôpital", List.of(GUID2), "og9"),
                givenUnit(
                    GUID10,
                    null,
                    Map.of("ar", "المستشفى", "fr", "L'hôpital", "en", "The hospital"),
                    List.of(GUID3),
                    "og10"
                ),
                givenUnit(
                    GUID11,
                    null,
                    new TreeMap<>(Map.of("ar", "المستشفى", "en", "The hospital")),
                    List.of(GUID3),
                    "og11"
                )
            )
        );

        assertThat(resolver.resolve("og2")).isEqualTo("MyUnit2");
        assertThat(resolver.resolve("og4")).isEqualTo(
            "MyUnit3/My_unit_title_that_is_really_very_very_long_so_that_it_goes_ove_aebaaaaaaaec3hn6abc5gamr7y4omzyaaaaq"
        );
        assertThat(resolver.resolve("og5")).isEqualTo(
            "MyUnit3/My_unit_title_that_is_really_very_very_long_so_that_it_goes_ove_aeaqaaaaaaec3hn6abc5gamr7y4onfyaaaaq"
        );
        assertThat(resolver.resolve("og6")).isEqualTo(
            "MyUnit3/My_unit_title_that_is_really_very_very_long_so_that_it_goes_ove_aebaaaaaaaec3hn6abc5gamr7y4omzyaaaaq/MyDuplicateUnitTitle"
        );
        assertThat(resolver.resolve("og7")).isEqualTo(
            "MyUnit3/My_unit_title_that_is_really_very_very_long_so_that_it_goes_ove_aebaaaaaaaec3hn6abc5gamr7y4omzyaaaaq/MyDuplicateUnitTitle_aedqaaaaacec3hn6abc5gamr7y4onxiaaaca"
        );
        assertThat(resolver.resolve("og8")).isEqualTo("MyUnit3/______________1");
        assertThat(resolver.resolve("og9")).isEqualTo("MyUnit2/L_hopital");
        assertThat(resolver.resolve("og10")).isEqualTo("MyUnit3/L_hopital");
        assertThat(resolver.resolve("og11")).isEqualTo("MyUnit3/________");
    }

    private static ArchiveUnitTreeExportModel givenUnit(
        String guid,
        String title,
        List<String> parents,
        String objectGroupId
    ) {
        return givenUnit(guid, title, Collections.emptyMap(), parents, objectGroupId);
    }

    private static ArchiveUnitTreeExportModel givenUnit(
        String guid,
        String title,
        Map<String, String> title_,
        List<String> parents,
        String objectGroupId
    ) {
        return new ArchiveUnitTreeExportModel(guid, title, title_, parents, objectGroupId);
    }
}

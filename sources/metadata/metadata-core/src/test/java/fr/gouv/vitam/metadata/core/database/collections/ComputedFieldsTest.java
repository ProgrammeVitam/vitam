/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
package fr.gouv.vitam.metadata.core.database.collections;

import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.database.utils.MetadataDocumentHelper;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Computed fields test for units & object groups.
 * All Unit / Got fields constants (static strings) are checked here.
 * This forces any new added field to be double checked with the MetadataDocumentHelper (common-database-private)
 *
 * FIXME : this is only an heuristic validation. We need to group all computed fields in a specific namespace hierarchy (ex. _internal._graph.*) to avoid missing computed fields exclusion.
 */
public class ComputedFieldsTest {

    private static List<String> expectedUnitComputedFields =
        Arrays.asList(Unit.UNITDEPTHS, Unit.UNITUPS, Unit.MINDEPTH, Unit.MAXDEPTH, Unit.GRAPH,
            Unit.PARENT_ORIGINATING_AGENCIES, MetadataDocument.GRAPH_LAST_PERSISTED_DATE,
            MetadataDocument.ORIGINATING_AGENCIES, Unit.COMPUTED_INHERITED_RULES, Unit.VALID_COMPUTED_INHERITED_RULES);

    private static List<String> expectedUnitMainFields = Arrays
        .asList(Unit.MANAGEMENT, Unit.UNIT_TYPE, Unit.STORAGERULE, Unit.APPRAISALRULE, Unit.ACCESSRULE, Unit.OPERATION_TRANSFERS,
            Unit.DISSEMINATIONRULE, Unit.REUSERULE, Unit.CLASSIFICATIONRULE, Unit.RULE, Unit.END, Unit.STORAGERULES,
            Unit.STORAGEEND, Unit.APPRAISALRULES, Unit.APPRAISALEND, Unit.ACCESSRULES, Unit.ACCESSEND,
            Unit.DISSEMINATIONRULES, Unit.DISSEMINATIONEND, Unit.REUSERULES, Unit.REUSEEND, Unit.CLASSIFICATIONRULES,
            Unit.CLASSIFICATIONEND, MetadataDocument.QUALIFIERS, MetadataDocument.NBCHILD, MetadataDocument.TYPE,
            MetadataDocument.UP, MetadataDocument.OG, MetadataDocument.OPS, MetadataDocument.OPI,
            MetadataDocument.ORIGINATING_AGENCY, VitamDocument.ID, VitamDocument.VERSION, VitamDocument.TENANT_ID,
            VitamDocument.SCORE, Unit.HISTORY,
            Unit.ELIMINATION, MetadataDocument.ATOMIC_VERSION,
            VitamDocument.SCORE, VitamDocument.SEDAVERSION, VitamDocument.IMPLEMENTATIONVERSION);

    private static List<String> expectedGotComputedFields =
        Arrays.asList(MetadataDocument.ORIGINATING_AGENCIES, MetadataDocument.GRAPH_LAST_PERSISTED_DATE, Unit.UNITUPS);

    private static List<String> expectedGotMainFields =
        Arrays.asList(ObjectGroup.USAGES, ObjectGroup.STORAGE, ObjectGroup.VERSIONS, ObjectGroup.DATAOBJECTVERSION,
            ObjectGroup.VERSIONS_STORAGE, ObjectGroup.OBJECTCOPIES, ObjectGroup.OBJECTSTORAGE,
            ObjectGroup.OBJECTSTRATEHY, ObjectGroup.OBJECTVERSION, ObjectGroup.OBJECTID, ObjectGroup.OBJECTSIZE,
            ObjectGroup.OBJECTFORMAT, ObjectGroup.OBJECTDIGEST, ObjectGroup.OBJECTDIGEST_VALUE,
            ObjectGroup.OBJECTDIGEST_TYPE, ObjectGroup.COPIES, ObjectGroup.OGDEPTHS, MetadataDocument.QUALIFIERS,
            MetadataDocument.NBCHILD, MetadataDocument.TYPE, MetadataDocument.UP, MetadataDocument.OG,
            MetadataDocument.OPS, MetadataDocument.OPI, MetadataDocument.ORIGINATING_AGENCY, VitamDocument.ID,
            VitamDocument.VERSION, VitamDocument.TENANT_ID, VitamDocument.SCORE, VitamDocument.SEDAVERSION,
            VitamDocument.IMPLEMENTATIONVERSION, MetadataDocument.ATOMIC_VERSION);

    @Test
    public void testUnitComputedFields() throws Exception {

        assertThat(expectedUnitComputedFields)
            .containsExactlyInAnyOrder(MetadataDocumentHelper.getComputedGraphUnitFields().toArray(new String[0]));

        for (Field field : Unit.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            if (!field.getType().equals(String.class)) {
                continue;
            }

            String s = (String) field.get(null);
            if (!expectedUnitComputedFields.contains(s) && !expectedUnitMainFields.contains(s)) {
                Assert.fail("Unknown unit document field name " + field.getName() + "(" + s + ")");
            }
        }
    }

    @Test
    public void testObjectGroupComputedFields() throws Exception {

        assertThat(expectedGotComputedFields)
            .containsExactlyInAnyOrder(
                MetadataDocumentHelper.getComputedGraphObjectGroupFields().toArray(new String[0]));

        for (Field field : ObjectGroup.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            if (!field.getType().equals(String.class)) {
                continue;
            }

            String s = (String) field.get(null);
            if (!expectedGotComputedFields.contains(s) && !expectedGotMainFields.contains(s)) {
                Assert.fail("Unknown got document field name " + field.getName() + "(" + s + ")");
            }
        }
    }
}

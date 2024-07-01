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
package fr.gouv.vitam.common.model.unit;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class ArchiveUnitInternalModelTest {

    @Test
    public void checkInternalExternalFieldMapping() {
        Map<String, Field> externalModelFields = Arrays.stream(ArchiveUnitModel.class.getDeclaredFields()).collect(
            Collectors.toMap(Field::getName, field -> field)
        );

        Map<String, Field> internalModelFields = Arrays.stream(
            ArchiveUnitInternalModel.class.getDeclaredFields()
        ).collect(Collectors.toMap(Field::getName, field -> field));

        assertThat(externalModelFields.keySet())
            .withFailMessage("Expected same field names")
            .isEqualTo(internalModelFields.keySet());
        for (String fieldName : internalModelFields.keySet()) {
            assertThat(internalModelFields.get(fieldName).getType())
                .withFailMessage("Expected same field type for field " + fieldName)
                .isEqualTo(externalModelFields.get(fieldName).getType());
        }
    }
}

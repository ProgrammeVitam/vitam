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

package fr.gouv.vitam.worker.core.impl;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.processing.WorkFlowExecutionContext;
import fr.gouv.vitam.common.model.validations.ValidationError;
import fr.gouv.vitam.worker.common.HandlerIO;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ValidationErrorManagerTest {

    @Test
    public void testPersistValidationErrors() throws Exception {
        // Given
        HandlerIO handlerIO = mock(HandlerIO.class);
        doReturn(PropertiesUtils.getResourceAsStream("ArchiveUnitBeforeErrorValidation.json"))
            .when(handlerIO)
            .getInputStreamFromWorkspace(WorkFlowExecutionContext.VITAM, "Units/unit1.json");
        ValidationErrorManager validationErrorManager = new ValidationErrorManager();
        List<ValidationError> validationErrors = List.of(
            new ValidationError()
                .setEvId("evId1")
                .setOutMessg("outMessg1")
                .setEvDetData("{}")
                .setEvTypeProc("evTypeProc1")
                .setOutDetail("outDetail1"),
            new ValidationError()
                .setEvId("evId2")
                .setOutMessg("outMessg2")
                .setEvDetData("{}")
                .setEvTypeProc("evTypeProc2")
                .setOutDetail("outDetail2")
        );

        // When
        validationErrorManager.handleUnitValidationErrors("unit1", validationErrors, handlerIO);

        // Then
        ArgumentCaptor<JsonNode> unitJsonNodeArgumentCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(handlerIO).transferJsonToWorkspace(
            eq(WorkFlowExecutionContext.VITAM),
            eq("Units"),
            eq("unit1.json"),
            unitJsonNodeArgumentCaptor.capture(),
            eq(false),
            eq(false)
        );

        JsonAssert.assertJsonEquals(
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream("ArchiveUnitAfterErrorValidation.json")),
            unitJsonNodeArgumentCaptor.getValue()
        );
    }
}

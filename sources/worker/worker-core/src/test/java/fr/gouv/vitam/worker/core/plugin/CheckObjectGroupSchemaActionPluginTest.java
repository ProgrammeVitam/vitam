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
package fr.gouv.vitam.worker.core.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.core.validation.MetadataValidationErrorCode;
import fr.gouv.vitam.metadata.core.validation.MetadataValidationException;
import fr.gouv.vitam.metadata.core.validation.OntologyValidator;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.validation.MetadataValidationProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class CheckObjectGroupSchemaActionPluginTest {

    @Rule
    public MockitoRule mockitoJUnit = MockitoJUnit.rule();

    private static final String OBJECT_GROUP_FINAL = "checkObjectGroupSchemaActionPlugin/object-group_FINAL.json";
    private static final String OBJECT_GROUP_OK = "checkObjectGroupSchemaActionPlugin/object-group_OK.json";
    private static final String OBJECT_GROUP_INVALID = "checkObjectGroupSchemaActionPlugin/object-group_invalid.json";
    private static final String OBJECT_GROUP_INVALID_CHAR =
        "checkObjectGroupSchemaActionPlugin/object-group_special_char_KO.json";

    private static final String OBJECT_NAME = "OBJECT_NAME";

    private CheckObjectGroupSchemaActionPlugin checkObjectGroupSchemaActionPlugin;

    @Mock
    private HandlerIO handlerIO;

    @Mock
    private WorkerParameters params;

    @Mock
    private OntologyValidator objectGroupOntologyValidator;

    @InjectMocks
    private MetadataValidationProvider metadataValidationProvider;

    @Before
    public void setUp() throws Exception {
        when(params.getObjectName()).thenReturn("OBJECT_NAME");
        checkObjectGroupSchemaActionPlugin = new CheckObjectGroupSchemaActionPlugin(metadataValidationProvider);
    }

    @Test
    public void givenWorkspaceNotExistWhenExecuteThenReturnResponseFATAL() {
        final ItemStatus response = checkObjectGroupSchemaActionPlugin.execute(params, handlerIO);
        assertEquals(response.getGlobalStatus(), StatusCode.FATAL);
    }

    @Test
    public void givenFinalCorrectObjectGroupJsonWhenExecuteThenReturnResponseOK() throws Exception {
        when(
            handlerIO.getInputStreamFromWorkspace(
                eq(IngestWorkflowConstants.OBJECT_GROUP_FOLDER + File.separator + OBJECT_NAME)
            )
        ).thenReturn(PropertiesUtils.getResourceAsStream(OBJECT_GROUP_FINAL));
        final ItemStatus response = checkObjectGroupSchemaActionPlugin.execute(params, handlerIO);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
    }

    @Test
    public void givenCorrectObjectGroupJsonWhenExecuteThenReturnResponseOK() throws Exception {
        when(
            handlerIO.getInputStreamFromWorkspace(
                eq(IngestWorkflowConstants.OBJECT_GROUP_FOLDER + File.separator + OBJECT_NAME)
            )
        ).thenReturn(PropertiesUtils.getResourceAsStream(OBJECT_GROUP_OK));
        File objectGroupFile = PropertiesUtils.getResourceFile(OBJECT_GROUP_FINAL);
        ObjectNode objectGroupFinal = (ObjectNode) JsonHandler.getFromFile(objectGroupFile);
        when(objectGroupOntologyValidator.verifyAndReplaceFields(any(JsonNode.class))).thenReturn(objectGroupFinal);
        final ItemStatus response = checkObjectGroupSchemaActionPlugin.execute(params, handlerIO);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
    }

    @Test
    public void givenInvalidObjectGroupJsonWhenExecuteThenReturnResponseKO() throws Exception {
        when(
            handlerIO.getInputStreamFromWorkspace(
                eq(IngestWorkflowConstants.OBJECT_GROUP_FOLDER + File.separator + OBJECT_NAME)
            )
        ).thenReturn(PropertiesUtils.getResourceAsStream(OBJECT_GROUP_INVALID));
        when(objectGroupOntologyValidator.verifyAndReplaceFields(any(JsonNode.class))).thenThrow(
            new MetadataValidationException(MetadataValidationErrorCode.ONTOLOGY_VALIDATION_FAILURE, "error")
        );
        final ItemStatus response = checkObjectGroupSchemaActionPlugin.execute(params, handlerIO);
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
        assertEquals(response.getItemId(), "CHECK_OBJECT_GROUP_SCHEMA");
        assertEquals(response.getItemsStatus().get("CHECK_OBJECT_GROUP_SCHEMA").getItemId(), "ONTOLOGY_VALIDATION");
    }

    @Test
    public void givenObjectGrouptWithSpecialCharactersJsonWhenExecuteThenReturnResponseKO() throws Exception {
        when(
            handlerIO.getInputStreamFromWorkspace(
                eq(IngestWorkflowConstants.OBJECT_GROUP_FOLDER + File.separator + OBJECT_NAME)
            )
        ).thenReturn(PropertiesUtils.getResourceAsStream(OBJECT_GROUP_INVALID_CHAR));
        final ItemStatus response = checkObjectGroupSchemaActionPlugin.execute(params, handlerIO);
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
        assertEquals(response.getGlobalOutcomeDetailSubcode(), "INVALID_OBJECT_GROUP");
        assertEquals(response.getItemsStatus().get("CHECK_OBJECT_GROUP_SCHEMA").getItemId(), "OBJECT_GROUP_SANITIZE");
    }
}

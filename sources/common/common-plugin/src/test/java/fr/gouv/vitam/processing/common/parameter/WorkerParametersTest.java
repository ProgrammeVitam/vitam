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
package fr.gouv.vitam.processing.common.parameter;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class WorkerParametersTest {

    @Test
    public void getMapParameters() {
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters();
        assertNotNull(params);
        for (final WorkerParameterName value : WorkerParameterName.values()) {
            params.putParameterValue(value, value.name());
        }
        assertEquals(params.getMapParameters().size(), WorkerParameterName.values().length);

        assertEquals(
            params.getMapParameters().get(WorkerParameterName.currentStep),
            WorkerParameterName.currentStep.name()
        );
        final WorkerParameters params2 = WorkerParametersFactory.newWorkerParameters();
        params2.setFromParameters(params);
        for (final WorkerParameterName value : WorkerParameterName.values()) {
            assertEquals(params.getParameterValue(value), params2.getParameterValue(value));
        }
        final Map<String, String> map = new HashMap<>();
        for (final WorkerParameterName value : WorkerParameterName.values()) {
            map.put(value.name(), params.getParameterValue(value));
        }
        final WorkerParameters params3 = WorkerParametersFactory.newWorkerParameters();
        params3.setMap(map);
        for (final WorkerParameterName value : WorkerParameterName.values()) {
            assertEquals(params.getParameterValue(value), params3.getParameterValue(value));
        }
    }

    @Test
    public void getMandatoriesParameters() {
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters();
        assertNotNull(params);

        final Set<WorkerParameterName> mandatories = params.getMandatoriesParameters();
        assertNotNull(mandatories);

        for (final WorkerParameterName value : mandatories) {
            assertEquals(value.name(), value.toString());
        }

        final Set<WorkerParameterName> mandatoryToAdd = new TreeSet<>();
        mandatoryToAdd.add(WorkerParameterName.currentStep);
        final WorkerParameters params2 = new DefaultWorkerParameters(mandatoryToAdd);
        assertNotNull(params2);
        assertEquals(1, params2.getMandatoriesParameters().size());
        assertEquals(null, params.getContainerName());
    }

    @Test
    public void defaultWorkerParametersConstructorTest() {
        final Map<String, String> map = new HashMap<>();
        map.put(WorkerParameterName.currentStep.name(), WorkerParameterName.currentStep.name());
        final WorkerParameters parameters = new DefaultWorkerParameters(map);
        assertNotNull(parameters);
        assertEquals(1, parameters.getMapParameters().size());
    }

    @Test
    public void getterSetterTest() {
        final WorkerParameters parameters = WorkerParametersFactory.newWorkerParameters();
        assertNotNull(parameters);
        assertEquals(0, parameters.getMapParameters().size());
        parameters.setContainerName("containerName");
        assertEquals(1, parameters.getMapParameters().size());
        assertEquals("containerName", parameters.getContainerName());
        parameters.setContainerName("containerId");
        assertEquals(1, parameters.getMapParameters().size());
        assertEquals("containerId", parameters.getContainerName());
        parameters.setCurrentStep("currentStep");
        assertEquals(2, parameters.getMapParameters().size());
        assertEquals("currentStep", parameters.getCurrentStep());
        parameters.setMetadataRequest("metadataRequest");
        assertEquals(3, parameters.getMapParameters().size());
        assertEquals("metadataRequest", parameters.getMetadataRequest());
        parameters.setObjectId("objectId");
        assertEquals(4, parameters.getMapParameters().size());
        assertEquals("objectId", parameters.getObjectId());
        parameters.setObjectName("objectName");
        assertEquals(5, parameters.getMapParameters().size());
        assertEquals("objectName", parameters.getObjectName());
        parameters.setProcessId("processId");
        assertEquals(6, parameters.getMapParameters().size());
        assertEquals("processId", parameters.getProcessId());
        parameters.setUrlMetadata("urlMetadata");
        assertEquals(7, parameters.getMapParameters().size());
        assertEquals("urlMetadata", parameters.getUrlMetadata());
        parameters.setUrlWorkspace("urlWorkspace");
        assertEquals(8, parameters.getMapParameters().size());
        assertEquals("urlWorkspace", parameters.getUrlWorkspace());
        final GUID guid = GUIDFactory.newGUID();
        parameters.setWorkerGUID(guid.getId());
        assertEquals(9, parameters.getMapParameters().size());
        assertEquals(guid.getId(), parameters.getWorkerGUID());
    }

    @Test
    public void toStringTest() {
        final GUID guid = GUIDFactory.newGUID();
        final String json =
            "{\n" +
            "  \"urlMetadata\" : \"urlMetadata\",\n" +
            "  \"urlWorkspace\" : \"urlWorkspace\",\n" +
            "  \"processId\" : \"processId\",\n" +
            "  \"stepUniqId\" : \"stepUniqId\",\n" +
            "  \"containerName\" : \"containerName\",\n" +
            "  \"objectNameList\" : \"[ \\\"objectName\\\" ]\",\n" +
            "  \"objectId\" : \"objectId\",\n" +
            "  \"workerGUID\" : \"" +
            guid.getId() +
            "\",\n" +
            "  \"metadataRequest\" : \"metadataRequest\",\n" +
            "  \"currentStep\" : \"currentStep\"\n" +
            "}";
        final WorkerParameters parameters = WorkerParametersFactory.newWorkerParameters(
            "processId",
            "stepUniqId",
            "containerName",
            "currentStep",
            Lists.newArrayList("objectName"),
            "urlMetadata",
            "urlWorkspace"
        );
        assertNotNull(parameters);
        parameters.setObjectId("objectId");
        parameters.setMetadataRequest("metadataRequest");
        parameters.setWorkerGUID(guid.getId());
        assertEquals(json, parameters.toString());
    }
}

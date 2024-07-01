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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class WorkerParametersSerializerTest {

    @Test
    public void shouldSerializeWorkerParameters() throws IOException {
        final WorkerParametersSerializer workerParametersSerializer = new WorkerParametersSerializer();
        final WorkerParametersDeserializer workerParametersdeSerializer = new WorkerParametersDeserializer();
        final DefaultWorkerParameters parameters = WorkerParametersFactory.newWorkerParameters();
        final ObjectMapper mapper = new ObjectMapper();
        for (final WorkerParameterName value : WorkerParameterName.values()) {
            parameters.putParameterValue(value, value.name());
        }

        assertNotNull(parameters);

        final StringWriter stringJson = new StringWriter();
        final JsonGenerator generator = new JsonFactory().createGenerator(stringJson);
        final SerializerProvider serializerProvider = mapper.getSerializerProvider();
        workerParametersSerializer.serialize(parameters, generator, serializerProvider);
        generator.flush();
        assertNotNull(stringJson.toString());

        final InputStream stream = new ByteArrayInputStream(stringJson.toString().getBytes(StandardCharsets.UTF_8));
        final JsonParser parser = mapper.getFactory().createParser(stream);
        final DeserializationContext ctxt = mapper.getDeserializationContext();
        final DefaultWorkerParameters parametersDeser =
            (DefaultWorkerParameters) workerParametersdeSerializer.deserialize(parser, ctxt);
        assertTrue(parametersDeser.getContainerName().equals(parameters.getContainerName()));
        assertTrue(parametersDeser.getCurrentStep().equals(parameters.getCurrentStep()));
        assertTrue(parametersDeser.getMetadataRequest().equals(parameters.getMetadataRequest()));
        assertTrue(parametersDeser.getUrlMetadata().equals(parameters.getUrlMetadata()));
        assertTrue(parametersDeser.getUrlWorkspace().equals(parameters.getUrlWorkspace()));
        assertTrue(parametersDeser.getObjectId().equals(parameters.getObjectId()));
        assertTrue(parametersDeser.getObjectName().equals(parameters.getObjectName()));
        assertTrue(parametersDeser.getWorkerGUID().equals(parameters.getWorkerGUID()));
        assertTrue(parametersDeser.getProcessId().equals(parameters.getProcessId()));
    }
}

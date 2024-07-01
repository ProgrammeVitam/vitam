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
package fr.gouv.vitam.logbook.common.parameters;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class LogbookParametersSerializerTest {

    @Test
    public void shouldSerializeOperationParameters() throws IOException {
        final LogbookOperationParameters params = LogbookParameterHelper.newLogbookOperationParameters();
        assertNotNull(params);

        for (final LogbookParameterName value : LogbookParameterName.values()) {
            if (!value.name().equals(LogbookParameterName.events.name())) {
                if (value.name().equals(LogbookParameterName.parentEventIdentifier.name())) {
                    params.putParameterValue(value, null);
                } else {
                    params.putParameterValue(value, value.name());
                }
            }
        }

        final LogbookOperationParametersSerializer logbookParametersSerializer =
            new LogbookOperationParametersSerializer();
        final StringWriter stringJson = new StringWriter();
        final JsonGenerator generator = new JsonFactory().createGenerator(stringJson);
        final SerializerProvider serializerProvider = new ObjectMapper().getSerializerProvider();
        logbookParametersSerializer.serialize(params, generator, serializerProvider);
        generator.flush();
        assertThat(
            stringJson.toString(),
            is(
                equalTo(
                    "{\"eventIdentifier\":\"eventIdentifier\",\"parentEventIdentifier\":null," +
                    "\"eventType\":\"eventType\",\"eventDateTime\":\"eventDateTime\"," +
                    "\"eventIdentifierProcess\":\"eventIdentifierProcess\",\"eventTypeProcess\":\"eventTypeProcess\"," +
                    "\"outcome\":\"outcome\",\"outcomeDetail\":\"outcomeDetail\",\"outcomeDetailMessage\":\"outcomeDetailMessage\"," +
                    "\"agentIdentifier\":\"agentIdentifier\",\"agentIdentifierApplication\":\"agentIdentifierApplication\"," +
                    "\"agentIdentifierPersonae\":\"agentIdentifierPersonae\"," +
                    "\"agentIdentifierApplicationSession\":\"agentIdentifierApplicationSession\",\"eventIdentifierRequest\":\"eventIdentifierRequest\"," +
                    "\"objectIdentifier\":\"objectIdentifier\",\"lifeCycleIdentifier\":\"lifeCycleIdentifier\",\"objectIdentifierRequest\":\"objectIdentifierRequest\"," +
                    "\"objectIdentifierIncome\":\"objectIdentifierIncome\",\"masterData\":\"masterData\",\"rightsStatementIdentifier\":\"rightsStatementIdentifier\"," +
                    "\"agIdExt\":\"agIdExt\",\"eventDetailData\":\"eventDetailData\"}"
                )
            )
        );
    }

    @Test
    public void shouldSerializeUnitParameters() throws IOException {
        final LogbookLifeCycleUnitParameters params = LogbookParameterHelper.newLogbookLifeCycleUnitParameters();
        assertNotNull(params);

        for (final LogbookParameterName value : LogbookParameterName.values()) {
            if (!value.name().equals(LogbookParameterName.events.name())) {
                if (value.name().equals(LogbookParameterName.parentEventIdentifier.name())) {
                    params.putParameterValue(value, null);
                } else {
                    params.putParameterValue(value, value.name());
                }
            }
        }

        final LogbookParametersSerializer logbookParametersSerializer = new LogbookParametersSerializer();
        final StringWriter stringJson = new StringWriter();
        final JsonGenerator generator = new JsonFactory().createGenerator(stringJson);
        final SerializerProvider serializerProvider = new ObjectMapper().getSerializerProvider();
        logbookParametersSerializer.serialize(params, generator, serializerProvider);
        generator.flush();
        assertThat(
            stringJson.toString(),
            is(
                equalTo(
                    "{\"eventIdentifier\":\"eventIdentifier\",\"parentEventIdentifier\":null," +
                    "\"eventType\":\"eventType\",\"eventDateTime\":\"eventDateTime\"," +
                    "\"eventIdentifierProcess\":\"eventIdentifierProcess\",\"eventTypeProcess\":\"eventTypeProcess\"," +
                    "\"outcome\":\"outcome\",\"outcomeDetail\":\"outcomeDetail\",\"outcomeDetailMessage\":\"outcomeDetailMessage\"," +
                    "\"agentIdentifier\":\"agentIdentifier\",\"agentIdentifierApplication\":\"agentIdentifierApplication\"," +
                    "\"agentIdentifierPersonae\":\"agentIdentifierPersonae\"," +
                    "\"agentIdentifierApplicationSession\":\"agentIdentifierApplicationSession\",\"eventIdentifierRequest\":\"eventIdentifierRequest\"," +
                    "\"objectIdentifier\":\"objectIdentifier\",\"lifeCycleIdentifier\":\"lifeCycleIdentifier\",\"objectIdentifierRequest\":\"objectIdentifierRequest\"," +
                    "\"objectIdentifierIncome\":\"objectIdentifierIncome\",\"masterData\":\"masterData\",\"rightsStatementIdentifier\":\"rightsStatementIdentifier\"," +
                    "\"agIdExt\":\"agIdExt\",\"eventDetailData\":\"eventDetailData\"}"
                )
            )
        );
    }
}

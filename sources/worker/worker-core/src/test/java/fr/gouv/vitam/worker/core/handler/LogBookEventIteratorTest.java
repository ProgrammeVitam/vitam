/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.worker.core.handler;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import fr.gouv.vitam.common.xml.XMLInputFactoryUtils;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Test;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LogBookEventIteratorTest {

    @Test
    public void should_create_iterator_of_events() throws Exception {
        // Given
        XMLEventReader xmlEventReader = createXmlEventReader(PropertiesUtils.getResourceAsStream("logbook_events.xml"));

        // When
        LogBookEventIterator logbookEvents = new LogBookEventIterator(xmlEventReader);

        // Then
        assertThat(logbookEvents.stream().collect(Collectors.toList())).hasSize(2);
    }

    @Test
    public void should_create_iterator_OK() throws Exception {
        // Given
        XMLEventReader xmlEventReader = createXmlEventReader(PropertiesUtils.getResourceAsStream("logbook_events.xml"));

        // When
        LogbookEvent event = new LogBookEventIterator(xmlEventReader).next();

        // Then
        assertThat(event).extracting(LogbookEvent::getEvId).isEqualTo("aedqaaaaacglsdgpaa3ikalnhzhaxfiaaaaq");
        assertThat(event).extracting(LogbookEvent::getEvTypeProc).isEqualTo("INGEST");
        assertThat(event).extracting(LogbookEvent::getEvType).isEqualTo("LFC.CHECK_MANIFEST");
        assertThat(event).extracting(LogbookEvent::getEvDateTime).isEqualTo("2019-09-17T08:19:25.206");
        assertThat(event).extracting(LogbookEvent::getOutcome).isEqualTo("OK");
        assertThat(event).extracting(LogbookEvent::getOutDetail).isEqualTo("LFC.CHECK_MANIFEST.OK");
        assertThat(event).extracting(LogbookEvent::getOutMessg).isEqualTo("Succès de la vérification de la cohérence du bordereau de transfert");
        assertThat(event).extracting(LogbookEvent::getEvDetData).isEqualTo("{ }");
        assertThat(event).extracting(LogbookEvent::getAgId).isEqualTo("{\"Name\":\"5ca8d99a4a94\",\"Role\":\"worker\",\"ServerId\":1555631311,\"SiteId\":1,\"GlobalPlatformId\":213454031}");
    }

    @Test
    public void should_return_empty_iterator_when_no_events() throws Exception {
        // Given
        XMLEventReader xmlEventReader = createXmlEventReader(new ByteArrayInputStream("<LogBook></LogBook>".getBytes()));

        // When
        LogBookEventIterator logbookEvents = new LogBookEventIterator(xmlEventReader);

        // Then
        assertThat(logbookEvents.iterator()).isEmpty();
    }

    @Test
    public void should_throw_error_when_not_expected_element_in_events() throws Exception {
        // Given
        XMLEventReader xmlEventReader = createXmlEventReader(PropertiesUtils.getResourceAsStream("logbook_events_unexpected_element.xml"));

        // When
        ThrowingCallable createIterator = () -> new LogBookEventIterator(xmlEventReader).next();

        // Then
        assertThatThrownBy(createIterator).isInstanceOf(VitamRuntimeException.class);
    }

    @Test
    public void should_throw_error_when_missing_mandatory_element_EventDateTime() throws Exception {
        // Given
        XMLEventReader xmlEventReader = createXmlEventReader(PropertiesUtils.getResourceAsStream("logbook_events_missing_mandatory.xml"));

        // When
        ThrowingCallable createIterator = () -> new LogBookEventIterator(xmlEventReader).next();

        // Then
        assertThatThrownBy(createIterator).isInstanceOf(VitamRuntimeException.class);
    }

    private XMLEventReader createXmlEventReader(InputStream xmlFile) throws XMLStreamException {
        XMLInputFactory xmlInputFactory = XMLInputFactoryUtils.newInstance();
        XMLStreamReader rawReader = xmlInputFactory.createXMLStreamReader(xmlFile);
        XMLStreamReader filteredReader = xmlInputFactory.createFilteredReader(rawReader, r -> !r.isWhiteSpace());
        XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(filteredReader);
        if (xmlEventReader.hasNext()) {
            xmlEventReader.nextEvent();
        }
        if (xmlEventReader.hasNext()) {
            xmlEventReader.nextEvent();
        }
        return xmlEventReader;
    }
}

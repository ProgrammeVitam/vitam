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
package fr.gouv.vitam.worker.core.handler;

import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class LogBookEventIterator implements Iterator<LogbookEvent>, Iterable<LogbookEvent> {

    private static final int CHARACTERISTICS = Spliterator.ORDERED | Spliterator.DISTINCT;

    private final XMLEventReader reader;

    public LogBookEventIterator(XMLEventReader reader) {
        this.reader = reader;
    }

    @Override
    public boolean hasNext() {
        if (!reader.hasNext()) {
            return false;
        }
        XMLEvent nextElement = peek();
        if (!nextElement.isStartElement()) {
            return false;
        }
        StartElement element = nextElement.asStartElement();
        return element.getName().getLocalPart().equals("Event");
    }

    @Override
    public LogbookEvent next() {
        LogbookEvent logbookEvent = new LogbookEvent();
        skip("Event");

        logbookEvent.setEvId(tryTake("EventIdentifier"));
        logbookEvent.setEvTypeProc(tryTake("EventTypeCode"));
        logbookEvent.setEvType(tryTake("EventType"));
        logbookEvent.setEvDateTime(take("EventDateTime"));
        skipIfExist("EventDetail");
        logbookEvent.setOutcome(tryTake("Outcome"));
        logbookEvent.setOutDetail(tryTake("OutcomeDetail"));
        logbookEvent.setOutMessg(tryTake("OutcomeDetailMessage"));
        logbookEvent.setEvDetData(tryTake("EventDetailData"));
        logbookEvent.setAgId(tryTake("AgentIdentifier"));
        logbookEvent.setObId(tryTake("ObjectIdentifier"));

        skip("Event");
        return logbookEvent;
    }

    @Override
    public Iterator<LogbookEvent> iterator() {
        return this;
    }

    @Override
    public Spliterator<LogbookEvent> spliterator() {
        return Spliterators.spliteratorUnknownSize(this, CHARACTERISTICS);
    }

    public Stream<LogbookEvent> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    private XMLEvent peek() {
        try {
            return reader.peek();
        } catch (XMLStreamException e) {
            throw new VitamRuntimeException(e);
        }
    }

    private void skip(String expectedName) {
        try {
            XMLEvent element = reader.nextEvent();
            if (element.isStartElement() && !element.asStartElement().getName().getLocalPart().equals(expectedName)) {
                throw new VitamRuntimeException(
                    String.format(
                        "Cannot skip element with name '%s' it should be '%s'.",
                        element.asStartElement().getName().getLocalPart(),
                        expectedName
                    )
                );
            }
            if (element.isEndElement() && !element.asEndElement().getName().getLocalPart().equals(expectedName)) {
                throw new VitamRuntimeException(
                    String.format(
                        "Cannot skip element with name '%s' it should be '%s'.",
                        element.asEndElement().getName().getLocalPart(),
                        expectedName
                    )
                );
            }
        } catch (XMLStreamException e) {
            throw new VitamRuntimeException(e);
        }
    }

    private void skipIfExist(String expectedName) {
        try {
            XMLEvent element = reader.peek();
            if (element.isStartElement() && element.asStartElement().getName().getLocalPart().equals(expectedName)) {
                reader.nextEvent();
            }
        } catch (XMLStreamException e) {
            throw new VitamRuntimeException(e);
        }
    }

    private String take(String expectedName) {
        try {
            XMLEvent start = reader.nextEvent();
            if (!start.isStartElement() || !start.asStartElement().getName().getLocalPart().equals(expectedName)) {
                throw new VitamRuntimeException(
                    String.format(
                        "Cannot take starting element with '%s' it should be '%s'.",
                        start.toString(),
                        expectedName
                    )
                );
            }
            return reader.getElementText();
        } catch (XMLStreamException e) {
            throw new VitamRuntimeException(e);
        }
    }

    private String tryTake(String expectedName) {
        XMLEvent start = peek();
        if (!start.isStartElement() || !start.asStartElement().getName().getLocalPart().equals(expectedName)) {
            return null;
        }
        return take(expectedName);
    }
}

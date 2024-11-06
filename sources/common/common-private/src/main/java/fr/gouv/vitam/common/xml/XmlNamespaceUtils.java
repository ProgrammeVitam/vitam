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

package fr.gouv.vitam.common.xml;

import org.apache.commons.lang3.StringUtils;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.TransformerException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

public final class XmlNamespaceUtils {

    private XmlNamespaceUtils() {
        // Empty private constructor
    }

    public static String parseXmlNamespace(InputStream inputStream) throws XMLStreamException {
        final XMLStreamReader xmlStreamReader = XMLInputFactoryUtils.newInstance().createXMLStreamReader(inputStream);
        try {
            while (xmlStreamReader.hasNext()) {
                if (xmlStreamReader.next() == XMLStreamConstants.START_ELEMENT) {
                    return xmlStreamReader.getNamespaceURI();
                }
            }
            throw new XMLStreamException("Expecting xml document");
        } finally {
            xmlStreamReader.close();
        }
    }

    public static void transformXMLNamespace(
        InputStream inputStream,
        OutputStream outputStream,
        String sourceNamespace,
        String targetNamespace
    ) throws TransformerException {
        try {
            XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
            XMLEventReader eventReader = XMLInputFactoryUtils.newInstance().createXMLEventReader(inputStream);
            XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(outputStream);
            XMLEventFactory eventFactory = XMLEventFactory.newInstance();

            if (StringUtils.isEmpty(sourceNamespace) || StringUtils.isEmpty(targetNamespace)) {
                throw new TransformerException("Source and target namespaces are required");
            }

            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                if (event.isStartElement()) {
                    eventWriter.add(
                        transformStartElement(event.asStartElement(), eventFactory, sourceNamespace, targetNamespace)
                    );
                } else if (event.isEndElement()) {
                    eventWriter.add(
                        transformEndElement(event.asEndElement(), eventFactory, sourceNamespace, targetNamespace)
                    );
                } else {
                    eventWriter.add(event);
                }
            }

            eventReader.close();
            eventWriter.close();
        } catch (XMLStreamException e) {
            throw new TransformerException(e);
        }
    }

    private static StartElement transformStartElement(
        StartElement startElement,
        XMLEventFactory eventFactory,
        String sourceNamespace,
        String targetNamespace
    ) {
        String namespaceURI = startElement.getName().getNamespaceURI();
        Iterator<Namespace> namespaces = transformNamespaces(
            startElement.getNamespaces(),
            eventFactory,
            sourceNamespace,
            targetNamespace
        );

        if (sourceNamespace.equals(namespaceURI)) {
            return eventFactory.createStartElement(
                startElement.getName().getPrefix(),
                targetNamespace,
                startElement.getName().getLocalPart(),
                startElement.getAttributes(),
                namespaces
            );
        }
        return eventFactory.createStartElement(
            startElement.getName().getPrefix(),
            namespaceURI,
            startElement.getName().getLocalPart(),
            startElement.getAttributes(),
            namespaces
        );
    }

    private static EndElement transformEndElement(
        EndElement endElement,
        XMLEventFactory eventFactory,
        String sourceNamespace,
        String targetNamespace
    ) {
        String namespaceURI = endElement.getName().getNamespaceURI();
        if (sourceNamespace.equals(namespaceURI)) {
            return eventFactory.createEndElement(
                endElement.getName().getPrefix(),
                targetNamespace,
                endElement.getName().getLocalPart()
            );
        }
        return endElement;
    }

    private static Iterator<Namespace> transformNamespaces(
        Iterator<Namespace> namespaces,
        XMLEventFactory eventFactory,
        String sourceNamespace,
        String targetNamespace
    ) {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return namespaces.hasNext();
            }

            @Override
            public Namespace next() {
                Namespace ns = namespaces.next();
                if (sourceNamespace.equals(ns.getNamespaceURI())) {
                    return eventFactory.createNamespace(ns.getPrefix(), targetNamespace);
                }
                return ns;
            }
        };
    }
}

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

package fr.gouv.vitam.common.manifest;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.Iterator;

public class VitamXMLStreamWriter implements XMLStreamWriter {

    protected final XMLStreamWriter delegate;

    private static final NamespaceContext emptyNamespaceContext = new NamespaceContext() {
        @Override
        public String getNamespaceURI(String prefix) {
            return "";
        }

        @Override
        public String getPrefix(String namespaceURI) {
            return "";
        }

        @Override
        public Iterator<String> getPrefixes(String namespaceURI) {
            return null;
        }
    };

    public VitamXMLStreamWriter(XMLStreamWriter del) {
        delegate = del;
    }

    public void close() throws XMLStreamException {
        delegate.close();
    }

    public void flush() throws XMLStreamException {
        delegate.flush();
    }

    public NamespaceContext getNamespaceContext() {
        return emptyNamespaceContext;
    }

    public String getPrefix(String uri) throws XMLStreamException {
        return delegate.getPrefix(uri);
    }

    public Object getProperty(String name) {
        return delegate.getProperty(name);
    }

    public void setDefaultNamespace(String uri) throws XMLStreamException {
        delegate.setDefaultNamespace(uri);
    }

    public void setNamespaceContext(NamespaceContext ctx) throws XMLStreamException {
        delegate.setNamespaceContext(ctx);
    }

    public void setPrefix(String pfx, String uri) throws XMLStreamException {
        delegate.setPrefix(pfx, uri);
    }

    public void writeAttribute(String prefix, String uri, String local, String value) throws XMLStreamException {
        delegate.writeAttribute(prefix, uri, local, value);
    }

    public void writeAttribute(String uri, String local, String value) throws XMLStreamException {
        delegate.writeAttribute(uri, local, value);
    }

    public void writeAttribute(String local, String value) throws XMLStreamException {
        delegate.writeAttribute(local, value);
    }

    public void writeCData(String cdata) throws XMLStreamException {
        delegate.writeCData(cdata);
    }

    public void writeCharacters(char[] arg0, int arg1, int arg2) throws XMLStreamException {
        delegate.writeCharacters(arg0, arg1, arg2);
    }

    public void writeCharacters(String text) throws XMLStreamException {
        delegate.writeCharacters(text);
    }

    public void writeComment(String text) throws XMLStreamException {
        delegate.writeComment(text);
    }

    public void writeDefaultNamespace(String uri) throws XMLStreamException {
        delegate.writeDefaultNamespace(uri);
    }

    public void writeDTD(String dtd) throws XMLStreamException {
        delegate.writeDTD(dtd);
    }

    public void writeEmptyElement(String prefix, String local, String uri) throws XMLStreamException {
        delegate.writeEmptyElement(prefix, local, uri);
    }

    public void writeEmptyElement(String uri, String local) throws XMLStreamException {
        delegate.writeEmptyElement(uri, local);
    }

    public void writeEmptyElement(String localName) throws XMLStreamException {
        delegate.writeEmptyElement(localName);
    }

    public void writeEndDocument() throws XMLStreamException {
        delegate.writeEndDocument();
    }

    public void writeEndElement() throws XMLStreamException {
        delegate.writeEndElement();
    }

    public void writeEntityRef(String ent) throws XMLStreamException {
        delegate.writeEntityRef(ent);
    }

    public void writeNamespace(String prefix, String uri) throws XMLStreamException {
        delegate.writeNamespace(prefix, uri);
    }

    public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
        delegate.writeProcessingInstruction(target, data);
    }

    public void writeProcessingInstruction(String target) throws XMLStreamException {
        delegate.writeProcessingInstruction(target);
    }

    public void writeStartDocument() throws XMLStreamException {
        delegate.writeStartDocument();
    }

    public void writeStartDocument(String encoding, String ver) throws XMLStreamException {
        delegate.writeStartDocument(encoding, ver);
    }

    public void writeStartDocument(String ver) throws XMLStreamException {
        delegate.writeStartDocument(ver);
    }

    public void writeStartElement(String prefix, String local, String uri) throws XMLStreamException {
        delegate.writeStartElement(prefix, local, uri);
    }

    public void writeStartElement(String uri, String local) throws XMLStreamException {
        delegate.writeStartElement(uri, local);
    }

    public void writeStartElement(String local) throws XMLStreamException {
        delegate.writeStartElement(local);
    }
}

/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.worker.core.extractseda;

import java.io.IOException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;


/**
 * custom serializer for element
 */

public class ElementSerializer extends StdSerializer<Element> {

    /**
     * constructor
     */
    public ElementSerializer() {
        this(null);
    }

    /**
     * onstructor
     * @param type
     */
    public ElementSerializer(Class<Element> type) {
        super(type);
    }

    /**
     * serialize Element
     * @param value
     * @param jgen
     * @param provider
     * @throws IOException
     */
    @Override
    public void serialize(Element value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        NodeList childNodes = value.getChildNodes();
        jgen.writeStartObject();
        Multimap<String, Node> objectObjectHashMap = ArrayListMultimap.create();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                objectObjectHashMap.put(child.getLocalName(), child);
            }
        }
        for (String s : objectObjectHashMap.keySet()) {
            jgen.writeArrayFieldStart(s);
            for (Node child : objectObjectHashMap.get(s)) {
                switch (child.getNodeType()) {
                    case Node.CDATA_SECTION_NODE:
                    case Node.TEXT_NODE:
                        break;
                    case Node.ELEMENT_NODE:
                        if (child.hasChildNodes() && child.getChildNodes().getLength() > 1) {
                            jgen.writeObject(child);
                            break;
                        }
                        jgen.writeString(child.getTextContent());
                        break;
                }
            }
            jgen.writeEndArray();
        }
        jgen.writeEndObject();
    }

}

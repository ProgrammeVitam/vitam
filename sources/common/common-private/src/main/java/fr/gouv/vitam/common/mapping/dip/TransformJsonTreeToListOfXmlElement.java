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
package fr.gouv.vitam.common.mapping.dip;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static fr.gouv.vitam.common.utils.SupportedSedaVersions.UNIFIED_NAMESPACE;

/**
 * Transform Json Tree To Xml list of xml Element
 */
public class TransformJsonTreeToListOfXmlElement {

    private TransformJsonTreeToListOfXmlElement() {}

    /**
     * Transform Json Tree to list of xml elements
     *
     * @param map Json tree
     * @return the list of elements transformed into xml
     */
    public static List<Element> mapJsonToElement(Map<String, ?> map) {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();
            List<Element> elementToReturn = new ArrayList<>();
            transformMapToElement(elementToReturn::add, document, map);
            return elementToReturn;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Recursive method for Transform Json Tree to list of elements
     *
     * @param consumer consumer to accept
     * @param document xml document
     * @param map Json tree
     */
    private static void transformMapToElement(Consumer<Element> consumer, Document document, Map<String, ?> map) {
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            //skip vitam technical metadata (_opi,..)
            if (entry.getKey().startsWith("#")) {
                continue;
            }

            mapObject(consumer, document, entry.getKey(), entry.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    private static void mapList(Consumer<Element> consumer, Document document, String key, Object value) {
        List<Object> list = (List<Object>) value;
        list.forEach(e -> mapObject(consumer, document, key, e));
    }

    @SuppressWarnings("unchecked")
    private static void mapObject(Consumer<Element> consumer, Document document, String key, Object value) {
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            mapObject(consumer, document, key, value.toString());
        } else if (value instanceof List) {
            mapList(consumer, document, key, value);
        } else if (value instanceof Map) {
            Element childElement = document.createElementNS(UNIFIED_NAMESPACE, key);
            transformMapToElement(childElement::appendChild, document, (Map<String, ?>) value);
            consumer.accept(childElement);
        }
    }

    private static void mapObject(Consumer<Element> consumer, Document document, String key, String value) {
        Element childElement = document.createElementNS(UNIFIED_NAMESPACE, key);
        childElement.appendChild(document.createTextNode(value));
        consumer.accept(childElement);
    }
}

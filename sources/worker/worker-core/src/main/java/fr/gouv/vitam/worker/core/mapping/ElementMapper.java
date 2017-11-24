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
package fr.gouv.vitam.worker.core.mapping;

import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * transform a {@link Element} to a {@link Map}
 */
public class ElementMapper {

    public Map<String, Object> toMap(List<Object> elements) {

        List<Map<String, List>> collect = elements.stream()
            .filter(item -> item instanceof Element)
            .map(item -> {
                Map<String, List> maps = new HashMap<>();
                Element element = (Element) item;
                maps.put(element.getLocalName(), singletonList(elementToMap(element)));
                return maps;
            }).collect(Collectors.toList());

        ListMultimap<String, Object> multimap = ArrayListMultimap.create();
        for (Map<String, List> stringListEntry : collect) {

            for (String s : stringListEntry.keySet()) {
                multimap.putAll(s, stringListEntry.get(s));
            }
        }

        return ((Map) multimap.asMap());
    }

    private Object elementToMap(Element item) {

        NodeList childNodes = item.getChildNodes();

        Multimap<String, Node> objectObjectHashMap = ArrayListMultimap.create();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                objectObjectHashMap.put(child.getLocalName(), child);
            }
            else if (child.getNodeType() == Node.TEXT_NODE) {
                if (Strings.isNullOrEmpty(child.getTextContent().trim())) {
                    continue;
                }
                return child.getTextContent();
            }
        }

        Map<String, List> maps = new HashMap<>();

        for (String s : objectObjectHashMap.keySet()) {
            List<Object> objects = new ArrayList<>();
            for (Node child : objectObjectHashMap.get(s)) {
                switch (child.getNodeType()) {
                    case Node.CDATA_SECTION_NODE:
                    case Node.TEXT_NODE:
                        break;
                    case Node.ELEMENT_NODE:
                        if (child.hasChildNodes() && child.getChildNodes().getLength() >= 1) {
                            objects.add(elementToMap((Element) child));
                            break;
                        }
                        objects.add(child.getTextContent());
                        break;
                }
            }
            maps.put(s, objects);
        }

        return maps;
    }

}

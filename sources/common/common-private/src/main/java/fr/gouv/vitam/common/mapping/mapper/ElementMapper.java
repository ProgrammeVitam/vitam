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
package fr.gouv.vitam.common.mapping.mapper;

import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * transform a {@link Element} to a {@link Map}
 */
public class ElementMapper {

    private ElementMapper() {}

    /**
     * Transform list to map
     *
     * @param elements to be transformed
     * @return the map
     */
    public static Map<String, Object> toMap(List<Object> elements) {
        Map<String, List<Object>> collect = elements
            .stream()
            .filter(item -> item instanceof Element)
            .map(item -> (Element) item)
            .map(item -> new SimpleImmutableEntry<>(item.getLocalName(), elementToMap(item)))
            .collect(
                Collectors.toMap(
                    SimpleImmutableEntry::getKey,
                    SimpleImmutableEntry::getValue,
                    (o, o2) -> Stream.concat(o.stream(), o2.stream()).collect(Collectors.toList())
                )
            );
        return (Map) collect;
    }

    private static List<Object> elementToMap(Element item) {
        NodeList childNodes = item.getChildNodes();
        Multimap<String, Node> objectObjectHashMap = ArrayListMultimap.create();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                objectObjectHashMap.put(child.getLocalName(), child);
            } else if (child.getNodeType() == Node.TEXT_NODE) {
                if (Strings.isNullOrEmpty(child.getTextContent().trim())) {
                    continue;
                }
                return singletonList(child.getTextContent());
            }
        }

        Map<String, List<Object>> maps = new HashMap<>();
        for (String s : objectObjectHashMap.keySet()) {
            List<Object> objects = new ArrayList<>();
            for (Node child : objectObjectHashMap.get(s)) {
                switch (child.getNodeType()) {
                    case Node.CDATA_SECTION_NODE:
                    case Node.TEXT_NODE:
                        break;
                    case Node.ELEMENT_NODE:
                        if (child.hasChildNodes() && child.getChildNodes().getLength() >= 1) {
                            objects.addAll(elementToMap((Element) child));
                            break;
                        }
                        String textContent = child.getTextContent();
                        if (!textContent.isEmpty()) {
                            objects.add(textContent);
                        }
                        break;
                }
            }
            maps.put(s, objects);
        }

        if (maps.isEmpty()) {
            return emptyList();
        }

        return singletonList(maps);
    }
}

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

package fr.gouv.vitam.griffons.imagemagick;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PuidImageType {
    public static final Map<String, String> formatTypes = Collections.unmodifiableMap(
        Stream.of(
            new SimpleImmutableEntry<>("fmt/41", "JPG"),
            new SimpleImmutableEntry<>("fmt/42", "JPG"),
            new SimpleImmutableEntry<>("x-fmt/398", "JPG"),
            new SimpleImmutableEntry<>("x-fmt/390", "JPG"),
            new SimpleImmutableEntry<>("x-fmt/391", "JPG"),
            new SimpleImmutableEntry<>("fmt/645", "JPG"),
            new SimpleImmutableEntry<>("fmt/43", "JPG"),
            new SimpleImmutableEntry<>("fmt/44", "JPG"),
            new SimpleImmutableEntry<>("fmt/112", "JPG"),
            new SimpleImmutableEntry<>("fmt/11", "PNG"),
            new SimpleImmutableEntry<>("fmt/12", "PNG"),
            new SimpleImmutableEntry<>("fmt/13", "PNG"),
            new SimpleImmutableEntry<>("fmt/935", "PNG"),
            new SimpleImmutableEntry<>("fmt/152", "TIF"),
            new SimpleImmutableEntry<>("fmt/367", "TIF"),
            new SimpleImmutableEntry<>("fmt/399", "TIF"),
            new SimpleImmutableEntry<>("fmt/388", "TIF"),
            new SimpleImmutableEntry<>("fmt/387", "TIF"),
            new SimpleImmutableEntry<>("fmt/155", "TIF"),
            new SimpleImmutableEntry<>("fmt/353", "TIF"),
            new SimpleImmutableEntry<>("fmt/154", "TIF"),
            new SimpleImmutableEntry<>("fmt/153", "TIF"),
            new SimpleImmutableEntry<>("fmt/156", "TIF"),
            new SimpleImmutableEntry<>("x-fmt/392", "JP2"),
            new SimpleImmutableEntry<>("x-fmt/178", "PPM"),
            new SimpleImmutableEntry<>("fmt/408", "PPM"),
            new SimpleImmutableEntry<>("fmt/568","WEBP"),
            new SimpleImmutableEntry<>("fmt/567","WEBP"),
            new SimpleImmutableEntry<>("fmt/566","WEBP")
        ).collect(Collectors.toMap(SimpleImmutableEntry::getKey, SimpleImmutableEntry::getValue))
    );

    private PuidImageType() {
    }
}

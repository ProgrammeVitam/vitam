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
package fr.gouv.vitam.storage.engine.common.model;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class DataCategoryTest {

    @Test
    public void checkContainerListDoc() throws Exception {
        // Doc containers are case-insensitive DataCategory enum names mapped by "DataCategory.getByCollectionName()"
        File docListContainersFile = new File("../../../../doc/fr/exploitation/topics/data/container_list.txt");
        List<String> containers = FileUtils.readLines(docListContainersFile, StandardCharsets.UTF_8);

        Set<DataCategory> dataCategorySet = new HashSet<>();
        for (String container : containers) {
            assertThat(container).startsWith("\"");
            assertThat(container).endsWith("\"");
            DataCategory dataCategory = DataCategory.getByCollectionName(
                container.substring(1, container.length() - 1)
            );
            assertThat(dataCategory).isNotNull();
            assertThat(dataCategorySet.add(dataCategory)).isTrue();
        }

        // For historical (bad) reasons DataCategory entries contain duplicate mappings to a "folder" property.
        // Since the folder is used to build actual offer container name, check matching against data category folder.
        Set<String> docContainerFolders = dataCategorySet
            .stream()
            .map(DataCategory::getFolder)
            .collect(Collectors.toSet());
        Set<String> actualContainerFolders = Arrays.stream(DataCategory.values())
            .map(DataCategory::getFolder)
            .collect(Collectors.toSet());

        assertThat(docContainerFolders).containsExactlyInAnyOrderElementsOf(actualContainerFolders);
    }

    @Test
    public void checkOfferResynchronizationContainerListDoc() throws Exception {
        File docOfferSyncContainerListFile = new File(
            "../../../../doc/fr/exploitation/topics/40-resynchronisation.rst"
        );
        File docContainerListFile = new File("../../../../doc/fr/exploitation/topics/data/container_list.txt");

        String doc = FileUtils.readFileToString(docOfferSyncContainerListFile, StandardCharsets.UTF_8);
        String[] offerSyncContainers = StringUtils.substringBetween(doc, "declare -a containers=\"", "\"").split(" ");

        List<String> containers = FileUtils.readLines(docContainerListFile, StandardCharsets.UTF_8)
            .stream()
            .map(container -> container.substring(1, container.length() - 1))
            .collect(Collectors.toList());

        assertThat(offerSyncContainers).containsExactlyInAnyOrderElementsOf(containers);
    }
}

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

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * test class for {@link ElementMapper}
 */
public class ElementMapperTest {

    private static JAXBContext jaxbContext;

    private ElementMapper elementMapper;

    @BeforeClass
    public static void setUp() throws Exception {
        jaxbContext = JAXBContext.newInstance(Content.class);
    }

    @Before
    public void init() throws Exception {
        elementMapper = new ElementMapper();
    }

    @Test
    public void should_map_element_to_hashMap() throws Exception {
        // Given
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Content content = (Content) unmarshaller.unmarshal(getClass().getResourceAsStream(
            "/element_with_complex_data.xml"));

        // When
        Map<String, Object> map = elementMapper.toMap(content.elements);

        // Then
        assertThat(map).hasSize(2);
        assertThat(map.get("metadata")).isNotNull();
        assertThat(map.get("locator")).isNotNull();
        assertThat((List<String>) map.get("locator")).containsExactlyInAnyOrder("tata", "toto");
        List<Map> metadata = (List<Map>) map.get("metadata");
        assertThat(metadata).hasSize(2);

        assertThat(metadata.get(0)).containsEntry("Addressee", newArrayList("a"))
            .containsEntry("Addressee2", newArrayList("a", "b"));

        assertThat(metadata.get(1)).containsEntry("Addressee", newArrayList("c"));
    }

    @XmlRootElement(name = "Content")
    private static class Content {

        @XmlAnyElement(lax = true)
        private List<Object> elements;

    }

}
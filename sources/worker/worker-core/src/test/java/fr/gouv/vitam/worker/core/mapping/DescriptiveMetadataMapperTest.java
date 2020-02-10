/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.worker.core.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import fr.gouv.culture.archivesdefrance.seda.v2.DescriptiveMetadataContentType;
import fr.gouv.culture.archivesdefrance.seda.v2.TextType;
import fr.gouv.vitam.common.model.VitamConstants;
import fr.gouv.vitam.common.model.unit.DescriptiveMetadataModel;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class DescriptiveMetadataMapperTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private DescriptiveMetadataMapper descriptiveMetadataMapper;

    @Test
    public void should_not_fill_title_if_title_has_no_default_lang() {
        // Given
        DescriptiveMetadataContentType metadataContentType = new DescriptiveMetadataContentType();
        TextType title = new TextType();
        title.setLang("fr");
        title.setValue("titre");
        metadataContentType.getTitle().add(title);

        // When
        DescriptiveMetadataModel descriptiveMetadataModel = descriptiveMetadataMapper.map(metadataContentType);

        // Then
        assertThat(descriptiveMetadataModel.getTitle()).isNull();
        assertThat(descriptiveMetadataModel.getTitle_().getTextTypes())
            .hasSize(1)
            .extracting("lang", "value")
            .contains(tuple("fr", "titre"));
    }


    @Test
    public void should_fill_title_if_title_has_default_lang() {
        // Given
        DescriptiveMetadataContentType metadataContentType = new DescriptiveMetadataContentType();
        TextType title = new TextType();
        title.setValue("titre");
        metadataContentType.getTitle().add(title);

        // When
        DescriptiveMetadataModel descriptiveMetadataModel = descriptiveMetadataMapper.map(metadataContentType);

        // Then
        assertThat(descriptiveMetadataModel.getTitle()).isEqualTo("titre");
        assertThat(descriptiveMetadataModel.getTitle_()).isNull();
    }

    @Test
    public void should_fill_description_if_description_has_default_lang() {
        // Given
        DescriptiveMetadataContentType metadataContentType = new DescriptiveMetadataContentType();
        TextType description = new TextType();
        description.setValue("description");
        metadataContentType.getDescription().add(description);

        // When
        DescriptiveMetadataModel descriptiveMetadataModel = descriptiveMetadataMapper.map(metadataContentType);

        // Then
        assertThat(descriptiveMetadataModel.getDescription()).isEqualTo("description");
        assertThat(descriptiveMetadataModel.getDescription_()).isNull();
    }

    @Test
    public void should_not_fill_description_if_description_has_no_default_lang() {
        // Given
        DescriptiveMetadataContentType metadataContentType = new DescriptiveMetadataContentType();
        TextType description = new TextType();
        description.setLang("fr");
        description.setValue("description");
        metadataContentType.getDescription().add(description);

        // When
        DescriptiveMetadataModel descriptiveMetadataModel = descriptiveMetadataMapper.map(metadataContentType);

        // Then
        assertThat(descriptiveMetadataModel.getDescription()).isNull();
        assertThat(descriptiveMetadataModel.getDescription_().getTextTypes())
            .hasSize(1)
            .extracting("lang", "value")
            .contains(tuple("fr", "description"));
    }
}

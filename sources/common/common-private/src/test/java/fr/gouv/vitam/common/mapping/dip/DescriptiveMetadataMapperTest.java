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
package fr.gouv.vitam.common.mapping.dip;

import fr.gouv.culture.archivesdefrance.seda.v2.CustodialHistoryItemType;
import fr.gouv.culture.archivesdefrance.seda.v2.DescriptiveMetadataContentType;
import fr.gouv.culture.archivesdefrance.seda.v2.TextType;
import fr.gouv.vitam.common.model.unit.CustodialHistoryModel;
import fr.gouv.vitam.common.model.unit.DataObjectReference;
import fr.gouv.vitam.common.model.unit.DescriptiveMetadataModel;
import fr.gouv.vitam.common.model.unit.TextByLang;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class DescriptiveMetadataMapperTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private DescriptiveMetadataMapper descriptiveMetadataMapper;

    @Test
    public void should_fill_title_field() throws DatatypeConfigurationException {
        // Given
        DescriptiveMetadataModel descriptiveMetadataModel = new DescriptiveMetadataModel();
        descriptiveMetadataModel.setTitle("titre_default");
        TextType textType_fr = new TextType();
        textType_fr.setLang("en");
        textType_fr.setValue("title");
        TextByLang title_ = new TextByLang(singletonList(textType_fr));

        descriptiveMetadataModel.setTitle_(title_);

        // When
        DescriptiveMetadataContentType contentType =
            descriptiveMetadataMapper.map(descriptiveMetadataModel, new ArrayList<>());

        // Then
        assertThat(contentType.getTitle())
            .hasSize(2)
            .extracting("lang", "value")
            .contains(tuple(null, "titre_default"))
            .contains(tuple("en", "title"));
    }

    @Test
    public void should_fill_description_field() throws DatatypeConfigurationException {
        // Given
        DescriptiveMetadataModel descriptiveMetadataModel = new DescriptiveMetadataModel();
        descriptiveMetadataModel.setDescription("description_default");
        TextType textType_fr = new TextType();
        textType_fr.setLang("en");
        textType_fr.setValue("description");
        TextByLang description_ = new TextByLang(singletonList(textType_fr));

        descriptiveMetadataModel.setDescription_(description_);

        // When
        DescriptiveMetadataContentType contentType =
            descriptiveMetadataMapper.map(descriptiveMetadataModel, new ArrayList<>());

        // Then
        assertThat(contentType.getDescription())
            .hasSize(2)
            .extracting("lang", "value")
            .contains(tuple(null, "description_default"))
            .contains(tuple("en", "description"));
    }

    @Test
    public void should_fill_custodialHistoryFile_field() throws DatatypeConfigurationException {
        // Given
        DescriptiveMetadataModel descriptiveMetadataModel = new DescriptiveMetadataModel();

        CustodialHistoryItemType custodialHistoryItemType = new CustodialHistoryItemType();
        custodialHistoryItemType.setValue("Ce champ est une description de la balise CustodialHistoryItem");
        CustodialHistoryItemType custodialHistoryItemType2 = new CustodialHistoryItemType();
        custodialHistoryItemType2.setValue("Ce champ est une autre description de la balise CustodialHistoryItem");

        List<CustodialHistoryItemType> listeCustodialHistoryItemType = new ArrayList<>();
        listeCustodialHistoryItemType.add(custodialHistoryItemType);
        listeCustodialHistoryItemType.add(custodialHistoryItemType2);

        List<String> custodialHistoryItem =
            listeCustodialHistoryItemType.stream().map(x -> x.getValue()).collect(Collectors.toList());

        DataObjectReference reference = new DataObjectReference();
        reference.setDataObjectReferenceId("ID222");

        CustodialHistoryModel custodialHistoryModel = new CustodialHistoryModel();
        custodialHistoryModel.setCustodialHistoryItem(custodialHistoryItem);
        custodialHistoryModel.setCustodialHistoryFile(reference);

        descriptiveMetadataModel.setCustodialHistory(custodialHistoryModel);

        // When
        DescriptiveMetadataContentType contentType =
            descriptiveMetadataMapper.map(descriptiveMetadataModel, new ArrayList<>());

        // Then
        assertThat(contentType.getCustodialHistory().getCustodialHistoryFile().getDataObjectReferenceId())
            .isEqualTo(reference.getDataObjectReferenceId());
        assertThat(contentType.getCustodialHistory().getCustodialHistoryItem().size()).isEqualTo(2);
    }

}

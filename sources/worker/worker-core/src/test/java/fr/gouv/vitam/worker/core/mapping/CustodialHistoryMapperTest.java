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
package fr.gouv.vitam.worker.core.mapping;

import fr.gouv.culture.archivesdefrance.seda.v2.ArchiveUnitType;
import fr.gouv.vitam.common.mapping.mapper.ElementMapper;
import fr.gouv.vitam.common.model.unit.ArchiveUnitInternalModel;
import fr.gouv.vitam.common.model.unit.ArchiveUnitRoot;
import fr.gouv.vitam.common.model.unit.CustodialHistoryModel;
import fr.gouv.vitam.common.model.unit.DataObjectReference;
import fr.gouv.vitam.common.model.unit.DescriptiveMetadataModel;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.util.List;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

/**
 * test class for {@link ElementMapper}
 */
public class CustodialHistoryMapperTest {

    private static JAXBContext jaxbContext;

    private ArchiveUnitMapper archiveUnitMapper;

    @BeforeClass
    public static void setUp() throws Exception {
        jaxbContext = JAXBContext.newInstance("fr.gouv.culture.archivesdefrance.seda.v2");
    }

    @Before
    public void init() throws Exception {
        archiveUnitMapper = new ArchiveUnitMapper(new DescriptiveMetadataMapper(), new RuleMapper());
    }

    @Test
    public void should_map_element_to_hashMap() throws Exception {
        // Given
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        ArchiveUnitType archiveUnitType = (ArchiveUnitType) unmarshaller.unmarshal(
            getClass().getResourceAsStream("/element_unit_with_custodial_history.xml")
        );

        // When
        ArchiveUnitRoot archiveUnitRoot = archiveUnitMapper.map(
            archiveUnitType,
            "",
            "",
            "operationId",
            "INGEST",
            "sedaVersion"
        );

        // Then
        ArchiveUnitInternalModel archiveUnit = archiveUnitRoot.getArchiveUnit();
        DescriptiveMetadataModel descriptiveMetadataModel = archiveUnit.getDescriptiveMetadataModel();
        assertNotNull(descriptiveMetadataModel);

        CustodialHistoryModel custodialHistoryModel = descriptiveMetadataModel.getCustodialHistory();
        assertNotNull(custodialHistoryModel);

        List<String> custodialHistoryItem = custodialHistoryModel.getCustodialHistoryItem();
        assertNotNull(custodialHistoryItem);
        assertTrue(custodialHistoryItem.size() == 2);

        DataObjectReference custodialHistoryFile = custodialHistoryModel.getCustodialHistoryFile();
        assertNotNull(custodialHistoryFile);
        assertTrue(custodialHistoryFile.getDataObjectGroupReferenceId().equals("ID0007"));
    }
}

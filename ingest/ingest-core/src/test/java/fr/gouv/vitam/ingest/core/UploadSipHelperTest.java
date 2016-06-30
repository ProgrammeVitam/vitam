/**
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
 */
package fr.gouv.vitam.ingest.core;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gouv.vitam.ingest.model.UploadResponseDTO;
import fr.gouv.vitam.ingest.upload.core.UploadSipHelper;


public class UploadSipHelperTest {

    private static Logger LOG = LoggerFactory.getLogger(UploadSipHelperTest.class);
    private UploadSipHelper uploadSipHelper;

    @Before
    public void before() throws Exception {
        uploadSipHelper = new UploadSipHelper();
    }

    @Test
    public void shouldUploadSipHelper() {

        String message = "success";
        String engineStatus = "success";
        String fileName = "Test";
        String vitamCode = "201";
        String vitamStatus = "success";
        Integer httpCode = 200;
        String engineCode = "success";

        final UploadResponseDTO uploadResponseDTO = UploadSipHelper.getUploadResponseDTO(fileName, httpCode, message,
                vitamCode, vitamStatus, engineCode, engineStatus);

        Assert.assertNotNull(uploadResponseDTO);

        Assert.assertEquals("Test", uploadResponseDTO.getFileName());
        Assert.assertEquals(200, uploadResponseDTO.getHttpCode());
        Assert.assertEquals("success", uploadResponseDTO.getMessage());
        Assert.assertEquals("201", uploadResponseDTO.getVitamCode());
        Assert.assertEquals("success", uploadResponseDTO.getVitamStatus());
        Assert.assertEquals("success", uploadResponseDTO.getEngineCode());
        Assert.assertEquals("success", uploadResponseDTO.getEngineStatus());

    }
}



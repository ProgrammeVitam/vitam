/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.ingest.internal.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import fr.gouv.vitam.ingest.internal.model.UploadResponseDTO;

public class UploadResponseDTOTest {

    @Test
    public final void test() {

        final UploadResponseDTO uploadResponseDTO = new UploadResponseDTO();
        assertNull(uploadResponseDTO.getEngineCode());
        assertNull(uploadResponseDTO.getEngineStatus());
        assertNull(uploadResponseDTO.getFileName());
        assertEquals(0, uploadResponseDTO.getHttpCode());
        assertNull(uploadResponseDTO.getMessage());
        assertNull(uploadResponseDTO.getVitamCode());
        assertNull(uploadResponseDTO.getVitamStatus());

        assertNotNull(uploadResponseDTO.setEngineCode("sucess"));
        assertNotNull(uploadResponseDTO.setEngineStatus("sucess"));
        assertNotNull(uploadResponseDTO.setFileName("Test"));
        assertNotNull(uploadResponseDTO.setHttpCode(0));
        assertNotNull(uploadResponseDTO.setMessage("Test"));
        assertNotNull(uploadResponseDTO.setVitamCode("sucess"));
        assertNotNull(uploadResponseDTO.setVitamStatus("sucess"));
    }
}

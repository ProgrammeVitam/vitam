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
package fr.gouv.vitam.common.format.identification;

import fr.gouv.vitam.common.format.identification.model.FormatIdentifierInfo;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierResponse;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.model.VitamConstants;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Mock client implementation for FormatIdentifier
 */
class FormatIdentifierMock implements FormatIdentifier {

    public static final String ZIP = "zip";

    @Override
    public List<FormatIdentifierResponse> analysePath(Path pathToFile) {
        try {
            if (isProbableZip(pathToFile) && isProbableSipFile(pathToFile)) {
                // SIP File
                String formatLitteral = "Zip File";
                String mimeType = "application/zip";
                String formatId = "x-fmt/263";
                String ns = "pronom";

                return Collections.singletonList(new FormatIdentifierResponse(formatLitteral, mimeType, formatId, ns));
            }
        } catch (IOException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }

        String formatLitteral = "Plain Text File";
        String mimeType = "text/plain";
        String formatId = "x-fmt/111";
        String ns = "pronom";

        return Collections.singletonList(new FormatIdentifierResponse(formatLitteral, mimeType, formatId, ns));
    }

    private boolean isProbableZip(Path pathToFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(pathToFile.toFile())) {
            // Zip magic number
            return (fis.read() == 0x50 && fis.read() == 0x4B);
        }
    }

    private boolean isProbableSipFile(Path pathToFile) throws IOException {
        try (ZipFile zipFile = new ZipFile(pathToFile.toFile())) {
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                if (entry.getName().matches(VitamConstants.MANIFEST_FILE_NAME_REGEX)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public FormatIdentifierInfo status() {
        return new FormatIdentifierInfo("1.0", "FormatIdentifierMock");
    }
}

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
package fr.gouv.vitam.tools;

import fr.gouv.vitam.common.guid.GUIDFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class SipTool {

    public static final String REPLACEMENT_STRING = "_REPLACE_ME_";
    public static final String REPLACEMENT_NAME = "_REPLACE_ME_NAME_";
    public static final String REPLACEMENT_VALUE = "_REPLACE_ME_VALUE_";

    public static Path copyAndModifyManifestInZip(
        Path zipPath,
        String text1,
        String replacement1,
        String text2,
        String replacement2
    ) throws IOException {
        File tempFile = Files.createTempFile(GUIDFactory.newGUID().toString(), ".zip").toFile();
        try (InputStream zipFile = new FileInputStream(zipPath.toFile())) {
            Files.copy(zipFile, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        try (FileSystem fs = FileSystems.newFileSystem(tempFile.toPath(), null)) {
            Path source = fs.getPath("manifest.xml");
            Path temp = fs.getPath("manifest_tmp.xml");
            if (Files.exists(temp)) {
                throw new IOException("error");
            }
            Files.move(source, temp);
            streamCopy(temp, source, text1, replacement1, text2, replacement2);
            Files.delete(temp);
        }
        return tempFile.getAbsoluteFile().toPath();
    }

    static void streamCopy(Path src, Path dst, String text1, String replacement1, String text2, String replacement2)
        throws IOException {
        try (
            BufferedReader br = new BufferedReader(new InputStreamReader(Files.newInputStream(src)));
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(dst)))
        ) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.replace(text1, replacement1);
                if (null != text2) {
                    line = line.replace(text2, replacement2);
                }
                bw.write(line);
                bw.newLine();
            }
        }
    }
}

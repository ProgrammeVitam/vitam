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
package fr.gouv.vitam.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * File Utility class
 *
 */
public final class FileUtil {
    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(FileUtil.class);

    private FileUtil() {
        // Unused
    }

    /**
     * @param filename
     * @return the content of the file
     * @throws IOException
     */
    public static final String readFile(final String filename) throws IOException {
        final File file = new File(filename);
        return readFile(file);
    }

    /**
     * @param file
     * @return the content of the file
     * @throws IOException
     */
    public static final String readFile(final File file) throws IOException {
        final StringBuilder builder = new StringBuilder();

        if (file != null && file.canRead()) {
            try {
                final FileInputStream inputStream = new FileInputStream(file);
                final InputStreamReader reader = new InputStreamReader(inputStream);
                final BufferedReader buffered = new BufferedReader(reader);
                String line;
                while ((line = buffered.readLine()) != null) {
                    builder.append(line).append('\n');
                }
                buffered.close();
                reader.close();
                inputStream.close();
            } catch (final IOException e) {
                LOGGER.error(e);
                throw e;
            }
        }

        return builder.toString();
    }

    /**
     * @param file
     * @param limit the limit in bytes to read
     * @return the content of the file
     * @throws IOException
     */
    public static final String readPartialFile(final File file, int limit)
        throws IOException {
        final StringBuilder builder = new StringBuilder();

        if (file != null && file.canRead() && limit > 0) {
            try {
                final FileInputStream inputStream = new FileInputStream(file);
                final InputStreamReader reader = new InputStreamReader(inputStream);
                final BufferedReader buffered = new BufferedReader(reader);
                String line;
                while ((line = buffered.readLine()) != null) {
                    builder.append(line).append('\n');
                    if (builder.length() >= limit) {
                        break;
                    }
                }
                buffered.close();
                reader.close();
                inputStream.close();
            } catch (final IOException e) {
                LOGGER.error(e);
                throw e;
            }
        }

        return builder.toString();
    }

    private static final void delereRecursiveInternal(File dir) {
        for (final File file : dir.listFiles()) {
            if (file.isDirectory()) {
                delereRecursiveInternal(file);
            }
            file.delete();
        }
    }

    /**
     * CARE: delete all files and directories from this file, this one included
     *
     * @param file
     */
    public static final void deleteRecursive(File file) {
        if (file == null || !file.isDirectory()) {
            return;
        }
        delereRecursiveInternal(file);
        file.delete();
    }
}

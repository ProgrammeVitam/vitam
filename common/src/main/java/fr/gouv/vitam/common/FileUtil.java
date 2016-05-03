/**
 * This file is part of Vitam Project.
 *
 * Copyright 2009, Frederic Bregier, and individual contributors by the author
 * tags. See the COPYRIGHT.txt in the distribution for a full listing of
 * individual contributors.
 *
 * All Vitam Project is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Vitam is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Vitam . If not, see <http://www.gnu.org/licenses/>.
 */
package fr.gouv.vitam.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * 
 *
 */
public class FileUtil {
    private static final VitamLogger LOGGER =
            VitamLoggerFactory.getInstance(FileUtil.class);
    /**
     * UTF-8 string
     */
    public static final String UTF_8 = "UTF-8";
    /**
     * UTF-8 Charset
     */
    public static final Charset UTF8 = Charset.forName(UTF_8);

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

        if (file.canRead()) {
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
     * @param limit
     *            the limit in bytes to read
     * @return the content of the file
     * @throws IOException
     */
    public static final String readPartialFile(final File file, int limit)
            throws IOException {
        final StringBuilder builder = new StringBuilder();

        if (file.canRead()) {
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
        for (File file : dir.listFiles()) {
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
        if (!file.isDirectory()) {
            return;
        }
        delereRecursiveInternal(file);
        file.delete();
    }

    private FileUtil() {
        // Unused
    }
}

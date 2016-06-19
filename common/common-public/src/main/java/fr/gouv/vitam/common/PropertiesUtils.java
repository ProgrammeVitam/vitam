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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Properties;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Property Utility class <br>
 * <br>
 * NOTE for developers: Do not add LOGGER there
 */
public final class PropertiesUtils {

    private static final String FILE_NOT_FOUND_IN_RESOURCES = "File not found in Resources: ";
    private static final String ARGUMENTS_MUST_BE_NON_NULL = "Arguments must be non null";

    private PropertiesUtils() {
        // Empty
    }

    /**
     * Get the File representation from the local path to the Resources directory
     *
     * @param resourcesFile properties file from resources directory
     * @return the associated File
     * @throws FileNotFoundException
     */
    public static final File getResourcesFile(String resourcesFile) throws FileNotFoundException {
        if (resourcesFile == null) {
            throw new FileNotFoundException(FILE_NOT_FOUND_IN_RESOURCES + resourcesFile);
        }
        URL url;
        try {
            url = PropertiesUtils.class.getClassLoader().getResource(resourcesFile);
        } catch (final SecurityException e) {// NOSONAR since an exception is thrown
            throw new FileNotFoundException(FILE_NOT_FOUND_IN_RESOURCES + resourcesFile);
        }
        if (url == null) {
            throw new FileNotFoundException(FILE_NOT_FOUND_IN_RESOURCES + resourcesFile);
        }
        File file;
        try {
            file = new File(url.toURI());
        } catch (final URISyntaxException e) { // NOSONAR
            file = new File(url.getFile().replaceAll("%20", " "));
        }
        if (file.exists()) {
            return file;
        }
        throw new FileNotFoundException(FILE_NOT_FOUND_IN_RESOURCES + resourcesFile);
    }

    /**
     * Get the Path representation from the local path to the Resources directory
     *
     * @param resourcesFile properties file from resources directory
     * @return the associated Path
     * @throws FileNotFoundException
     */
    public static final Path getResourcesPath(String resourcesFile) throws FileNotFoundException {
        return getResourcesFile(resourcesFile).toPath();
    }

    /**
     * Get the File associated with this filename, trying in this order: as fullpath, as in Vitam Config Folder, as
     * Resources file
     *
     * @param filename
     * @return the File if found
     * @throws FileNotFoundException if not fount
     */
    public static final File findFile(String filename) throws FileNotFoundException {
        // First try as full path
        File file = new File(filename);
        try {
            if (!file.exists()) {
                // Second try using VitamConfigFolder
                file = fileFromConfigFolder(filename);
                if (!file.exists()) {
                    // Third try using Resources
                    file = getResourcesFile(filename);
                }

            }
        } catch (final FileNotFoundException e) {// NOSONAR need to rewrite the exception
            throw new FileNotFoundException("File not found: " + filename);
        }
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + filename);
        }
        return file;
    }

    /**
     * Return a full file path using Config folder as root and subpath as sub paths.
     *
     * @param subpath the subpath under Config folder
     * @return the full file path (no check on existing is done)
     */
    public static final File fileFromConfigFolder(String subpath) {
        return new File(SystemPropertyUtil.getVitamConfigFolder(), subpath);
    }

    /**
     * Return a full file path using Data folder as root and subpath as sub paths.
     *
     * @param subpath the subpath under Data folder
     * @return the full file path (no check on existing is done)
     */
    public static final File fileFromDataFolder(String subpath) {
        return new File(SystemPropertyUtil.getVitamDataFolder(), subpath);
    }

    /**
     * Return a full file path using Log folder as root and subpath as sub paths.
     *
     * @param subpath the subpath under Log folder
     * @return the full file path (no check on existing is done)
     */
    public static final File fileFromLogFolder(String subpath) {
        return new File(SystemPropertyUtil.getVitamLogFolder(), subpath);
    }

    /**
     * Return a full file path using Tmp folder as root and subpath as sub paths.
     *
     * @param subpath the subpath under Tmp folder
     * @return the full file path (no check on existing is done)
     */
    public static final File fileFromTmpFolder(String subpath) {
        return new File(SystemPropertyUtil.getVitamTmpFolder(), subpath);
    }

    /**
     * Read a properties file and returns the associated Properties
     *
     * @param propertiesFile properties file
     * @return the associated Properties
     * @throws IOException
     */
    public static final Properties readProperties(File propertiesFile) throws IOException {
        if (propertiesFile == null) {
            throw new FileNotFoundException(FILE_NOT_FOUND_IN_RESOURCES + propertiesFile);
        }
        final Properties properties = new Properties();
        try (InputStream inputStream = new FileInputStream(propertiesFile)) {
            properties.load(inputStream);
        }
        return properties;
    }

    /**
     * Read the Yaml file and return the object read
     *
     * @param yamlFile
     * @param clasz the class representing the target object
     * @return the object read
     * @throws IOException
     */
    public static final <C> C readYaml(File yamlFile, Class<C> clasz) throws IOException {
        if (yamlFile == null || clasz == null) {
            throw new FileNotFoundException(ARGUMENTS_MUST_BE_NON_NULL);
        }
        try (final FileReader yamlFileReader = new FileReader(yamlFile)) {
            final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            return clasz.cast(mapper.readValue(yamlFileReader, clasz));
        }
    }

    /**
     * Read the Yaml file and return the object read
     *
     * @param yamlPath
     * @param clasz the class representing the target object
     * @return the object read
     * @throws IOException
     */
    public static final <C> C readYaml(Path yamlPath, Class<C> clasz) throws IOException {
        if (yamlPath == null || clasz == null) {
            throw new FileNotFoundException(ARGUMENTS_MUST_BE_NON_NULL);
        }
        final File file = yamlPath.toFile();
        return readYaml(file, clasz);
    }
}

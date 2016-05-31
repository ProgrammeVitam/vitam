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
 * Property Utility class
 */
public final class PropertiesUtils {

    private PropertiesUtils() {
        // Empty
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
            throw new FileNotFoundException("File not found in Resources: " + propertiesFile);
        }
        final Properties properties = new Properties();
        try (InputStream inputStream = new FileInputStream(propertiesFile)) {
            properties.load(inputStream);
        }
        return properties;
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
            throw new FileNotFoundException("File not found in Resources: " + resourcesFile);
        }
        URL url;
        try {
            url = PropertiesUtils.class.getClassLoader().getResource(resourcesFile);
        } catch (final SecurityException e) {// NOSONAR since an exception is thrown
            throw new FileNotFoundException("File not found in Resources: " + resourcesFile);
        }
        if (url == null) {
            throw new FileNotFoundException("File not found in Resources: " + resourcesFile);
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
        throw new FileNotFoundException("File not found in Resources: " + resourcesFile);
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
     * Read a properties file from resources directory and returns the associated Properties
     *
     * @param propertiesResourcesFile properties file from resources directory
     * @return the associated Properties
     * @throws IOException
     */
    public static final Properties readResourcesProperties(String propertiesResourcesFile) throws IOException {
        final File propertiesFile = getResourcesFile(propertiesResourcesFile);
        return readProperties(propertiesFile);
    }

    /**
     * Read the Yaml file and return the object read
     * @param yamlResourcesPath
     * @param clasz the class representing the target object
     * @return the object read
     * @throws IOException
     */
    public static final <C> C readResourcesYaml(Path yamlResourcesPath, Class<C> clasz) throws IOException {
        if (yamlResourcesPath == null || clasz == null) {
            throw new FileNotFoundException("Arguments must be non null");
        }
        File file = yamlResourcesPath.toFile();
        final FileReader yamlFile = new FileReader(file);
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return clasz.cast(mapper.readValue(yamlFile, clasz));
    }

    /**
     * Read the Yaml file and return the object read
     * @param yamlResourcesFile
     * @param clasz the class representing the target object
     * @return the object read
     * @throws IOException
     */
    public static final <C> C readResourcesYaml(String yamlResourcesFile, Class<C> clasz) throws IOException {
        if (yamlResourcesFile == null || clasz == null) {
            throw new FileNotFoundException("Arguments must be non null");
        }
        File file = getResourcesFile(yamlResourcesFile);
        final FileReader yamlFile = new FileReader(file);
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return clasz.cast(mapper.readValue(yamlFile, clasz));
    }
}

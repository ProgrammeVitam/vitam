/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.logging.SysErrLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Property Utility class
 *
 * NOTE for developers: Do not add LOGGER there
 */

public final class PropertiesUtils {

    private static final String FILE_NOT_FOUND_IN_RESOURCES = "File not found in Resources: ";
    private static final String ARGUMENTS_MUST_BE_NON_NULL = "Arguments must be non null";

    private PropertiesUtils() {
        // Empty
    }

    /**
     * Get the InputStream representation from the local path to the Resources directory
     *
     * @param resourcesFile properties file from resources directory
     * @return the associated File
     * @throws FileNotFoundException if the resource file not found
     */
    public static InputStream getConfigAsStream(String resourcesFile) throws FileNotFoundException {
        File file = new File(resourcesFile);
        if (!file.canRead()) {
            file = PropertiesUtils.fileFromConfigFolder(resourcesFile);
        }
        return file.canRead() ? new FileInputStream(file) : getResourceAsStream(resourcesFile);
    }

    /**
     * Get the InputStream representation from the local path to the Resources directory
     *
     * @param resourcesFile properties file from resources directory
     * @return the associated File
     * @throws FileNotFoundException if the resource file not found
     */
    public static File getConfigFile(String resourcesFile) throws FileNotFoundException {
        File file = new File(resourcesFile);
        if (!file.canRead()) {
            file = PropertiesUtils.fileFromConfigFolder(resourcesFile);
        }
        return file.canRead() ? file : getResourceFile(resourcesFile);
    }

    /**
     * Get the InputStream representation from the Resources directory
     *
     * @param resourcesFile properties file from resources directory
     * @return the associated File
     * @throws FileNotFoundException if the resource file not found
     */
    public static InputStream getResourceAsStream(String resourcesFile) throws FileNotFoundException {
        if (resourcesFile == null) {
            throw new FileNotFoundException(FILE_NOT_FOUND_IN_RESOURCES);
        }
        InputStream stream = null;
        try {
            stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcesFile);
        } catch (final SecurityException e) {
            // since another exception is thrown
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
        if (stream == null) {
            try {
                stream = PropertiesUtils.class.getClassLoader().getResourceAsStream(resourcesFile);
            } catch (final SecurityException e) {
                // since another exception is thrown
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
        }
        if (stream == null) {
            throw new FileNotFoundException(FILE_NOT_FOUND_IN_RESOURCES + resourcesFile);
        }
        return stream;
    }

    /**
     * Get the File representation from the local path to the Resources directory
     *
     * @param resourcesFile properties file from resources directory
     * @return the associated File
     * @throws FileNotFoundException if the resource file not found
     */
    public static File getResourceFile(String resourcesFile) throws FileNotFoundException {
        if (resourcesFile == null) {
            throw new FileNotFoundException(FILE_NOT_FOUND_IN_RESOURCES + resourcesFile);
        }
        URL url;
        try {
            url = PropertiesUtils.class.getClassLoader().getResource(resourcesFile);
        } catch (final SecurityException e) {
            // since another exception is thrown
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            throw new FileNotFoundException(FILE_NOT_FOUND_IN_RESOURCES + resourcesFile);
        }
        if (url == null) {
            url = Thread.currentThread().getContextClassLoader().getResource(resourcesFile);
        }
        if (url == null) {
            throw new FileNotFoundException(FILE_NOT_FOUND_IN_RESOURCES + resourcesFile);
        }
        File file;
        try {
            file = new File(url.toURI());
        } catch (final URISyntaxException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            file = new File(URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8));
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
     * @throws FileNotFoundException if resource file not found
     */
    public static Path getResourcePath(String resourcesFile) throws FileNotFoundException {
        return getResourceFile(resourcesFile).toPath();
    }

    public static Stream<String> getResourceListing(Class clazz, String path) throws URISyntaxException, IOException {
        URL dirURL = clazz.getClassLoader().getResource(path);
        if (dirURL != null && dirURL.getProtocol().equals("file")) {
            return Arrays.stream(new File(dirURL.toURI()).list());
        }

        if (dirURL == null) {
            /*
             * In case of a jar file, we can't actually find a directory.
             * Have to assume the same jar as clazz.
             */
            String me = clazz.getName().replace(".", "/") + ".class";
            dirURL = clazz.getClassLoader().getResource(me);
        }

        if (dirURL != null && dirURL.getProtocol().equals("jar")) {
            /* A JAR path */
            String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf('!')); //strip out only the JAR file
            try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"))) {
                Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
                Set<String> result = new HashSet<>(); //avoid duplicates in case it is a subdirectory
                while (entries.hasMoreElements()) {
                    String name = entries.nextElement().getName();
                    if (name.startsWith(path)) { //filter according to the path
                        String entry = name.substring(path.length());
                        int checkSubdir = entry.indexOf('/');
                        if (checkSubdir >= 0) {
                            // if it is a subdirectory, we just return the directory name
                            entry = entry.substring(0, checkSubdir);
                        }
                        result.add(entry);
                    }
                }
                return result.stream();
            }
        }

        throw new UnsupportedOperationException("Cannot list files for URL " + dirURL);
    }

    /**
     * Get the File associated with this filename, trying in this order: as fullpath, as in Vitam Config Folder, as
     * Resources file
     *
     * @param filename the file name
     * @return the File if found
     * @throws FileNotFoundException if not fount
     */
    public static File findFile(String filename) throws FileNotFoundException {
        // First try as full path
        File file = new File(filename);
        try {
            if (!file.exists()) {
                // Second try using VitamConfigFolder
                file = fileFromConfigFolder(filename);
                if (!file.exists()) {
                    // Third try using Resources
                    file = getResourceFile(filename);
                }
            }
        } catch (final FileNotFoundException e) {
            // need to rewrite the exception
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
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
    public static File fileFromConfigFolder(String subpath) {
        return new File(VitamConfiguration.getVitamConfigFolder(), subpath);
    }

    /**
     * Return a full file path using Data folder as root and subpath as sub paths.
     *
     * @param subpath the subpath under Data folder
     * @return the full file path (no check on existing is done)
     */
    public static File fileFromDataFolder(String subpath) {
        return new File(VitamConfiguration.getVitamDataFolder(), subpath);
    }

    /**
     * Return a full file path using Log folder as root and subpath as sub paths.
     *
     * @param subpath the subpath under Log folder
     * @return the full file path (no check on existing is done)
     */
    static File fileFromLogFolder(String subpath) {
        return new File(VitamConfiguration.getVitamLogFolder(), subpath);
    }

    /**
     * Return a full file path using Tmp folder as root and subpath as sub paths.
     *
     * @param subpath the subpath under Tmp folder
     * @return the full file path (no check on existing is done)
     */
    public static File fileFromTmpFolder(String subpath) {
        try {
            String canonicalPath = new File(VitamConfiguration.getVitamTmpFolder()).getCanonicalPath();
            File file = new File(VitamConfiguration.getVitamTmpFolder(), subpath);
            String fileCanonicalPath = file.getCanonicalPath();

            if (!fileCanonicalPath.startsWith(canonicalPath)) {
                throw new VitamRuntimeException(String.format("invalid path with subpath: %s", subpath));
            }

            return file;
        } catch (IOException e) {
            throw new VitamRuntimeException(e);
        }
    }

    /**
     * Read a properties file and returns the associated Properties
     *
     * @param propertiesFile properties file
     * @return the associated Properties
     * @throws IOException if cannot load file
     */
    public static Properties readProperties(File propertiesFile) throws IOException {
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
     * @param yamlFile the yaml file to read
     * @param clasz the class representing the target object
     * @return the object read
     * @throws IOException if read yaml input stream to class template exception occurred
     */
    public static <C> C readYaml(File yamlFile, Class<C> clasz) throws IOException {
        if (yamlFile == null || clasz == null) {
            throw new FileNotFoundException(ARGUMENTS_MUST_BE_NON_NULL);
        }
        try (final FileReader yamlFileReader = new FileReader(yamlFile)) {
            final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            return clasz.cast(mapper.readValue(yamlFileReader, clasz));
        } catch (final RuntimeException e) {
            throw new IOException(e);
        }
    }

    /**
     * Read the Yaml file and return the object read
     *
     * @param yamlFile the yaml file
     * @param typeReference the type reference representing the target interface object
     * @return the object read
     * @throws IOException if read yaml input stream to class template exception occurred
     */
    public static <C> C readYaml(File yamlFile, TypeReference<C> typeReference) throws IOException {
        if (yamlFile == null || typeReference == null) {
            throw new FileNotFoundException(ARGUMENTS_MUST_BE_NON_NULL);
        }
        try (final FileReader yamlFileReader = new FileReader(yamlFile)) {
            final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            return mapper.readValue(yamlFileReader, typeReference);
        } catch (final RuntimeException e) {
            throw new IOException(e);
        }
    }

    /**
     * Read the Yaml InputStream and return the object read
     *
     * @param yamlInputStream the yaml input stream to read
     * @param clasz the class representing the target object
     * @return the object read
     * @throws IOException if read yaml input stream to class template exception occurred
     */
    public static <C> C readYaml(InputStream yamlInputStream, Class<C> clasz) throws IOException {
        if (yamlInputStream == null || clasz == null) {
            throw new FileNotFoundException(ARGUMENTS_MUST_BE_NON_NULL);
        }
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            return clasz.cast(mapper.readValue(yamlInputStream, clasz));
        } catch (final RuntimeException e) {
            throw new IOException(e);
        }
    }

    /**
     * Read the Yaml file and return the object read
     *
     * @param yamlPath yaml file path
     * @param clasz the class representing the target object
     * @return the object read
     * @throws IOException if file not found exception
     */
    public static <C> C readYaml(Path yamlPath, Class<C> clasz) throws IOException {
        if (yamlPath == null || clasz == null) {
            throw new FileNotFoundException(ARGUMENTS_MUST_BE_NON_NULL);
        }
        final File file = yamlPath.toFile();
        return readYaml(file, clasz);
    }

    /**
     * Write the Yaml file
     *
     * @param destination the destination file
     * @param config the configuration object to write using Yaml format
     * @throws IOException if write object config exception occurred
     */
    public static void writeYaml(File destination, Object config) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(destination)) {
            final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.writeValue(outputStream, config);
        }
    }

    /**
     * Get the String content from the Resources directory
     *
     * @param resourcesFile properties file from resources directory
     * @return the associated File content as a String
     * @throws FileNotFoundException if the resource file not found
     */
    public static String getResourceAsString(String resourcesFile) throws FileNotFoundException {
        if (resourcesFile == null) {
            throw new FileNotFoundException(FILE_NOT_FOUND_IN_RESOURCES);
        }

        String fileAsString = null;
        try {
            fileAsString = new String(Files.readAllBytes(getResourcePath(resourcesFile)));
        } catch (final SecurityException e) {
            // since another exception is thrown
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        } catch (final IOException e) {
            throw new FileNotFoundException(FILE_NOT_FOUND_IN_RESOURCES + resourcesFile);
        }

        return fileAsString;
    }
}

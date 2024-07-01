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
package fr.gouv.vitam.common.security;

import com.google.common.base.Joiner;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.owasp.esapi.SafeFile;
import org.owasp.esapi.errors.ValidationException;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Checker for Sanity of file manipulation to avoid Path Traversal vulnerability
 */
public class SafeFileChecker {

    private static final Pattern FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-_@]+(\\.[a-zA-Z0-9\\-_@]+)*$");
    private static final Pattern PATH_COMPONENT_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-_@]+(\\.[a-zA-Z0-9\\-_@]+)*$");

    private static final AlertService alertService = new AlertServiceImpl();
    public static final String LOCAL_ENVIRONMENT = "local";

    private SafeFileChecker() {
        // Empty constructor
    }

    /**
     * File path sanity checker.
     * Checks folder & filename authorized patterns, path traversal attacks & ESAPI sanity checks
     *
     * @param safeRootPath first or initial part(s) of a path representing a FileSystem resource
     * @param subPaths sub path parts. Every part should be a single folder level, except last part which is the actual filename.
     * @return the resolved {@link File}
     * @throws IllegalPathException thrown when any check fails with UnChecked or Runtime exception
     */
    public static File checkSafeFilePath(String safeRootPath, String... subPaths) throws IllegalPathException {
        return checkSafePath(safeRootPath, subPaths, false);
    }

    /**
     * Directory path sanity checker.
     * Checks folder authorized patterns, path traversal attacks & ESAPI sanity checks
     *
     * @param safeRootPath first or initial part(s) of a path representing a FileSystem resource
     * @param subPaths sub path parts. Every part should be a single folder level.
     * @return the resolved directory {@link File}
     * @throws IllegalPathException thrown when any check fails with UnChecked or Runtime exception
     */
    public static File checkSafeDirPath(String safeRootPath, String... subPaths) throws IllegalPathException {
        return checkSafePath(safeRootPath, subPaths, true);
    }

    /**
     * Path sanity for class-path resources
     * Checks filename authorized patterns, path traversal attacks & ESAPI sanity checks
     *
     * @param resourceName the resource file name to check
     * @throws IllegalPathException thrown when any check fails with UnChecked or Runtime exception
     */
    public static void checkSafeRessourceFilePath(String resourceName) throws IllegalPathException {
        // Validate ressource name using dummy root path
        checkSafePath(VitamConfiguration.getVitamConfigFolder(), new String[] { resourceName }, false);
    }

    private static File checkSafePath(String safeRootPath, String[] subPaths, boolean isDirectory)
        throws IllegalPathException {
        if (StringUtils.isEmpty(safeRootPath)) {
            throw new IllegalPathException("Null or empty root path");
        }
        if (subPaths == null) {
            throw new IllegalPathException("Null sub paths");
        }
        for (String subPath : subPaths) {
            if (subPath == null) {
                throw new IllegalPathException("Null sub path");
            }
        }

        String finalPath = safeRootPath;
        if (!finalPath.endsWith(File.separator)) {
            finalPath = finalPath + File.separator;
        }
        finalPath = finalPath + Joiner.on(File.separator).join(subPaths);

        try {
            // Check filename
            if (!isDirectory) {
                if (ArrayUtils.isEmpty(subPaths)) {
                    throw new VitamRuntimeException("Missing filename");
                }

                String fileName = subPaths[subPaths.length - 1];
                if (!FILENAME_PATTERN.matcher(fileName).matches()) {
                    throw new VitamRuntimeException("Invalid filename: '" + fileName + "'");
                }
            }

            // Check sub-directories
            int nbDirectorySubPaths = isDirectory ? subPaths.length : subPaths.length - 1;
            for (int i = 0; i < nbDirectorySubPaths; i++) {
                String subPath = subPaths[i];
                if (!PATH_COMPONENT_PATTERN.matcher(subPath).matches()) {
                    throw new VitamRuntimeException("Invalid sub-path: '" + subPath + "'");
                }
            }

            // OWASP ESAPI checks
            File sanityCheckedFile = doSanityCheck(finalPath);

            // Avoid canonical path check for local environment due to symbolic link used for conf files
            if (
                VitamConfiguration.getEnvironmentName() != null &&
                VitamConfiguration.getEnvironmentName().equals(LOCAL_ENVIRONMENT)
            ) {
                return sanityCheckedFile;
            }

            // Path Traversal check
            doCanonicalPathCheck(sanityCheckedFile);
            return sanityCheckedFile;
        } catch (Exception e) {
            String error = "Check path traversal error: '" + finalPath + "'";
            alertService.createAlert(error);
            throw new IllegalPathException(error, e);
        }
    }

    /**
     * sanity check with ESAPI validation
     *
     * @param path
     */
    private static File doSanityCheck(String path) throws ValidationException {
        return new SafeFile(path);
    }

    /**
     * Check path traversal attacks by ensuring canonical path is the same as the file path
     *
     * @param file the file to check
     * @throws IOException
     */
    private static void doCanonicalPathCheck(File file) throws IOException {
        String path = file.getPath();
        String canonicalPath = file.getCanonicalPath();

        if (!path.equals(canonicalPath)) {
            throw new IOException(String.format("Invalid path (%s) did not match canonical : %s", path, canonicalPath));
        }
    }
}

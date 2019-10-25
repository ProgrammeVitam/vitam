/*
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

package fr.gouv.vitam.common.security;

import fr.gouv.vitam.common.exception.VitamRuntimeException;
import org.junit.Test;

import java.io.IOException;

/**
 * Test Class for SafeFileChecker
 *
 * @author afraoucene
 */

public class SafeFileCheckerTest {
    private final String ROOT_PATH = "/my.dir/app-directory";
    private final String ROOT_MULTI_SLASH_PATH = "/mydir///app-directory";
    private final String ROOT_DOOTED_PATH = "/..0_StorageTraceability_20180220@031002.zip";
    private final String ROOT_PATH_INFECTED = "/mydir/./app-_directory";
    private final String SUBPATH_SAFE = "json_good_sanity,";
    private final String SUBPATH_INFECTED = "../..//etc/password";
    private final String SUBPATH_INFECTED_ENCODED = "%2e%2e%2f..\\/etc/password";
    private final String SUBPATH_INFECTED_BAD_CHARS = "myDir&,";
    private final String VALID_FILENAME = "good-file,.report.json";
    private final String INVALID_FILENAME = "my%2ffilename.json";
    private final String INVALID_FILENAME_NULLED = "filename\0.json";
    private final String INVALID_BLACKLISTED_FILENAME = "my|filena?me<.json";

    @Test(expected = IOException.class)
    public void checkInvalidPathComponentFile() throws IOException, VitamRuntimeException {
        SafeFileChecker.checkSafeFilePath(ROOT_PATH, SUBPATH_INFECTED, VALID_FILENAME);
    }
    @Test
    public void checkValidMultiSlashPathComponentFile() throws IOException, VitamRuntimeException {
        SafeFileChecker.checkSafeFilePath(ROOT_MULTI_SLASH_PATH, SUBPATH_SAFE, VALID_FILENAME);
    }

    @Test
    public void checkValidDotedPathComponentFile() throws IOException, VitamRuntimeException {
        SafeFileChecker.checkSafeFilePath(ROOT_DOOTED_PATH, SUBPATH_SAFE, VALID_FILENAME);
    }

    @Test(expected = IOException.class)
    public void checkInvalidRootPathComponentFile() throws IOException, VitamRuntimeException {
        SafeFileChecker.checkSafeFilePath(ROOT_PATH_INFECTED, SUBPATH_SAFE, VALID_FILENAME);
    }

    @Test(expected = IOException.class)
    public void checkInvalidEncodedPathComponentFile() throws IOException, VitamRuntimeException {
        SafeFileChecker.checkSafeFilePath(ROOT_PATH, SUBPATH_INFECTED_ENCODED);
    }

    @Test(expected = IOException.class)
    public void checkInvalidBadCharPathComponentFile() throws IOException, VitamRuntimeException {
        SafeFileChecker.checkSafeFilePath(ROOT_PATH, SUBPATH_INFECTED_BAD_CHARS);
    }

    @Test
    public void checkValidPath() throws IOException, VitamRuntimeException {
        SafeFileChecker.checkSafeFilePath(ROOT_PATH, SUBPATH_SAFE);
    }

    @Test(expected = IOException.class)
    public void checkInvalidNulledFilenamePath() throws IOException, VitamRuntimeException {
        SafeFileChecker.checkSafeFilePath(ROOT_PATH, SUBPATH_SAFE, INVALID_FILENAME_NULLED);
    }

    @Test(expected = IOException.class)
    public void checkInvalidFilenamePath() throws IOException, VitamRuntimeException {
        SafeFileChecker.checkSafeFilePath(ROOT_PATH, SUBPATH_SAFE, INVALID_FILENAME);
    }

    @Test(expected = IOException.class)
    public void checkInvalidBlackListedFilenamePath() throws IOException, VitamRuntimeException {
        SafeFileChecker.checkSafeFilePath(ROOT_PATH, SUBPATH_SAFE, INVALID_BLACKLISTED_FILENAME);
    }
}

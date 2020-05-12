/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThatCode;

/**
 * Test Class for SafeFileChecker
 *
 * @author afraoucene
 */

public class SafeFileCheckerTest {
    private final String VALID_ROOT_PATH = "/mydir";
    private final String VALID_SUBPATH = "json_good_sanity";

    private static List<String> validPaths = new ArrayList<>();
    private static List<String> invalidPaths = new ArrayList<>();
    private static List<String> invalidFilenames = new ArrayList<>();

    @BeforeClass
    public static void setUpBeforeClass() {
        validPaths.add("/directory/subdirectory");
        validPaths.add("/directory");
        validPaths.add("/dir.ectory/subdirectory");
        validPaths.add("/dir_ect_ory/sub.dir.ectory");
        validPaths.add("/dir-ectory/subdirectory");
        validPaths.add("/dir-ectory");
        validPaths.add("/my.dir/app-directory");
        validPaths.add("/mydir///app-directory");

        invalidPaths.add("filename\0.json");
        invalidPaths.add("my&Dir");
        invalidPaths.add("%2e%2e%2f..\\/etc/password");
        invalidPaths.add("/mydir/./app-_directory");
        invalidPaths.add("../../etc/password");
        invalidPaths.add("/mydir/./app-_directory");
        invalidPaths.add("../..//etc/password");
        invalidPaths.add("myDir&,");

        invalidFilenames.add("my|filena?me<.json");
        invalidFilenames.add(".file");
        invalidFilenames.add("my%2ffilename.json");
    }

    @Test
    public void checkValidSubPaths() {
        for(String subPath : validPaths) {
            assertThatCode(() -> SafeFileChecker.checkSafeFilePath(VALID_ROOT_PATH, subPath))
                .doesNotThrowAnyException();
        }
    }

    @Test
    public void checkValidRootPaths() {
        for(String rootPath : validPaths) {
            assertThatCode(() -> SafeFileChecker.checkSafeFilePath(rootPath, VALID_SUBPATH))
                .doesNotThrowAnyException();
        }
    }

    @Test
    public void checkInvalidRootPaths() {
        for(String rootPath : invalidPaths) {
            assertThatCode(() -> SafeFileChecker.checkSafeFilePath(rootPath, VALID_SUBPATH))
                .isInstanceOf(IOException.class);
        }
    }

    @Test
    public void checkInvalidSubPaths() {
        for(String subPath : invalidFilenames) {
            assertThatCode(() -> SafeFileChecker.checkSafeFilePath(VALID_ROOT_PATH, subPath))
                .isInstanceOf(IOException.class);
        }
    }
}

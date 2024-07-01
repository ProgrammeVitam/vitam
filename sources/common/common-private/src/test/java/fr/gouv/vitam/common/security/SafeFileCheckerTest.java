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

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SafeFileCheckerTest {

    private static final String VALID_ROOT_PATH = "/mydir";
    private static final String VALID_SUB_PATH = "json_good_sanity";
    private static final String VALID_FILENAME = "file.txt";

    private final List<String> validRootPaths = new ArrayList<>();
    private final List<String> invalidRootPaths = new ArrayList<>();

    private final List<String[]> validSubPaths = new ArrayList<>();
    private final List<String[]> invalidSubPaths = new ArrayList<>();

    private final List<String> invalidFilenames = new ArrayList<>();
    private final List<String> validFilenames = new ArrayList<>();

    @Before
    public void setUpBeforeClass() {
        validRootPaths.add("/directory/subdirectory");
        validRootPaths.add("/directory");
        validRootPaths.add("/directory/");
        validRootPaths.add("/dir.ectory/subdirectory");
        validRootPaths.add("/dir_ect_ory/sub.dir.ectory/");
        validRootPaths.add("/dir-ectory/subdirectory");
        validRootPaths.add("/dir-ectory");

        invalidRootPaths.add(null);
        invalidRootPaths.add("");
        invalidRootPaths.add("./");
        invalidRootPaths.add("/dir/../");

        validSubPaths.add(new String[0]);
        validSubPaths.add(new String[] { "a" });
        validSubPaths.add(new String[] { "simpleDir" });
        validSubPaths.add(new String[] { "simpleDir", "Complex_Dir.Name" });
        validSubPaths.add(new String[] { "dir@dir" });

        invalidSubPaths.add(new String[] { null });
        invalidSubPaths.add(new String[] { "" });
        invalidSubPaths.add(new String[] { "." });
        invalidSubPaths.add(new String[] { ".." });
        invalidSubPaths.add(new String[] { "/" });
        invalidSubPaths.add(new String[] { "\\" });
        invalidSubPaths.add(new String[] { "Illégàl" });
        invalidSubPaths.add(new String[] { "subDir", "" });
        invalidSubPaths.add(new String[] { "dir&dir" });
        invalidSubPaths.add(new String[] { "dir#dir" });
        invalidSubPaths.add(new String[] { "dir$dir" });
        invalidSubPaths.add(new String[] { "dir!dir" });
        invalidSubPaths.add(new String[] { "dir?dir" });
        invalidSubPaths.add(new String[] { "dir<dir" });
        invalidSubPaths.add(new String[] { "dir\tdir" });
        invalidSubPaths.add(new String[] { "dir\0dir" });
        invalidSubPaths.add(new String[] { "%2e%2e%2f" });
        invalidSubPaths.add(new String[] { ".", "PathTraversal" });
        invalidSubPaths.add(new String[] { "dir", "..", "PathTraversal" });

        invalidFilenames.add(null);
        invalidFilenames.add("");
        invalidFilenames.add("my|Filename.json");
        invalidFilenames.add("myFilena?me.json");
        invalidFilenames.add("myFilename<.json");
        invalidFilenames.add("myFïlename.json");
        invalidFilenames.add("myFilen+me.json");
        invalidFilenames.add("myFilen$me.json");
        invalidFilenames.add("myFilen&me.json");
        invalidFilenames.add("myFilen#me.json");
        invalidFilenames.add("myFile name.json");
        invalidFilenames.add("myFile\tname.json");
        invalidFilenames.add("myFile\0name.json");
        invalidFilenames.add("myFile%2ename.json");
        invalidFilenames.add(".file");

        validFilenames.add("noextension");
        validFilenames.add("simpleFile.txt");
        validFilenames.add("myFilen@me.json");
        validFilenames.add("org.my-org.modules.my-lib_2.0-1-SNAPSHOT.jar");
    }

    @Test
    public void checkSafeDirPathWithValidRootPath() {
        for (String validRootPath : validRootPaths) {
            assertThatCode(() -> SafeFileChecker.checkSafeDirPath(validRootPath)).doesNotThrowAnyException();

            assertThatCode(
                () -> SafeFileChecker.checkSafeDirPath(validRootPath, VALID_SUB_PATH)
            ).doesNotThrowAnyException();
        }
    }

    @Test
    public void checkSafeDirPathWithInvalidRootPath() {
        for (String invalidRootPath : invalidRootPaths) {
            assertThatThrownBy(() -> SafeFileChecker.checkSafeDirPath(invalidRootPath)).isInstanceOf(
                IllegalPathException.class
            );

            assertThatThrownBy(() -> SafeFileChecker.checkSafeDirPath(invalidRootPath, VALID_SUB_PATH)).isInstanceOf(
                IllegalPathException.class
            );
        }
    }

    @Test
    public void checkSafeDirPathWithValidSubPaths() {
        for (String[] subPaths : validSubPaths) {
            assertThatCode(
                () -> SafeFileChecker.checkSafeDirPath(VALID_ROOT_PATH, subPaths)
            ).doesNotThrowAnyException();
        }
    }

    @Test
    public void checkSafeDirPathWithInvalidSubPaths() {
        for (String[] subPaths : invalidSubPaths) {
            System.out.println(Arrays.asList(subPaths));
            assertThatThrownBy(() -> SafeFileChecker.checkSafeDirPath(VALID_ROOT_PATH, subPaths)).isInstanceOf(
                IllegalPathException.class
            );
        }
    }

    @Test
    public void checkSafeFilePathWithValidRootPath() {
        for (String validRootPath : validRootPaths) {
            assertThatCode(
                () -> SafeFileChecker.checkSafeFilePath(validRootPath, VALID_FILENAME)
            ).doesNotThrowAnyException();

            assertThatCode(
                () -> SafeFileChecker.checkSafeFilePath(validRootPath, VALID_SUB_PATH, VALID_FILENAME)
            ).doesNotThrowAnyException();
        }
    }

    @Test
    public void checkSafeFilePathWithInvalidRootPath() {
        for (String invalidRootPath : invalidRootPaths) {
            assertThatThrownBy(() -> SafeFileChecker.checkSafeFilePath(invalidRootPath, VALID_FILENAME)).isInstanceOf(
                IllegalPathException.class
            );

            assertThatThrownBy(
                () -> SafeFileChecker.checkSafeFilePath(invalidRootPath, VALID_SUB_PATH, VALID_FILENAME)
            ).isInstanceOf(IllegalPathException.class);
        }
    }

    @Test
    public void checkSafeFilePathWithValidSubPaths() {
        for (String[] subPaths : validSubPaths) {
            assertThatCode(() -> {
                String[] subPathsWithFileName = ArrayUtils.add(subPaths, VALID_FILENAME);
                SafeFileChecker.checkSafeFilePath(VALID_ROOT_PATH, subPathsWithFileName);
            }).doesNotThrowAnyException();
        }
    }

    @Test
    public void checkSafeFilePathWithInvalidSubPaths() {
        for (String[] subPaths : invalidSubPaths) {
            assertThatThrownBy(() -> {
                String[] subPathsWithFileName = ArrayUtils.add(subPaths, VALID_FILENAME);
                SafeFileChecker.checkSafeFilePath(VALID_ROOT_PATH, subPathsWithFileName);
            }).isInstanceOf(IllegalPathException.class);
        }
    }

    @Test
    public void checkSafeFilePathWithInvalidFilenames() {
        for (String invalidFileName : invalidFilenames) {
            assertThatThrownBy(() -> SafeFileChecker.checkSafeFilePath(VALID_ROOT_PATH, invalidFileName)).isInstanceOf(
                IllegalPathException.class
            );

            assertThatThrownBy(
                () -> SafeFileChecker.checkSafeFilePath(VALID_ROOT_PATH, VALID_SUB_PATH, invalidFileName)
            ).isInstanceOf(IllegalPathException.class);
        }
    }

    @Test
    public void checkSafeFilePathWithValidFilenames() {
        for (String validFileName : validFilenames) {
            assertThatCode(
                () -> SafeFileChecker.checkSafeFilePath(VALID_ROOT_PATH, validFileName)
            ).doesNotThrowAnyException();

            assertThatCode(
                () -> SafeFileChecker.checkSafeFilePath(VALID_ROOT_PATH, VALID_SUB_PATH, validFileName)
            ).doesNotThrowAnyException();
        }
    }
}

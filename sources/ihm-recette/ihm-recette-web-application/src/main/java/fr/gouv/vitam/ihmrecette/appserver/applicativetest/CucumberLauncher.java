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
package fr.gouv.vitam.ihmrecette.appserver.applicativetest;

import cucumber.runtime.ClassFinder;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ResourceLoaderClassFinder;
import fr.gouv.vitam.functionaltest.cucumber.report.VitamReporter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to manage cucumber
 */
public class CucumberLauncher {

    /**
     * report directory
     */
    private Path tnrReportDirectory;

    public CucumberLauncher(Path tnrReportDirectory) {
        this.tnrReportDirectory = tnrReportDirectory;
    }

    /**
     * lauch cucumber test
     *
     * @param arguments list of cucumber arguments
     * @return status code
     * @throws IOException
     */
    byte launchCucumberTest(List<String> arguments) throws IOException {
        RuntimeOptions runtimeOptions = new RuntimeOptions(arguments);

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        ResourceLoader resourceLoader = new MultiLoader(classLoader);
        ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);
        Runtime runtime = new Runtime(resourceLoader, classFinder, classLoader, runtimeOptions);
        runtime.run();
        return runtime.exitStatus();
    }

    /**
     * create a list to manage cucumber arguments
     *
     * @param glueCode path on the glue code
     * @param featurePath path on the feature
     * @param reportName
     * @return list of cucumber arguments on a list
     */
    public List<String> buildCucumberArgument(String glueCode, Path featurePath, String reportName) {
        List<String> arguments = new ArrayList<>();
        arguments.add("-s");
        arguments.add("-g");
        arguments.add(glueCode);
        arguments.add("-p");

        Path reportPath = tnrReportDirectory.resolve(reportName);
        arguments.add(VitamReporter.class.getCanonicalName() + ":" + reportPath.toString());
        arguments.add(featurePath.toAbsolutePath().toString());
        return arguments;
    }
}

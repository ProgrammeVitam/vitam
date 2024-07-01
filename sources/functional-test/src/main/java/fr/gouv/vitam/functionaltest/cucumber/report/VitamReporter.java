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
package fr.gouv.vitam.functionaltest.cucumber.report;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.json.JsonHandler;
import gherkin.formatter.Formatter;
import gherkin.formatter.NiceAppendable;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.Background;
import gherkin.formatter.model.Examples;
import gherkin.formatter.model.Feature;
import gherkin.formatter.model.Match;
import gherkin.formatter.model.Result;
import gherkin.formatter.model.Scenario;
import gherkin.formatter.model.ScenarioOutline;
import gherkin.formatter.model.Step;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

public class VitamReporter implements Reporter, Formatter {

    private NiceAppendable output;
    private Reports reports = new Reports();
    private Report report = new Report();
    private Feature currentFeature;
    private Queue<Step> steps = new LinkedList<>();

    public VitamReporter(Appendable appendable) {
        output = new NiceAppendable(appendable);
    }

    @Override
    public void syntaxError(String s, String s1, List<String> list, String s2, Integer integer) {
        String out = String.format("syntaxError(): s=%s, s1=%s, list=%s, s2=%s, integer=%s", s, s1, list, s2, integer);
        System.out.println(out);
    }

    @Override
    public void uri(String s) {}

    @Override
    public void feature(Feature feature) {
        System.out.println("\n\n########\nFEATURE: " + feature.getName() + " - " + feature.getId());
        currentFeature = feature;
        steps.clear();
    }

    @Override
    public void scenarioOutline(ScenarioOutline scenarioOutline) {}

    @Override
    public void examples(Examples examples) {}

    @Override
    public void startOfScenarioLifeCycle(Scenario scenario) {
        report.setStart(LocalDateUtil.now());
    }

    @Override
    public void background(Background background) {}

    @Override
    public void scenario(Scenario scenario) {
        System.out.println("- SCENARIO: " + scenario.getName() + " (line: " + scenario.getLine() + ")");
        report = new Report();
        report.setDescription(scenario.getName());
        report.setFeature(currentFeature.getName());
        report.setTags(
            scenario
                .getTags()
                .stream()
                .map(tag -> tag.getName().substring(1, tag.getName().length()))
                .collect(Collectors.toList())
        );
        currentFeature
            .getTags()
            .stream()
            .map(tag -> tag.getName().substring(1, tag.getName().length()))
            .forEach(tagName -> report.addTag(tagName));
        reports.add(report);
        steps.clear();
    }

    @Override
    public void step(Step step) {
        steps.add(step);
    }

    @Override
    public void endOfScenarioLifeCycle(Scenario scenario) {
        report.setEnd(LocalDateUtil.now());
    }

    @Override
    public void done() {
        System.out.println("##### DONE ####");
        reports.setEnd(LocalDateUtil.now());
        output.append(JsonHandler.prettyPrint(reports));
    }

    @Override
    public void close() {
        output.close();
    }

    @Override
    public void eof() {}

    @Override
    public void before(Match match, Result result) {}

    @Override
    public void result(Result result) {
        Step step = steps.poll();
        System.out.printf(
            "  * - %s - %s%s%n",
            Instant.now(),
            result.getStatus().toUpperCase(),
            step != null ? " - " + step.getName() + " (line: " + step.getLine() + ")" : ""
        );

        if (!result.getStatus().equals(Result.PASSED) && !result.getStatus().equals(Result.SKIPPED.getStatus())) {
            report.addError(result.getErrorMessage());
            reports.add(report);
        }
    }

    @Override
    public void after(Match match, Result result) {}

    @Override
    public void match(Match match) {}

    @Override
    public void embedding(String s, byte[] bytes) {}

    @Override
    public void write(String operationId) {
        report.setOperationId(operationId);
    }
}

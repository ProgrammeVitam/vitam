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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class for reports
 *
 * @author geled
 */
public class Reports {

    /**
     * list of report
     */
    private List<Report> reports;

    /**
     * Start time
     */
    private LocalDateTime start;
    /**
     * End time
     */
    private LocalDateTime end;

    /**
     * constructor
     */
    public Reports() {
        reports = new ArrayList<>();
        start = LocalDateUtil.now();
    }

    /**
     * @return number of OK test
     */
    public long getNumberOfTestOK() {
        return reports.stream().filter(Report::isOK).count();
    }

    /**
     * @return number of KO test
     */
    public long getNumberOfTestKO() {
        return reports.stream().filter(report -> !report.isOK()).count();
    }

    /**
     * @param tagName Tag Value
     * @return number of OK Test By TagName
     */
    public long numberOfTestOKByTagName(String tagName) {
        return reports.stream().filter(report -> report.getTags().contains(tagName) && report.isOK()).count();
    }

    /**
     * @param tagName Tag value
     * @return number of KO Test By TagName
     */
    public long numberOfTestKOByTagName(String tagName) {
        return reports.stream().filter(report -> report.getTags().contains(tagName) && !report.isOK()).count();
    }

    /**
     * @return List of TagInfo
     */
    public List<TagInfo> getTags() {
        return reports
            .stream()
            .map(report -> report.getTags())
            .flatMap(List::stream)
            .collect(Collectors.toSet())
            .stream()
            .map(tagName -> new TagInfo(tagName, numberOfTestOKByTagName(tagName), numberOfTestKOByTagName(tagName)))
            .collect(Collectors.toList());
    }

    /**
     * @return list of report
     */
    public List<Report> getReports() {
        return reports;
    }

    /**
     * @param report add an individual report
     */
    public void add(Report report) {
        reports.add(report);
    }

    /**
     * @return the end
     */
    public LocalDateTime getEnd() {
        return end;
    }

    /**
     * @param end the end to set
     * @return this
     */
    public void setEnd(LocalDateTime end) {
        this.end = end;
    }

    /**
     * @return the start
     */
    public LocalDateTime getStart() {
        return start;
    }
}

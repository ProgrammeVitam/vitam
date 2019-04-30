package fr.gouv.vitam.worker.core.plugin.probativevalue.pojo;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static org.assertj.core.api.Assertions.assertThat;

public class ProbativeReportEntryTest {
    @Test
    public void should_create_entry_without_status_always_KO() {
        // Given / When
        ProbativeReportEntry entry = ProbativeReportEntry.koFrom("startDate", Collections.emptyList(), "objectGroupId", "objectId", "usageVersion", Collections.emptyList());

        // Then
        assertThat(entry.getStatus()).isEqualTo(KO);
    }

    @Test
    public void should_create_entry_without_status_always_KO_static() {
        // Given / When
        ProbativeReportEntry entry = ProbativeReportEntry.koFrom("startDate", Collections.emptyList(), "objectGroupId", "objectId", "usageVersion");

        // Then
        assertThat(entry.getStatus()).isEqualTo(KO);
    }

    @Test
    public void should_create_entry_without_status_always_KO_static_1() {
        // Given / When
        ProbativeReportEntry entry = ProbativeReportEntry.koFrom("startDate", Collections.emptyList(), "objectGroupId", "objectId", "usageVersion", Collections.emptyList());

        // Then
        assertThat(entry.getStatus()).isEqualTo(KO);
    }

    @Test
    public void should_create_entry_without_status_always_KO_static_2() {
        // Given / When
        ProbativeReportEntry entry = ProbativeReportEntry.koFrom("startDate", Collections.emptyList(), "objectGroupId", "objectId", "usageVersion", Collections.emptyList(), Collections.emptyList());

        // Then
        assertThat(entry.getStatus()).isEqualTo(KO);
    }

    @Test
    public void should_create_entry_OK() {
        // Given
        List<String> unitIds = Collections.singletonList("aUnitId");
        List<ProbativeOperation> operations = Arrays.asList(aRandomProbativeOperation(), aRandomProbativeOperation(), aRandomProbativeOperation());
        List<ProbativeCheck> checks = Stream.of(ChecksInformation.values()).map(this::toProbativeCheck).collect(Collectors.toList());

        // When
        ProbativeReportEntry entry = new ProbativeReportEntry("startDate", unitIds, "objectGroupId", "objectId", "usageVersion", operations, checks);

        // Then
        assertThat(entry.getStatus()).isEqualTo(OK);
    }

    private ProbativeCheck toProbativeCheck(ChecksInformation check) {
        String sourceDestination = RandomStringUtils.randomAlphabetic(5);
        return ProbativeCheck.okFrom(check, sourceDestination, sourceDestination);
    }

    private ProbativeOperation aRandomProbativeOperation() {
        return new ProbativeOperation(
            RandomStringUtils.randomAlphabetic(5),
            "evType",
            "evIdAppSession",
            "rightStatementIdentifier",
            "agIdApp",
            "evDateTime"
        );
    }
}
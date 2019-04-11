package fr.gouv.vitam.storage.offers.tape.cas;

import com.google.common.collect.Streams;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.storage.tapelibrary.TapeLibraryTopologyConfiguration;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BucketTopologyHelperTest {

    @BeforeClass
    public static void initializeClass() {
        VitamConfiguration.setTenants(Arrays.asList(0, 1, 2, 3));
        VitamConfiguration.setEnvironmentName(null);
    }

    @Test
    public void testInitialization() throws Exception {

        assertThatThrownBy(() -> loadTopology("topology-test-bad-bucket-conf-duplicate-tenants.conf"))
            .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> loadTopology("topology-test-bad-bucket-conf-empty-tenant.conf"))
            .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> loadTopology("topology-test-bad-bucket-conf-missing-tenants."))
            .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> loadTopology("topology-test-bad-bucket-conf-null-tenant.conf"))
            .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> loadTopology("topology-test-bad-bucket-empty-buckets.conf"))
            .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> loadTopology("topology-test-bad-bucket-negative-tar-buffering-timeout.conf"))
            .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> loadTopology("topology-test-bad-bucket-null-buckets.conf"))
            .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> loadTopology("topology-test-bad-file-buckets-duplicate-folder.conf"))
            .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> loadTopology("topology-test-bad-file-buckets-missing-default.conf"))
            .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> loadTopology("topology-test-bad-file-buckets-missing-empty-folders.conf"))
            .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> loadTopology("topology-test-bad-file-buckets-missing-invalid-folder.conf"))
            .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> loadTopology("topology-test-bad-file-buckets-missing-non-empty-default.conf"))
            .isInstanceOf(Exception.class);
        assertThatThrownBy(() -> loadTopology("topology-test-bad-file-buckets-missing-null-folders.conf"))
            .isInstanceOf(Exception.class);

        loadTopology("topology-test.conf");
    }

    @Test
    public void getFileBucketFromContainerName() throws Exception {

        // Given
        BucketTopologyHelper bucketTopologyHelper = loadTopology("topology-test.conf");

        // When / Then
        assertThat(bucketTopologyHelper.getFileBucketFromContainerName("0_unit")).isEqualTo("test-metadata");
        assertThat(bucketTopologyHelper.getFileBucketFromContainerName("1_object")).isEqualTo("admin-objects");
        assertThat(bucketTopologyHelper.getFileBucketFromContainerName("2_report")).isEqualTo("prod-default");
    }

    @Test
    public void getFileBucketFromContainerNameWithExplicitFileBucketConf() throws Exception {

        // Given
        BucketTopologyHelper bucketTopologyHelper = loadTopology("topology-test-explicit-file-buckets.conf");

        // When / Then
        assertThat(bucketTopologyHelper.getFileBucketFromContainerName("0_unit")).isEqualTo("test-metadata");
        assertThat(bucketTopologyHelper.getFileBucketFromContainerName("1_object")).isEqualTo("admin-objects");
        assertThat(bucketTopologyHelper.getFileBucketFromContainerName("2_report")).isEqualTo("prod-default");
    }

    @Test
    public void getBucketFromFileBucket() throws Exception {

        // Given
        BucketTopologyHelper bucketTopologyHelper = loadTopology("topology-test.conf");

        // When / Then
        assertThat(bucketTopologyHelper.getBucketFromFileBucket("test-metadata")).isEqualTo("test");
        assertThat(bucketTopologyHelper.getBucketFromFileBucket("admin-objects")).isEqualTo("admin");
        assertThat(bucketTopologyHelper.getBucketFromFileBucket("prod-default")).isEqualTo("prod");
    }

    @Test
    public void listFileBuckets() throws Exception {

        // Given
        BucketTopologyHelper bucketTopologyHelper = loadTopology("topology-test.conf");

        // When / Then
        assertThat(bucketTopologyHelper.listFileBuckets()).containsExactlyInAnyOrder(
            "test-metadata", "test-objects", "test-default",
            "admin-metadata", "admin-objects", "admin-default",
            "prod-metadata", "prod-objects", "prod-default");
    }

    @Test
    public void listContainerNames() throws Exception {

        // Given
        BucketTopologyHelper bucketTopologyHelper = loadTopology("topology-test.conf");

        // When / Then
        assertThat(bucketTopologyHelper.listContainerNames("admin-metadata")).containsExactlyInAnyOrder(
            "1_unit", "1_objectGroup");
        assertThat(bucketTopologyHelper.listContainerNames("prod-default")).isEqualTo(
            Streams.concat(
                Arrays.stream(DataCategory.values())
                    .filter(
                        dataCategory -> !Arrays.asList(DataCategory.UNIT, DataCategory.OBJECTGROUP, DataCategory.OBJECT)
                            .contains(dataCategory))
                    .map(dataCategory -> "2_" + dataCategory.getFolder()),
                Arrays.stream(DataCategory.values())
                    .filter(
                        dataCategory -> !Arrays.asList(DataCategory.UNIT, DataCategory.OBJECTGROUP, DataCategory.OBJECT)
                            .contains(dataCategory))
                    .map(dataCategory -> "3_" + dataCategory.getFolder())
            ).collect(Collectors.toSet())
        );
    }

    @Test
    public void getTarBufferingTimeoutInMinutes() throws Exception {

        // Given
        BucketTopologyHelper bucketTopologyHelper = loadTopology("topology-test.conf");

        // When / Then
        assertThat(bucketTopologyHelper.getTarBufferingTimeoutInMinutes("test")).isEqualTo(1);
        assertThat(bucketTopologyHelper.getTarBufferingTimeoutInMinutes("admin")).isEqualTo(10);
        assertThat(bucketTopologyHelper.getTarBufferingTimeoutInMinutes("prod")).isEqualTo(60);
    }

    private BucketTopologyHelper loadTopology(String s) throws java.io.IOException {
        TapeLibraryTopologyConfiguration configuration = PropertiesUtils.readYaml(PropertiesUtils
            .getResourcePath(s), TapeLibraryTopologyConfiguration.class);

        return new BucketTopologyHelper(configuration);
    }
}

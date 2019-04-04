package fr.gouv.vitam.common.storage.tapelibrary;

import fr.gouv.vitam.common.PropertiesUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

// FIXME: 04/04/19 fix this tests after merge
@Ignore("To be fixed")
public class TapeLibraryConfConfigurationTest {

    public static final String OFFER_TAPE_TEST_CONF = "offer-tape-test.conf";

    @Test
    public void testLoadOfferTapeConfOK() throws IOException {

        TapeLibraryConfiguration configuration =
            PropertiesUtils.readYaml(PropertiesUtils.findFile(OFFER_TAPE_TEST_CONF),
                TapeLibraryConfiguration.class);
        assertThat(configuration).isNotNull();

        assertThat(configuration.getTapeLibraries()).isNotEmpty();
        assertThat(configuration.getTapeLibraries().keySet()).hasSize(2);
        assertThat(configuration.getTapeLibraries().keySet()).contains("TAPE_LIB_1", "TAPE_LIB_2");
        assertThat(configuration.getTapeLibraries().values()).hasSize(2);
        Iterator<TapeLibraryConf> tapeLibraryIt = configuration.getTapeLibraries().values().iterator();


        TapeLibraryConf tapeLibraryConf = tapeLibraryIt.next();
        assertThat(tapeLibraryConf.getRobots()).hasSize(2);
        assertThat(tapeLibraryConf.getDrives()).hasSize(4);

        assertThat(tapeLibraryConf.getRobots())
            .extracting("device")
            .contains("/dev/sg3", "/dev/sg4");

        assertThat(tapeLibraryConf.getDrives())
            .extracting("device", "mtPath", "ddPath", "tarPath", "timeoutInMilliseconds")
            .contains(
                tuple("/dev/nst1", "/bin/mt", "/bin/dd", "/bin/tar", 60000L),
                tuple("/dev/nst2", "mt", "dd", "tar", 40000L),
                tuple("/dev/nst3", "mt", "dd", "tar", 20000L),
                tuple("/dev/nst4", "mt", "dd", "tar", 60000L)
            );


        tapeLibraryConf = tapeLibraryIt.next();
        assertThat(tapeLibraryConf.getRobots()).hasSize(2);
        assertThat(tapeLibraryConf.getDrives()).hasSize(4);

        assertThat(tapeLibraryConf.getRobots())
            .extracting("device")
            .contains("/dev/sg5", "/dev/sg6");

        assertThat(tapeLibraryConf.getDrives())
            .extracting("device", "mtPath", "ddPath", "tarPath", "timeoutInMilliseconds")
            .contains(
                tuple("/dev/nst5", "mt", "dd", "tar", 60000L),
                tuple("/dev/nst6", "mt", "dd", "tar", 60000L),
                tuple("/dev/nst7", "mt", "dd", "tar", 60000L),
                tuple("/dev/nst8", "mt", "dd", "tar", 60000L)
            );

    }
}

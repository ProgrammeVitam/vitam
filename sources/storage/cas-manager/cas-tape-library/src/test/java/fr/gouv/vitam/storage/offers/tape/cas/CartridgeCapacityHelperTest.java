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
package fr.gouv.vitam.storage.offers.tape.cas;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.storage.tapelibrary.TapeLibraryConf;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CartridgeCapacityHelperTest {

    @Test
    public void testConfigurationLoadingKoDuplicateType() throws Exception {
        TapeLibraryConf conf = loadConf("cartridge-capacity-test-ko-duplicate-type.conf");
        assertThatThrownBy(() -> new CartridgeCapacityHelper(conf))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testConfigurationLoadingKoEmptyConf() throws Exception {
        TapeLibraryConf conf = loadConf("cartridge-capacity-test-ko-empty-conf.conf");
        assertThatThrownBy(() -> new CartridgeCapacityHelper(conf))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testConfigurationLoadingKoEmptyType() throws Exception {
        TapeLibraryConf conf = loadConf("cartridge-capacity-test-ko-empty-type.conf");
        assertThatThrownBy(() -> new CartridgeCapacityHelper(conf))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testConfigurationLoadingKoMissingCapacity() throws Exception {
        TapeLibraryConf conf = loadConf("cartridge-capacity-test-ko-missing-capacity.conf");
        assertThatThrownBy(() -> new CartridgeCapacityHelper(conf))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testConfigurationLoadingKoMissingConf() throws Exception {
        TapeLibraryConf conf = loadConf("cartridge-capacity-test-ko-missing-conf.conf");
        assertThatThrownBy(() -> new CartridgeCapacityHelper(conf))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testConfigurationLoadingKoMissingThreshold() throws Exception {
        TapeLibraryConf conf = loadConf("cartridge-capacity-test-ko-missing-threshold.conf");
        assertThatThrownBy(() -> new CartridgeCapacityHelper(conf))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testConfigurationLoadingKoMissingType() throws Exception {
        TapeLibraryConf conf = loadConf("cartridge-capacity-test-ko-missing-type.conf");
        assertThatThrownBy(() -> new CartridgeCapacityHelper(conf))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testConfigurationLoadingKoNegativeCapacity() throws Exception {
        TapeLibraryConf conf = loadConf("cartridge-capacity-test-ko-negative-capacity.conf");
        assertThatThrownBy(() -> new CartridgeCapacityHelper(conf))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testConfigurationLoadingKoNullEntry() throws Exception {
        TapeLibraryConf conf = loadConf("cartridge-capacity-test-ko-null-entry.conf");
        assertThatThrownBy(() -> new CartridgeCapacityHelper(conf))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testConfigurationLoadingKoTooHighCapacity() throws Exception {
        TapeLibraryConf conf = loadConf("cartridge-capacity-test-ko-too-high-capacity.conf");
        assertThatThrownBy(() -> new CartridgeCapacityHelper(conf))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testConfigurationLoadingKoTooHighThreshold() throws Exception {
        TapeLibraryConf conf = loadConf("cartridge-capacity-test-ko-too-high-threshold.conf");
        assertThatThrownBy(() -> new CartridgeCapacityHelper(conf))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testConfigurationLoadingTooLowThreshold() throws Exception {
        TapeLibraryConf conf = loadConf("cartridge-capacity-test-ko-too-low-threshold.conf");
        assertThatThrownBy(() -> new CartridgeCapacityHelper(conf))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testConfigurationLoadingOK() throws Exception {
        TapeLibraryConf conf = loadConf("cartridge-capacity-test-ok.conf");
        assertThatCode(() -> new CartridgeCapacityHelper(conf))
            .doesNotThrowAnyException();
    }

    @Test
    public void testGetThresholdOfTapeOK() throws Exception {
        TapeLibraryConf conf = loadConf("cartridge-capacity-test-ok.conf");
        CartridgeCapacityHelper cartridgeCapacityHelper = new CartridgeCapacityHelper(conf);

        Long fullTapeOccupationThreshold = cartridgeCapacityHelper.getFullTapeOccupationThreshold("LTO-6");

        assertThat(fullTapeOccupationThreshold).isNotNull();
        assertThat(fullTapeOccupationThreshold).isEqualTo(2_250_000_000_000L);
    }

    @Test
    public void testGetThresholdOfUnknownCartridgeTypeThenNull() throws Exception {
        TapeLibraryConf conf = loadConf("cartridge-capacity-test-ok.conf");
        CartridgeCapacityHelper cartridgeCapacityHelper = new CartridgeCapacityHelper(conf);

        Long fullTapeOccupationThreshold = cartridgeCapacityHelper.getFullTapeOccupationThreshold("UNKNOWN");
        assertThat(fullTapeOccupationThreshold).isNull();
    }

    private TapeLibraryConf loadConf(String resourcesFile) throws IOException {
        return PropertiesUtils.readYaml(
            PropertiesUtils.getConfigAsStream(resourcesFile),
            TapeLibraryConf.class);
    }
}
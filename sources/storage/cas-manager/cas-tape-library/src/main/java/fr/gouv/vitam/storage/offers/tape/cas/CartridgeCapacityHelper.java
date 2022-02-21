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

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.storage.tapelibrary.CartridgeCapacityConfiguration;
import fr.gouv.vitam.common.storage.tapelibrary.TapeLibraryConf;

import java.util.HashMap;
import java.util.Map;

public class CartridgeCapacityHelper {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CartridgeCapacityHelper.class);

    private static final int MIN_FULL_TAPE_THRESHOLD_PERCENTAGE = 1;
    private static final int MAX_FULL_TAPE_THRESHOLD_PERCENTAGE = 1000;

    private static final int MIN_CARTRIDGE_TYPE_CAPACITY_IN_MB = 1;
    private static final int MAX_CARTRIDGE_TYPE_CAPACITY_IN_MB = 1_000_000_000;
    public static final long MB_TO_BYTES = 1_000_000L;

    private int fullCartridgeDetectionThresholdInPercentage;
    private Map<String, Integer> cartridgeCapacityByType;

    public CartridgeCapacityHelper(TapeLibraryConf configuration) {

        ParametersChecker.checkParameter("Invalid conf. Missing tape library configuration", configuration);

        ParametersChecker.checkParameter("Invalid conf. Missing full cartridge detection threshold percentage",
            configuration.getFullCartridgeDetectionThresholdInPercentage());

        if (configuration.getFullCartridgeDetectionThresholdInPercentage() < MIN_FULL_TAPE_THRESHOLD_PERCENTAGE ||
            configuration.getFullCartridgeDetectionThresholdInPercentage() > MAX_FULL_TAPE_THRESHOLD_PERCENTAGE) {
            throw new IllegalArgumentException(
                "Invalid conf. Full cartridge detection threshold percentage must be between " +
                    MIN_FULL_TAPE_THRESHOLD_PERCENTAGE + " and " + MAX_FULL_TAPE_THRESHOLD_PERCENTAGE);
        }
        this.fullCartridgeDetectionThresholdInPercentage =
            configuration.getFullCartridgeDetectionThresholdInPercentage();

        ParametersChecker.checkParameter("Invalid conf. Missing full cartridge capacity configuration",
            configuration.getCartridgeCapacities());
        if (configuration.getCartridgeCapacities().isEmpty()) {
            throw new IllegalArgumentException("Invalid conf. Empty full cartridge capacity configuration");
        }

        this.cartridgeCapacityByType = new HashMap<>();
        for (CartridgeCapacityConfiguration cartridgeCapacity : configuration.getCartridgeCapacities()) {
            ParametersChecker.checkParameter("Invalid conf. Missing full cartridge capacity configuration",
                cartridgeCapacity);
            ParametersChecker.checkParameter("Invalid conf. Missing full cartridge capacity type",
                cartridgeCapacity.getCartridgeType());
            ParametersChecker.checkParameter("Invalid conf. Missing full cartridge capacity size",
                cartridgeCapacity.getCartridgeCapacityInMB());

            if (cartridgeCapacity.getCartridgeCapacityInMB() < MIN_CARTRIDGE_TYPE_CAPACITY_IN_MB ||
                cartridgeCapacity.getCartridgeCapacityInMB() > MAX_CARTRIDGE_TYPE_CAPACITY_IN_MB) {
                throw new IllegalArgumentException(
                    "Invalid conf. Missing full cartridge capacity must be between " +
                        MIN_FULL_TAPE_THRESHOLD_PERCENTAGE + " and " + MAX_FULL_TAPE_THRESHOLD_PERCENTAGE);
            }

            if (cartridgeCapacityByType.containsKey(cartridgeCapacity.getCartridgeType())) {
                throw new IllegalArgumentException(
                    "Invalid conf. Duplicate cartridge type '" + cartridgeCapacity.getCartridgeType() + "'");
            }

            cartridgeCapacityByType.put(cartridgeCapacity.getCartridgeType(),
                cartridgeCapacity.getCartridgeCapacityInMB());
        }
    }

    public Long getFullTapeOccupationThreshold(String cartridgeType) {
        if (!this.cartridgeCapacityByType.containsKey(cartridgeType)) {
            LOGGER.error("Unknown cartridge type '" + cartridgeType + "'");
            return null;
        }
        return this.cartridgeCapacityByType.get(cartridgeType) * MB_TO_BYTES
            * this.fullCartridgeDetectionThresholdInPercentage / 100L;
    }
}

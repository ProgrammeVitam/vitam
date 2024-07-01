/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */

package fr.gouv.vitam.common.error;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test class for error code enum validation. Modify this class only if you exactly know what you do !
 */
public class CodeTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CodeTest.class);
    private static final String ERROR_SERVICE = "[SERVICE] ";
    private static final String ERROR_DOMAIN = "[DOMAIN] ";
    private static final String ERROR_CODE_VITAM = "[CODE_VITAM] ";
    private static final String MESSAGE_CODE_RANGE = "{%s} code not in range [A-Z0-9] for {%s}";
    private static final String MESSAGE_CODE_LENGTH = "{%s} with {%s} code must have two characters alphanumerics";
    private static final String DUPLICATE_MESSAGE = " {%s} code for {%s} is duplicated with {%s}";

    /**
     * Validate alphanumeric format of service code [A-Z0-9] with size 2
     */
    @Test
    public void serviceCodeValidation() {
        boolean error = false;
        char c;
        for (final ServiceName service : ServiceName.values()) {
            if (service.getCode().length() != 2) {
                error = true;
                LOGGER.error(ERROR_SERVICE + String.format(MESSAGE_CODE_LENGTH, service.name(), service.getCode()));
            } else {
                for (int i = 0; i < service.getCode().length(); i++) {
                    c = service.getCode().charAt(i);
                    if ((c < 0x30 || c > 0x39) && (c < 0x41 || c > 0x5a)) {
                        error = true;
                        LOGGER.error(
                            ERROR_SERVICE + String.format(MESSAGE_CODE_RANGE, service.getCode(), service.name())
                        );
                        break;
                    }
                }
            }
        }
        assertFalse(error);
    }

    /**
     * Validate alphanumeric format of domain code [A-Z0-9] with size 2
     */
    @Test
    public void domainCodeValidation() {
        boolean error = false;
        char c;
        for (final DomainName domain : DomainName.values()) {
            if (domain.getCode().length() != 2) {
                error = true;
                LOGGER.error(ERROR_DOMAIN + String.format(MESSAGE_CODE_LENGTH, domain.name(), domain.getCode()));
            }
            for (int i = 0; i < domain.getCode().length(); i++) {
                c = domain.getCode().charAt(i);
                if ((c < 0x30 || c > 0x39) && (c < 0x41 || c > 0x5a)) {
                    error = true;
                    LOGGER.error(ERROR_DOMAIN + String.format(MESSAGE_CODE_RANGE, domain.getCode(), domain.name()));
                    break;
                }
            }
        }
        assertFalse(error);
    }

    /**
     * Validate alphanumeric format of vitam code [A-Z0-9] with size 2
     */
    @Test
    public void codeVitamCodeValidation() {
        boolean error = false;
        char c;
        for (final VitamCode vitamCode : VitamCode.values()) {
            if (vitamCode.getItem().length() != 2) {
                error = true;
                LOGGER.error(
                    ERROR_CODE_VITAM + String.format(MESSAGE_CODE_LENGTH, vitamCode.name(), vitamCode.getItem())
                );
            }
            for (int i = 0; i < vitamCode.getItem().length(); i++) {
                c = vitamCode.getItem().charAt(i);
                if ((c < 0x30 || c > 0x39) && (c < 0x41 || c > 0x5a)) {
                    error = true;
                    LOGGER.error(
                        ERROR_CODE_VITAM + String.format(MESSAGE_CODE_RANGE, vitamCode.getItem(), vitamCode.name())
                    );
                    break;
                }
            }
        }
        assertFalse(error);
    }

    /**
     * Check duplicated service code
     */
    @Test
    public void serviceCodeDuplicateValidation() {
        final Map<String, String> enumCodesNames = new HashMap<>();
        final List<String> messages = new ArrayList<>();
        for (final ServiceName service : ServiceName.values()) {
            if (enumCodesNames.containsKey(service.getCode())) {
                messages.add(
                    String.format(
                        DUPLICATE_MESSAGE,
                        service.getCode(),
                        service.name(),
                        enumCodesNames.get(service.getCode())
                    )
                );
            } else {
                enumCodesNames.put(service.getCode(), service.name());
            }
        }
        assertTrue(ERROR_SERVICE + messages.stream().map(s -> s).collect(Collectors.joining("\n")), messages.isEmpty());
    }

    /**
     * Check duplicated domain code
     */
    @Test
    public void domainCodeDuplicateValidation() {
        final Map<String, String> enumCodesNames = new HashMap<>();
        final List<String> messages = new ArrayList<>();
        for (final DomainName domain : DomainName.values()) {
            if (enumCodesNames.containsKey(domain.getCode())) {
                messages.add(
                    String.format(
                        DUPLICATE_MESSAGE,
                        domain.getCode(),
                        domain.name(),
                        enumCodesNames.get(domain.getCode())
                    )
                );
            } else {
                enumCodesNames.put(domain.getCode(), domain.name());
            }
        }
        assertTrue(ERROR_DOMAIN + messages.stream().map(s -> s).collect(Collectors.joining("\n")), messages.isEmpty());
    }

    /**
     * Check duplicated vitam code
     */
    @Test
    public void vitamCodeDuplicateValidation() {
        final Map<String, String> enumCodesNames = new HashMap<>();
        final List<String> messages = new ArrayList<>();
        String code;
        for (final VitamCode vitamCode : VitamCode.values()) {
            code = vitamCode.getService().getCode() + vitamCode.getDomain().getCode() + vitamCode.getItem();
            if (enumCodesNames.containsKey(code)) {
                messages.add(String.format(DUPLICATE_MESSAGE, code, vitamCode.name(), enumCodesNames.get(code)));
            } else {
                enumCodesNames.put(code, vitamCode.name());
            }
        }
        assertTrue(
            ERROR_CODE_VITAM + messages.stream().map(s -> s).collect(Collectors.joining("\n")),
            messages.isEmpty()
        );
    }
}

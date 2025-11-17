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

package fr.gouv.vitam.collect.internal.core.helpers;

import fr.gouv.vitam.collect.internal.core.common.CollectErrorMessagesEnum;
import fr.gouv.vitam.collect.internal.core.common.CollectErrorParamEnum;
import fr.gouv.vitam.common.error.VitamErrorDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractErrorAccumulator<E extends Exception> implements AutoCloseable {

    private final List<String> errorMessages = new ArrayList<>();
    private final List<VitamErrorDetails> errorDetails = new ArrayList<>();
    private final int maxErrorCount;

    protected AbstractErrorAccumulator(int maxErrorCount) {
        this.maxErrorCount = maxErrorCount;
    }

    public void reportOneError(
        CollectErrorMessagesEnum errorMessageKey,
        Map<CollectErrorParamEnum, String> errorMessageParams
    ) throws E {
        errorMessages.add(CollectErrorDetailHelper.generateErrorMessage(errorMessageKey, errorMessageParams));
        errorDetails.add(CollectErrorDetailHelper.generateVitamErrorsDetails(errorMessageKey, errorMessageParams));
        if (errorMessages.size() >= maxErrorCount) {
            throwException();
        }
    }

    private void throwException() throws E {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            if (errorMessages.size() == 1) {
                stringBuilder.append("1 error:");
            } else if (errorMessages.size() < maxErrorCount) {
                stringBuilder.append(errorMessages.size()).append(" errors:");
            } else {
                stringBuilder.append("At least ").append(errorMessages.size()).append(" errors:");
            }

            for (String errorMessage : errorMessages) {
                stringBuilder.append("\n- ").append(errorMessage);
            }

            throw buildException(stringBuilder.toString(), errorDetails);
        } finally {
            errorMessages.clear();
        }
    }

    protected abstract E buildException(String errorMessage, List<VitamErrorDetails> errorDetails);

    @Override
    public void close() throws E {
        if (!errorMessages.isEmpty()) {
            throwException();
        }
    }
}

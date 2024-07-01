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
package fr.gouv.vitam.common.mockito;

import org.apache.commons.collections4.CollectionUtils;
import org.mockito.ArgumentMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;

/**
 * Mockito Matcher that checks unordered list equality to avoid order validation.
 */
public class UnorderedListMatcher<T extends Comparable<? super T>> implements ArgumentMatcher<List<T>> {

    private final List<T> left;

    public UnorderedListMatcher(List<T> expected) {
        this.left = expected;
    }

    @Override
    public boolean matches(List<T> right) {
        if (this.left == null) {
            return right == null;
        }
        if (CollectionUtils.size(left) != CollectionUtils.size(right)) {
            return false;
        }
        List<T> leftSet = new ArrayList<>(left);
        List<T> rightSet = new ArrayList<>(right);
        leftSet.sort(Comparator.naturalOrder());
        rightSet.sort(Comparator.naturalOrder());
        return leftSet.equals(rightSet);
    }

    @SafeVarargs
    public static <T extends Comparable<? super T>> List<T> eqUnorderedList(T... items) {
        return argThat(new UnorderedListMatcher<>(Arrays.asList(items)));
    }
}

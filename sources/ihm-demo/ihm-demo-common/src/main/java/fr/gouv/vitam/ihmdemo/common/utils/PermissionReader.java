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
package fr.gouv.vitam.ihmdemo.common.utils;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.subject.Subject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * read permission of resources class
 */
public class PermissionReader {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PermissionReader.class);

    /**
     * return the all permissions for vitam
     *
     * @param type
     * @param annotation
     * @return set of String
     */
    public static Set<String> getMethodsAnnotatedWith(
        final Class<?> type,
        final Class<? extends RequiresPermissions> annotation
    ) {
        final Set<String> methods = new HashSet<>();
        Class<?> klass = type;
        while (klass != Object.class) { // need to iterated thought hierarchy in order to retrieve methods from above the current instance
            // iterate though the list of methods declared in the class represented by klass variable, and add those annotated with the specified annotation
            final List<Method> allMethods = new ArrayList<>(Arrays.asList(klass.getDeclaredMethods()));
            for (final Method method : allMethods) {
                if (method.isAnnotationPresent(annotation)) {
                    RequiresPermissions annotInstance = method.getAnnotation(annotation);
                    methods.addAll(Lists.newArrayList(annotInstance.value()));
                }
            }
            // move to the upper class in the hierarchy in search for more methods
            klass = klass.getSuperclass();
        }
        return methods;
    }

    /**
     * filter permissions for a specific user
     *
     * @param permissions
     * @param subject
     * @return list of String
     */
    public static List<String> filterPermission(Collection<String> permissions, Subject subject) {
        ArrayList<String> filteringPermissions = new ArrayList<>();

        for (String permission : permissions) {
            try {
                subject.checkPermission(permission);
                filteringPermissions.add(permission);
            } catch (AuthorizationException e) {
                LOGGER.debug("user has no permission {}", permission);
            }
        }

        return filteringPermissions;
    }
}

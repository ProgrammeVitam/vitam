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
package fr.gouv.vitam.model.validation.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class PojoModelExtractor {

    public List<PojoModel> extractPojoModels(Class clazz) {
        return extractPojoModels(clazz, "");
    }

    private List<PojoModel> extractPojoModels(Class clazz, String currentPath) {
        // List all fields recursively
        Class currentClazz = clazz;
        List<Field> fields = new ArrayList<>();
        while (currentClazz != null) {
            fields.addAll(Arrays.asList(currentClazz.getDeclaredFields()));
            currentClazz = currentClazz.getSuperclass();
        }

        List<PojoModel> models = new ArrayList<>();

        for (Field field : fields) {
            field.setAccessible(true);
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            String fieldIdentifier = getFieldIdentifier(field);
            boolean isArray = isArray(field);

            Class<?> entryClass = getEntryClass(field);

            PojoModelType modelType = getModelType(entryClass);

            String fullPath = currentPath.isEmpty() ? fieldIdentifier : currentPath + "." + fieldIdentifier;

            models.add(new PojoModel(fieldIdentifier, fullPath, isArray, modelType));

            if (modelType == PojoModelType.OBJECT) {
                models.addAll(extractPojoModels(entryClass, fullPath));
            }
        }
        return models;
    }

    private PojoModelType getModelType(Class<?> currentClazz) {
        if (currentClazz == Boolean.class || currentClazz == boolean.class) {
            return PojoModelType.BOOLEAN;
        }

        if (currentClazz == String.class) {
            return PojoModelType.STRING;
        }

        if (
            currentClazz == Long.class ||
            currentClazz == long.class ||
            currentClazz == Integer.class ||
            currentClazz == int.class ||
            currentClazz == BigInteger.class
        ) {
            return PojoModelType.LONG;
        }

        if (currentClazz == Double.class || currentClazz == double.class || currentClazz == BigDecimal.class) {
            return PojoModelType.DOUBLE;
        }

        if (currentClazz.isEnum()) {
            return PojoModelType.ENUM;
        }

        return PojoModelType.OBJECT;
    }

    private Class<?> getEntryClass(Field field) {
        Class<?> currentClazz;
        if (field.getType().isArray()) {
            currentClazz = field.getType().getComponentType();
        } else if (Collection.class.isAssignableFrom(field.getType())) {
            ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
            currentClazz = (Class<?>) parameterizedType.getActualTypeArguments()[0];
        } else {
            currentClazz = field.getType();
        }
        return currentClazz;
    }

    private String getFieldIdentifier(Field field) {
        JsonProperty[] annotationsByType = field.getAnnotationsByType(JsonProperty.class);
        if (annotationsByType.length == 0) {
            throw new IllegalStateException(
                "Missing JsonProperty annotation for field " +
                field.getName() +
                " for class " +
                field.getDeclaringClass().getCanonicalName()
            );
        }
        return annotationsByType[0].value();
    }

    private boolean isArray(Field field) {
        return field.getType().isArray() || Collection.class.isAssignableFrom(field.getType());
    }
}

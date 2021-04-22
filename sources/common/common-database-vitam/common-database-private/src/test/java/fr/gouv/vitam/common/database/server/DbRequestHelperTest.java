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
package fr.gouv.vitam.common.database.server;

import com.google.common.collect.Lists;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoWriteException;
import com.mongodb.WriteError;
import com.mongodb.bulk.BulkWriteError;
import fr.gouv.vitam.common.exception.DatabaseException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DbRequestHelperTest {

    @Test
    public void testIsDuplicateKeyErrorOnMongoWriteException() {
        MongoWriteException mongoWriteException = mock(MongoWriteException.class);
        WriteError writeErrorMock = mock(WriteError.class);
        when(writeErrorMock.getCategory()).thenReturn(ErrorCategory.DUPLICATE_KEY);
        when(mongoWriteException.getError()).thenReturn(writeErrorMock);
        // Case duplicate key
        assertThat(DbRequestHelper.isDuplicateKeyError(mongoWriteException)).isTrue();

        // Other Case
        when(writeErrorMock.getCategory()).thenReturn(ErrorCategory.EXECUTION_TIMEOUT);
        assertThat(DbRequestHelper.isDuplicateKeyError(mongoWriteException)).isFalse();
    }

    @Test
    public void testIsDuplicateKeyErrorOnMongoBulkWriteException() {
        MongoBulkWriteException mongoWriteException = mock(MongoBulkWriteException.class);
        BulkWriteError writeErrorMock = mock(BulkWriteError.class);
        when(writeErrorMock.getCategory()).thenReturn(ErrorCategory.DUPLICATE_KEY);
        when(mongoWriteException.getWriteErrors()).thenReturn(Lists.newArrayList(writeErrorMock));
        // Case duplicate key
        assertThat(DbRequestHelper.isDuplicateKeyError(mongoWriteException)).isTrue();

        // Other Case
        when(writeErrorMock.getCategory()).thenReturn(ErrorCategory.EXECUTION_TIMEOUT);
        assertThat(DbRequestHelper.isDuplicateKeyError(mongoWriteException)).isFalse();
    }

    @Test
    public void testIsDuplicateKeyErrorOnDataBaseException() {
        assertThat(DbRequestHelper.isDuplicateKeyError(mock(DatabaseException.class))).isFalse();
    }

    @Test
    public void testIsDuplicateKeyErrorOnDataBaseExceptionCauseMongoWriteException() {
        MongoWriteException mongoWriteException = mock(MongoWriteException.class);
        WriteError writeErrorMock = mock(WriteError.class);
        when(writeErrorMock.getCategory()).thenReturn(ErrorCategory.DUPLICATE_KEY);
        when(mongoWriteException.getError()).thenReturn(writeErrorMock);

        DatabaseException databaseException = mock(DatabaseException.class);
        when(databaseException.getCause()).thenReturn(mongoWriteException);

        assertThat(DbRequestHelper.isDuplicateKeyError(databaseException)).isTrue();

        when(writeErrorMock.getCategory()).thenReturn(ErrorCategory.EXECUTION_TIMEOUT);
        assertThat(DbRequestHelper.isDuplicateKeyError(databaseException)).isFalse();
    }

    @Test
    public void testIsDuplicateKeyErrorOnDataBaseExceptionCauseMongoBulkWriteException() {
        MongoBulkWriteException mongoWriteException = mock(MongoBulkWriteException.class);
        BulkWriteError writeErrorMock = mock(BulkWriteError.class);
        when(writeErrorMock.getCategory()).thenReturn(ErrorCategory.DUPLICATE_KEY);
        when(mongoWriteException.getWriteErrors()).thenReturn(Lists.newArrayList(writeErrorMock));

        DatabaseException databaseException = mock(DatabaseException.class);
        when(databaseException.getCause()).thenReturn(mongoWriteException);

        assertThat(DbRequestHelper.isDuplicateKeyError(databaseException)).isTrue();

        when(writeErrorMock.getCategory()).thenReturn(ErrorCategory.EXECUTION_TIMEOUT);
        assertThat(DbRequestHelper.isDuplicateKeyError(databaseException)).isFalse();
    }
}

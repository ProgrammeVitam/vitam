/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.thread;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.annotation.Annotation;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Junit Test rule used to run tests with a given {@link Executor} ; such tests should be annotated with the
 * {@link RunWithCustomExecutor} annotation.
 *
 * Mainly designed to allow tests to be run inside VitamThread
 *
 * Usage example : <code>
 * public class ExampleTest {
 *
 *   @Rule
 *   public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());
 *
 *   @Test
 *   @RunWithCustomExecutor
 *   public void runInVitamThreadTest() {
 *     [...]
 *   }
 * }
 * </code>
 *
 * @see RunWithCustomExecutor
 */
public class RunWithCustomExecutorRule implements TestRule, ClassRule {

    private final ExecutorService executor;

    /**
     * Note : the lifecycle of the the executor should be managed outside this class.
     *
     * @param executor The executor to use to run tests in.
     */
    public RunWithCustomExecutorRule(ExecutorService executor) {
        this.executor = executor;
    }


    @Override
    public Statement apply(Statement base, Description description) {
        // Restricts the rule application to the tests annotated with the relevant annotation
        if (description.getAnnotation(RunWithCustomExecutor.class) != null) {
            return new RunInVitamThreadStatement(base);
        } else {
            return base;
        }
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return RunWithCustomExecutor.class;
    }

    /**
     * Statement used to launch decorated statement in a VitamThread
     */
    private class RunInVitamThreadStatement extends Statement {

        private final Statement baseStatement;
        private volatile Throwable throwable;

        public RunInVitamThreadStatement(Statement base) {
            baseStatement = base;
        }

        @Override
        public void evaluate() throws Throwable {
            // Submit work in another thread
            final Future<?> future = executor.submit(() -> {
                try {
                    baseStatement.evaluate();
                } catch (final Throwable t) {
                    throwable = t;
                }
            });
            // Wait for result
            future.get();
            // Recatch exception if needed
            if (throwable != null) {
                throw throwable;
            }
        }
    }

    @Override
    public int order() {
        return Rule.DEFAULT_ORDER;
    }
}

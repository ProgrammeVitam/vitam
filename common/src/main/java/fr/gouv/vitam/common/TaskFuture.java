/**
 * This file is part of Vitam Project.
 *
 * Copyright 2009, Frederic Bregier, and individual contributors by the author
 * tags. See the COPYRIGHT.txt in the distribution for a full listing of
 * individual contributors.
 *
 * All Vitam Project is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Vitam is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Vitam . If not, see <http://www.gnu.org/licenses/>.
 */
package fr.gouv.vitam.common;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.TimeUnit;

/**
 * Task Future operation<br>
 * Completely inspired from the excellent ChannelFuture of Netty, but without
 * any channel inside.
 * 
 * 
 * 
 */
public class TaskFuture {
    private static final Throwable CANCELLED = new Throwable();

    private final boolean cancellable;

    private boolean done;

    private Throwable cause;

    private int waiters;

    /**
     * Creates a new instance.
     * 
     */
    public TaskFuture() {
        cancellable = false;
    }

    /**
     * Creates a new instance.
     * 
     * @param cancellable
     *            {@code true} if and only if this future can be canceled
     */
    public TaskFuture(boolean cancellable) {
        this.cancellable = cancellable;
    }

    /**
     * Returns {@code true} if and only if this future is complete, regardless
     * of whether the operation was successful, failed, or canceled.
     * 
     * @return True if the future is complete
     */
    public synchronized boolean isDone() {
        return done;
    }

    /**
     * Returns {@code true} if and only if the operation was completed
     * successfully.
     * 
     * @return True if the future is successful
     */
    public synchronized boolean isSuccess() {
        return done && cause == null;
    }

    /**
     * Returns {@code true} if and only if the operation was completed but
     * unsuccessfully.
     * 
     * @return True if the future is done but unsuccessful
     */
    public synchronized boolean isFailed() {
        return cause != null;
    }

    /**
     * Returns the cause of the failed operation if the operation has failed.
     * 
     * @return the cause of the failure. {@code null} if succeeded or this
     *         future is not completed yet.
     */
    public synchronized Throwable getCause() {
        if (cause != CANCELLED) {
            return cause;
        }
        return null;
    }

    /**
     * Returns {@code true} if and only if this future was canceled by a
     * {@link #cancel()} method.
     * 
     * @return True if the future was canceled
     */
    public synchronized boolean isCancelled() {
        return cause == CANCELLED;
    }

    /**
     * Rethrows the exception that caused this future fail if this future is
     * complete and failed.
     * 
     * @return this
     * @throws Exception
     */
    public TaskFuture rethrowIfFailed() throws Exception {
        if (!isDone()) {
            return this;
        }

        Throwable cause = getCause();
        if (cause == null) {
            return this;
        }

        if (cause instanceof Exception) {
            throw (Exception) cause;
        }

        if (cause instanceof Error) {
            throw (Error) cause;
        }

        throw new RuntimeException(cause);
    }

    /**
     * Waits for this future to be completed.
     * 
     * @return The TaskFuture
     * 
     * @throws InterruptedException
     *             if the current thread was interrupted
     */
    public TaskFuture await() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        synchronized (this) {
            while (!done) {
                waiters++;
                try {
                    this.wait();
                } finally {
                    waiters--;
                }
            }
        }
        return this;
    }

    /**
     * Waits for this future to be completed within the specified time limit.
     * 
     * @param timeout
     * @param unit
     * 
     * @return {@code true} if and only if the future was completed within the
     *         specified time limit
     * 
     * @throws InterruptedException
     *             if the current thread was interrupted
     */
    public boolean await(long timeout, TimeUnit unit)
            throws InterruptedException {
        return await0(unit.toNanos(timeout), true);
    }

    /**
     * Waits for this future to be completed within the specified time limit.
     * 
     * @param timeoutMillis
     * 
     * @return {@code true} if and only if the future was completed within the
     *         specified time limit
     * 
     * @throws InterruptedException
     *             if the current thread was interrupted
     */
    public boolean await(long timeoutMillis) throws InterruptedException {
        return await0(MILLISECONDS.toNanos(timeoutMillis), true);
    }

    /**
     * Waits for this future to be completed without interruption. This method
     * catches an {@link InterruptedException} and discards it silently.
     * 
     * @return The TaskFuture
     */
    public TaskFuture awaitUninterruptibly() {
        boolean interrupted = false;
        synchronized (this) {
            while (!done) {
                waiters++;
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    interrupted = true;
                } finally {
                    waiters--;
                }
            }
        }

        if (interrupted) {
            Thread.currentThread().interrupt();
        }

        return this;
    }

    /**
     * Waits for this future to be completed within the specified time limit
     * without interruption. This method catches an {@link InterruptedException}
     * and discards it silently.
     * 
     * @param timeout
     * @param unit
     * 
     * @return {@code true} if and only if the future was completed within the
     *         specified time limit
     */
    public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
        try {
            return await0(unit.toNanos(timeout), false);
        } catch (InterruptedException e) {
            throw new InternalError();
        }
    }

    /**
     * Waits for this future to be completed within the specified time limit
     * without interruption. This method catches an {@link InterruptedException}
     * and discards it silently.
     * 
     * @param timeoutMillis
     * 
     * @return {@code true} if and only if the future was completed within the
     *         specified time limit
     */
    public boolean awaitUninterruptibly(long timeoutMillis) {
        try {
            return await0(MILLISECONDS.toNanos(timeoutMillis), false);
        } catch (InterruptedException e) {
            throw new InternalError();
        }
    }

    private boolean await0(long timeoutNanos, boolean interruptable)
            throws InterruptedException {
        if (interruptable && Thread.interrupted()) {
            throw new InterruptedException();
        }

        long startTime = timeoutNanos <= 0 ? 0 : System.nanoTime();
        long waitTime = timeoutNanos;
        boolean interrupted = false;

        try {
            synchronized (this) {
                if (done) {
                    return done;
                } else if (waitTime <= 0) {
                    return done;
                }

                waiters++;
                try {
                    for (;;) {
                        try {
                            this.wait(waitTime / 1000000,
                                    (int) (waitTime % 1000000));
                        } catch (InterruptedException e) {
                            if (interruptable) {
                                throw e;
                            } else {
                                interrupted = true;
                            }
                        }

                        if (done) {
                            return true;
                        }
                        waitTime = timeoutNanos - (System.nanoTime() - startTime);
                        if (waitTime <= 0) {
                            return done;
                        }
                    }
                } finally {
                    waiters--;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Marks this future as a success and notifies all listeners.
     * 
     * @return {@code true} if and only if successfully marked this future as a
     *         success. Otherwise {@code false} because this future is already
     *         marked as either a success or a failure.
     */
    public boolean setSuccess() {
        synchronized (this) {
            // Allow only once.
            if (done) {
                return false;
            }

            done = true;
            if (waiters > 0) {
                notifyAll();
            }
        }
        return true;
    }

    /**
     * Marks this future as a failure and notifies all listeners.
     * 
     * @param cause
     * @return {@code true} if and only if successfully marked this future as a
     *         failure. Otherwise {@code false} because this future is already
     *         marked as either a success or a failure.
     */
    public boolean setFailure(Throwable cause) {
        synchronized (this) {
            // Allow only once.
            if (done) {
                return false;
            }

            this.cause = cause;
            done = true;
            if (waiters > 0) {
                notifyAll();
            }
        }
        return true;
    }

    /**
     * Cancels the operation associated with this future and notifies all
     * listeners if canceled successfully.
     * 
     * @return {@code true} if and only if the operation has been canceled.
     *         {@code false} if the operation can't be canceled or is already
     *         completed.
     */
    public boolean cancel() {
        if (!cancellable) {
            return false;
        }
        synchronized (this) {
            // Allow only once.
            if (done) {
                return false;
            }

            cause = CANCELLED;
            done = true;
            if (waiters > 0) {
                notifyAll();
            }
        }
        return true;
    }

    /**
     * Experimental: try to re-enable the future
     */
    public void reset() {
        synchronized (this) {
            this.done = false;
            this.cause = null;
        }
    }
}

/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.utilities.java.support.service;

import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.Duration;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponent;

import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for {@link ReloadableService}. This base class will use a background thread that will perform a periodic
 * check, via {@link #shouldReload()}, and, if required, invoke the services {@link #reload()} method. <br/>
 * 
 * This class does <em>not</em> deal with any synchronization. That is left to implementing classes.
 * 
 * @param <T> The sort of service this implements.
 */
public abstract class AbstractReloadableService<T> extends AbstractIdentifiableInitializableComponent implements
        ReloadableService<T>, UnmodifiableComponent {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractReloadableService.class);

    /**
     * Number of milliseconds between one reload check and another. A value of 0 or less indicates that no reloading
     * will be performed. Default value: 300,000 (5*60*1000, or 5 minutes)
     */
    @Duration private long reloadCheckDelay = 300000;

    /** Timer used to schedule configuration reload tasks. */
    @Nullable private Timer reloadTaskTimer;

    /** Watcher that monitors the set of configuration resources for this service for changes. */
    @Nullable private ServiceReloadTask reloadTask;

    /** The last time time the service was reloaded, whether successful or not. */
    @Nullable private DateTime lastReloadInstant;

    /** The last time the service was reloaded successfully. */
    @Nullable private DateTime lastSuccessfulReleaseIntant;

    /** The cause of the last reload failure, if the last reload failed. */
    @Nullable private Throwable reloadFailureCause;

    /** Do we fail immediately if the config is bogus? */
    private boolean failFast;

    /** The log prefix. */
    @Nullable private String logPrefix;

    /**
     * Gets the number of milliseconds between one reload check and another. A value of 0 or less indicates that no
     * reloading will be performed.
     * 
     * @return number of milliseconds between one reload check and another
     */
    public long getReloadCheckDelay() {
        return reloadCheckDelay;
    }

    /**
     * Sets the number of milliseconds between one reload check and another. A value of 0 or less indicates that no
     * reloading will be performed.
     * 
     * This setting can not be changed after the service has been initialized.
     * 
     * @param delay number of milliseconds between one reload check and another
     */
    public void setReloadCheckDelay(@Duration final long delay) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        reloadCheckDelay = delay;
    }

    /**
     * Gets the timer used to schedule configuration reload tasks.
     * 
     * @return timer used to schedule configuration reload tasks
     */
    @Nullable public Timer getReloadTaskTimer() {
        return reloadTaskTimer;
    }

    /**
     * Sets the timer used to schedule configuration reload tasks.
     * 
     * This setting can not be changed after the service has been initialized.
     * 
     * @param timer timer used to schedule configuration reload tasks
     */
    public void setReloadTaskTimer(@Nullable final Timer timer) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        reloadTaskTimer = timer;
    }

    /** {@inheritDoc} */
    @Override @Nullable public DateTime getLastReloadAttemptInstant() {
        return lastReloadInstant;
    }

    /** {@inheritDoc} */
    @Override @Nullable public DateTime getLastSuccessfulReloadInstant() {
        return lastSuccessfulReleaseIntant;
    }

    /** {@inheritDoc} */
    @Override @Nullable public Throwable getReloadFailureCause() {
        return reloadFailureCause;
    }

    /**
     * Do we fail fast?
     * 
     * @return Returns whether we fast.
     */
    public boolean isFailFast() {
        return failFast;
    }

    /**
     * Sets whether we fail fast.
     * 
     * @param value what to set.
     */
    public void setFailFast(final boolean value) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        failFast = value;
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        log.info("{} Performing initial load", getLogPrefix());
        try {
            doReload();
            lastSuccessfulReleaseIntant = new DateTime(ISOChronology.getInstanceUTC());
        } catch (final ServiceException e) {
            if (isFailFast()) {
                throw new ComponentInitializationException(getLogPrefix() + " could not perform initial load", e);
            }
            log.error("{} Initial load failed", getLogPrefix(), e);
            if (reloadCheckDelay > 0) {
                log.info("{} Continuing to poll configuration", getLogPrefix());
            } else {
                log.error("{} No further attempts will be made to reload", getLogPrefix());
            }
        }

        if (reloadCheckDelay > 0) {
            if (null == reloadTaskTimer) {
                log.info("{} No reload task timer specified, creating default", getLogPrefix());
                reloadTaskTimer = new Timer("Timer for " + getId());
            }
            log.info("{} Reload time set to: {}, starting refresh thread", getLogPrefix(), reloadCheckDelay);
            reloadTask = new ServiceReloadTask();
            reloadTaskTimer.schedule(reloadTask, reloadCheckDelay, reloadCheckDelay);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void doDestroy() {
        log.info("{} Starting shutdown", getLogPrefix());
        super.doDestroy();
        if (reloadTask != null) {
            reloadTask.cancel();
        }
        log.info("{} Completing shutdown", getLogPrefix());
    }

    /**
     * {@inheritDoc}
     * 
     */
    @Override public final void reload() {

        final DateTime now = new DateTime(ISOChronology.getInstanceUTC());
        lastReloadInstant = now;

        try {
            doReload();

            lastSuccessfulReleaseIntant = now;
        } catch (final ServiceException e) {
            log.error("{} Reload for {} failed", getLogPrefix(), getId(), e);
            reloadFailureCause = e;
        }
    }

    /**
     * Called by the {@link ServiceReloadTask} to determine if the service should be reloaded.
     * 
     * <p>
     * No lock is held when this method is called, so any locking needed should be handled internally.
     * </p>
     * 
     * @return true iff the service should be reloaded
     */
    protected abstract boolean shouldReload();

    /**
     * Performs the actual reload.
     * 
     * <p>
     * No lock is held when this method is called, so any locking needed should be handled internally.
     * </p>
     * 
     * @throws ServiceException thrown if there is a problem reloading the service
     */
    protected void doReload() {
        log.info("{} Reloading service configuration", getLogPrefix());
    }

    /**
     * Return a string which is to be prepended to all log messages.
     * 
     * @return "Service '<definitionID>' :"
     */
    @Nonnull @NotEmpty protected String getLogPrefix() {
        // local cache of cached entry to allow unsynchronised clearing of per class cache.
        String prefix = logPrefix;
        if (null == prefix) {
            StringBuilder builder = new StringBuilder("Service '").append(getId()).append("':");
            prefix = builder.toString();
            if (null == logPrefix) {
                logPrefix = prefix;
            }
        }
        return prefix;
    }

    /**
     * A watcher that determines if a service should be reloaded and does so as appropriate.
     */
    protected class ServiceReloadTask extends TimerTask {

        /** {@inheritDoc} */
        @Override public void run() {

            if (shouldReload()) {
                reload();
            }
        }
    }
}
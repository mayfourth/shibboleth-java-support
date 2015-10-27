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

package net.shibboleth.utilities.java.support.security;

import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.crypto.SecretKey;

import net.shibboleth.utilities.java.support.annotation.Duration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonNegative;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.resource.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implements a strategy for access to versioned symmetric keys using a keystore, and a standalone file
 * for tracking the latest key version, to compensate for the lack of extensible attribute support in the
 * pre-Java 8 KeyStore API.
 * 
 * <p>The separate resource must be a Java properties file containing a {@link #CURRENT_VERSION_PROP}
 * property pointing to the latest key version.</p> 
 */
public class BasicKeystoreKeyStrategy extends AbstractInitializableComponent implements DataSealerKeyStrategy {

    /** Name of property representing current key version. */
    @Nonnull @NotEmpty public static final String CURRENT_VERSION_PROP = "CurrentVersion";
    
    /** Class logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(BasicKeystoreKeyStrategy.class);

    /** Type of keystore to use for access to keys. */
    @NonnullAfterInit private String keystoreType;

    /** Keystore resource. */
    @NonnullAfterInit private Resource keystoreResource;

    /** Version resource. */
    @NonnullAfterInit private Resource keyVersionResource;
    
    /** Password for keystore. */
    @NonnullAfterInit private String keystorePassword;

    /** Keystore base alias for encryption keys. */
    @NonnullAfterInit private String keyAlias;

    /** Password for encryption key(s). */
    @NonnullAfterInit private String keyPassword;

    /** Current key alias loaded. */
    @NonnullAfterInit private String currentAlias;

    /** Current default key loaded. */
    @NonnullAfterInit private SecretKey defaultKey;
    
    /** Number of milliseconds between key update checks. Default value: (PT15M). */
    @Duration @NonNegative private long updateInterval;

    /** Timer used to schedule update tasks. */
    private Timer updateTaskTimer;

    /** Timer used to schedule update tasks if no external one set. */
    private Timer internalTaskTimer;

    /** Task that checks for updated key version. */
    private TimerTask updateTask;
    
    /** Constructor. */
    public BasicKeystoreKeyStrategy() {
        keystoreType = "JCEKS";
        updateInterval = 15 * 60 * 1000L;
    }
    
    /**
     * Set the keystore type.
     * 
     * @param type the keystore type
     */
    public void setKeystoreType(@Nonnull @NotEmpty final String type) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        keystoreType = Constraint.isNotNull(StringSupport.trimOrNull(type), "Keystore type cannot be null or empty");
    }

    /**
     * Set the keystore resource.
     * 
     * @param resource the keystore resource
     */
    public void setKeystoreResource(@Nonnull @NotEmpty final Resource resource) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        keystoreResource = Constraint.isNotNull(resource, "Keystore resource cannot be null");
    }

    /**
     * Set the key version resource.
     * 
     * @param resource the key version resource
     */
    public void setKeyVersionResource(@Nonnull @NotEmpty final Resource resource) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        keyVersionResource = Constraint.isNotNull(resource, "Key version resource cannot be null");
    }
    
    /**
     * Set the keystore password.
     * 
     * @param password the keystore password
     */
    public void setKeystorePassword(@Nonnull @NotEmpty final String password) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        keystorePassword = Constraint.isNotNull(password, "Keystore password cannot be null");
    }

    /**
     * Set the encryption key alias base name.
     * 
     * @param alias the encryption key alias base
     */
    public void setKeyAlias(@Nonnull @NotEmpty final String alias) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        keyAlias = Constraint.isNotNull(StringSupport.trimOrNull(alias),
                "Key alias base cannot be null or empty");
    }

    /**
     * Set the encryption key password.
     * 
     * @param password the encryption key password
     */
    public void setKeyPassword(@Nonnull @NotEmpty final String password) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        keyPassword = Constraint.isNotNull(password, "Key password cannot be null");
    }

    /**
     * Set the number of milliseconds between key update checks. A value of 0 indicates that no updates will be
     * performed.
     * 
     * This setting cannot be changed after the service has been initialized.
     * 
     * @param interval number of milliseconds between key update checks
     */
    public void setUpdateInterval(@Duration @NonNegative final long interval) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        updateInterval = Constraint.isGreaterThanOrEqual(0, interval,
                "Update interval must be greater than or equal to zero");
    }

    /**
     * Set the timer used to schedule update tasks.
     * 
     * This setting cannot be changed after the service has been initialized.
     * 
     * @param timer timer used to schedule update tasks
     */
    public void setUpdateTaskTimer(@Nullable final Timer timer) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        updateTaskTimer = timer;
    }
    
    /** {@inheritDoc} */
    @Override
    public void doInitialize() throws ComponentInitializationException {
        try {
            try {
                Constraint.isNotNull(keystoreType, "Keystore type cannot be null");
                Constraint.isNotNull(keystoreResource, "Keystore resource cannot be null");
                Constraint.isNotNull(keyVersionResource, "Key version resource cannot be null");
                Constraint.isNotNull(keystorePassword, "Keystore password cannot be null");
                Constraint.isNotNull(keyAlias, "Key alias base cannot be null");
                Constraint.isNotNull(keyPassword, "Key password cannot be null");
            } catch (final ConstraintViolationException e) {
                throw new ComponentInitializationException(e);
            }
            
            updateDefaultKey();
    
        } catch (final KeyException e) {
            log.error("Error loading default key from base name '{}'", keyAlias, e);
            throw new ComponentInitializationException("Exception loading the default key", e);
        }

        if (updateInterval > 0) {
            updateTask = new TimerTask() {
                @Override
                public void run() {
                    try {
                        updateDefaultKey();
                    } catch (final KeyException e) {
                        
                    }
                }
            };
            if (updateTaskTimer == null) {
                internalTaskTimer = new Timer(true);
            } else {
                internalTaskTimer = updateTaskTimer;
            }
            internalTaskTimer.schedule(updateTask, updateInterval, updateInterval);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void doDestroy() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
            if (updateTaskTimer == null) {
                internalTaskTimer.cancel();
            }
            internalTaskTimer = null;
        }
        super.doDestroy();
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull public Pair<String,SecretKey> getDefaultKey() throws KeyException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        
        synchronized(this) {
            return new Pair(currentAlias, defaultKey);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    @Nonnull public SecretKey getKey(@Nonnull @NotEmpty final String name) throws KeyException {
        
        synchronized(this) {
            if (defaultKey != null && name.equals(currentAlias)) {
                return defaultKey;
            }
        }

        try {
            final KeyStore ks = KeyStore.getInstance(keystoreType);
            ks.load(keystoreResource.getInputStream(), keystorePassword.toCharArray());

            final Key loadedKey = ks.getKey(name, keyPassword.toCharArray());
            if (loadedKey == null) {
                log.info("Key '{}' not found", name);
                throw new KeyNotFoundException("Key was not present in keystore");
            } else if (!(loadedKey instanceof SecretKey)) {
                log.error("Key '{}' is not a symmetric key", name);
                throw new KeyException("Key was of incorrect type");
            }
            return (SecretKey) loadedKey;
        } catch (final KeyStoreException | NoSuchAlgorithmException | CertificateException
                    | IOException | UnrecoverableKeyException e) {
            log.error("Error loading key named '{}'", name, e);
            throw new KeyException(e);
        }
    }

    /**
     * Update the loaded copy of the default key based on the current key version if it's out of date
     * (loading key version from scratch if need be).
     * 
     * @throws KeyException if the key cannot be updated
     */
    private void updateDefaultKey() throws KeyException {
        
        synchronized(this) {
            try (final InputStream is = keyVersionResource.getInputStream()) {
                // Refresh the key version and compare to the current one.
                final Properties props = new Properties();
                props.load(is);
                
                final StringBuilder builder = new StringBuilder(keyAlias);
                builder.append(props.getProperty(CURRENT_VERSION_PROP, ""));
                                
                final String newAlias = builder.toString();
                
                if (currentAlias == null) {
                    log.info("Loading initial default key: {}", newAlias);
                } else if (!currentAlias.equals(newAlias)) {
                    log.info("Updating default key from {} to {}", currentAlias, newAlias);
                } else {
                    log.debug("Default key version has not changed, still {}", currentAlias);
                    return;
                }
                
                // Try and load the new key and update the alias.
                defaultKey = getKey(newAlias);
                currentAlias = newAlias;
                
                log.info("Default key updated to {}", currentAlias);
                
            } catch (final IOException e) {
                log.error("IOException updating key version", e);
                throw new KeyException(e);
            }
        }
    }
    
}
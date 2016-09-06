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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyException;
import java.security.SecureRandom;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.crypto.SecretKey;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.apache.commons.codec.BinaryDecoder;
import org.apache.commons.codec.BinaryEncoder;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Applies a MAC to time-limited information and encrypts with a symmetric key.
 */
public class DataSealer extends AbstractInitializableComponent {

    /** Size of UTF-8 data chunks to read/write. */
    private static final int CHUNK_SIZE = 60000;
    
    /** Class logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(DataSealer.class);

    /** Source of keys. */
    @NonnullAfterInit private DataSealerKeyStrategy keyStrategy;

    /** Source of secure random data. */
    @NonnullAfterInit private SecureRandom random;

    /** Encodes encrypted bytes to string. */
    @Nonnull private BinaryEncoder encoder = new Base64(0, new byte[] { '\n' });

    /** Decodes encrypted string to bytes. */
    @Nonnull private BinaryDecoder decoder = (Base64) encoder;


    /**
     * Set the key strategy.
     * 
     * @param strategy key strategy
     */
    public void setKeyStrategy(@Nonnull final DataSealerKeyStrategy strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        keyStrategy = Constraint.isNotNull(strategy, "DataSealerKeyStrategy cannot be null");
    }
    
    /**
     * Set the pseudorandom generator.
     * 
     * @param r the pseudorandom generator to set
     */
    public void setRandom(@Nonnull final SecureRandom r) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        random = Constraint.isNotNull(r, "SecureRandom cannot be null");
    }

    /**
     * Sets the encoder to use to produce a ciphertext string from bytes. Default is standard base-64 encoding without
     * line breaks.
     *
     * @param e Byte-to-string encoder.
     */
    public void setEncoder(@Nonnull final BinaryEncoder e) {
        encoder = Constraint.isNotNull(e, "Encoder cannot be null");
    }

    /**
     * Sets the decoder to use to convert a ciphertext string to bytes. Default is standard base-64 decoding.
     *
     * @param d String-to-byte decoder.
     */
    public void setDecoder(@Nonnull final BinaryDecoder d) {
        decoder = Constraint.isNotNull(d, "Decoder cannot be null");
    }

    /** {@inheritDoc} */
    public void doInitialize() throws ComponentInitializationException {
        try {
            try {
                Constraint.isNotNull(keyStrategy, "Keystore type cannot be null");
            } catch (final ConstraintViolationException e) {
                throw new ComponentInitializationException(e);
            }
            
            if (random == null) {
                random = new SecureRandom();
            }

            final SecretKey initialKey = keyStrategy.getDefaultKey().getSecond();

            // Before we finish initialization, make sure that things are working.
            testEncryption(initialKey);

        } catch (final KeyException e) {
            log.error(e.getMessage());
            throw new ComponentInitializationException("Exception loading the keystore", e);
        } catch (final DataSealerException e) {
            log.error(e.getMessage());
            throw new ComponentInitializationException("Exception testing the encryption settings used", e);
        }
    }

    /**
     * Decrypts and verifies an encrypted bundle created with {@link #wrap(String, long)}.
     * 
     * @param wrapped the encoded blob
     * 
     * @return the decrypted data, if it's unexpired
     * @throws DataSealerException if the data cannot be unwrapped and verified
     */
    @Nonnull public String unwrap(@Nonnull @NotEmpty final String wrapped) throws DataSealerException {

        return unwrap(wrapped, null);
    }
    
    /**
     * Decrypts and verifies an encrypted bundle created with {@link #wrap(String, long)}, optionally
     * returning the label of the key used to encrypt the data.
     * 
     * @param wrapped the encoded blob
     * @param keyUsed a buffer to receive the alias of the key used to encrypt the data
     * 
     * @return the decrypted data, if it's unexpired
     * @throws DataSealerException if the data cannot be unwrapped and verified
     */
    @Nonnull public String unwrap(@Nonnull @NotEmpty final String wrapped, @Nullable final StringBuffer keyUsed)
            throws DataSealerException {

        try {
            final byte[] in = decoder.decode(wrapped.getBytes(StandardCharsets.UTF_8));

            final ByteArrayInputStream inputByteStream = new ByteArrayInputStream(in);
            final DataInputStream inputDataStream = new DataInputStream(inputByteStream);
            
            // Extract alias of key, and load if necessary.
            final String keyAlias = inputDataStream.readUTF();
            log.trace("Data was encrypted by key named '{}'", keyAlias);
            if (keyUsed != null) {
                keyUsed.append(keyAlias);
            }
            final SecretKey key = keyStrategy.getKey(keyAlias);
            
            final GCMBlockCipher cipher = new GCMBlockCipher(new AESEngine());
            
            // Load the IV.
            final int ivSize = cipher.getUnderlyingCipher().getBlockSize();
            final byte[] iv = new byte[ivSize];
            inputDataStream.readFully(iv);

            final AEADParameters aeadParams =
                    new AEADParameters(new KeyParameter(key.getEncoded()), 128, iv, keyAlias.getBytes());
            cipher.init(false, aeadParams);

            // Data can't be any bigger than the original minus IV.
            final byte[] data = new byte[in.length - ivSize];
            final int dataSize = inputDataStream.read(data);
            
            final byte[] plaintext = new byte[cipher.getOutputSize(dataSize)];
            final int outputLen = cipher.processBytes(data, 0, dataSize, plaintext, 0);
            cipher.doFinal(plaintext, outputLen);
            
            // Pass the plaintext into the subroutine for processing.
            return extractAndCheckDecryptedData(plaintext);

        } catch (final IllegalStateException | InvalidCipherTextException| IOException | DecoderException e) {
            log.error("Exception unwrapping data", e);
            throw new DataSealerException("Exception unwrapping data", e);
        } catch (final KeyNotFoundException e) {
            if (keyUsed != null) {
                log.info("Data was wrapped with a key ({}) no longer available", keyUsed.toString());
            } else {
                log.info("Data was wrapped with a key no longer available");
            }
            throw new DataExpiredException("Data wrapped with expired key");
        } catch (final KeyException e) {
            log.error(e.getMessage());
            throw new DataSealerException("Exception loading key", e);
        }
    }

    /**
     * Extract the GZIP'd data and test for expiration before returning it.
     * 
     * @param decryptedBytes the data we are looking at
     * 
     * @return the decoded data if it is valid and unexpired
     * @throws DataSealerException if the data cannot be unwrapped and verified
     */
    @Nonnull private String extractAndCheckDecryptedData(@Nonnull @NotEmpty final byte[] decryptedBytes)
            throws DataSealerException {
        
        try {
            final ByteArrayInputStream byteStream = new ByteArrayInputStream(decryptedBytes);
            final GZIPInputStream compressedData = new GZIPInputStream(byteStream);
            final DataInputStream dataInputStream = new DataInputStream(compressedData);

            final long decodedExpirationTime = dataInputStream.readLong();
            if (System.currentTimeMillis() > decodedExpirationTime) {
                log.debug("Unwrapped data has expired");
                throw new DataExpiredException("Unwrapped data has expired");
            }

            final StringBuffer accumulator = new StringBuffer();
            
            int count = 0;
            while (true) {
                try {
                    final String decodedData = dataInputStream.readUTF();
                    accumulator.append(decodedData);
                    log.trace("Read chunk #{} from output stream", ++count);
                } catch (final EOFException e) {
                    break;
                }
            }

            log.trace("Unwrapped data verified");
            return accumulator.toString();
        } catch (final IOException e) {
            log.error(e.getMessage());
            throw new DataSealerException("Caught IOException unwrapping data", e);
        }
    }

    /**
     * Encodes data into an AEAD-encrypted blob, gzip(exp|data)
     * 
     * <ul>
     * <li>exp = expiration time of the data; 8 bytes; Big-endian</li>
     * <li>data = the data; a UTF-8-encoded string</li>
     * </ul>
     * 
     * <p>As part of encryption, the key alias is supplied as additional authenticated data
     * to the cipher. Afterwards, the encrypted data is prepended by the IV and then again by the alias
     * (in length-prefixed UTF-8 format), which identifies the key used. Finally the result is base64-encoded.</p>
     * 
     * @param data the data to wrap
     * @param exp expiration time
     * @return the encoded blob
     * @throws DataSealerException if the wrapping operation fails
     */
    @Nonnull public String wrap(@Nonnull @NotEmpty final String data, final long exp) throws DataSealerException {

        if (data == null || data.length() == 0) {
            throw new IllegalArgumentException("Data must be supplied for the wrapping operation");
        }

        try {
            final GCMBlockCipher cipher = new GCMBlockCipher(new AESEngine());
            final byte[] iv = new byte[cipher.getUnderlyingCipher().getBlockSize()];
            random.nextBytes(iv);
            
            final Pair<String,SecretKey> defaultKey = keyStrategy.getDefaultKey();
            
            final AEADParameters aeadParams =
                    new AEADParameters(new KeyParameter(defaultKey.getSecond().getEncoded()), 128, iv,
                            defaultKey.getFirst().getBytes());
            cipher.init(true, aeadParams);

            final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            final GZIPOutputStream compressedStream = new GZIPOutputStream(byteStream);
            final DataOutputStream dataStream = new DataOutputStream(compressedStream);

            dataStream.writeLong(exp);
            
            int count = 0;
            int start = 0;
            final int dataLength = data.length();
            while (start < dataLength) {
                dataStream.writeUTF(data.substring(start, start + Math.min(dataLength - start, CHUNK_SIZE)));
                start += Math.min(dataLength - start, CHUNK_SIZE);
                log.trace("Wrote chunk #{} to output stream", ++count);
            }

            dataStream.flush();
            compressedStream.flush();
            compressedStream.finish();
            byteStream.flush();

            final byte[] plaintext = byteStream.toByteArray();
            
            final byte[] encryptedData = new byte[cipher.getOutputSize(plaintext.length)];
            int outputLen = cipher.processBytes(plaintext, 0, plaintext.length, encryptedData, 0);
            outputLen += cipher.doFinal(encryptedData, outputLen);

            final ByteArrayOutputStream finalByteStream = new ByteArrayOutputStream();
            final DataOutputStream finalDataStream = new DataOutputStream(finalByteStream);
            finalDataStream.writeUTF(defaultKey.getFirst());
            finalDataStream.write(iv);
            finalDataStream.write(encryptedData, 0, outputLen);
            finalDataStream.flush();
            finalByteStream.flush();

            return new String(encoder.encode(finalByteStream.toByteArray()), StandardCharsets.UTF_8);

        } catch (final Exception e) {
            log.error("Exception wrapping data", e);
            throw new DataSealerException("Exception wrapping data", e);
        }

    }

    /**
     * Run a test over the configured bean properties.
     * 
     * @param key   key to test
     * 
     * @throws DataSealerException if the test fails
     */
    private void testEncryption(@Nonnull final SecretKey key) throws DataSealerException {

        final String decrypted;
        try {
            final GCMBlockCipher cipher = new GCMBlockCipher(new AESEngine());
            final byte[] iv = new byte[cipher.getUnderlyingCipher().getBlockSize()];
            random.nextBytes(iv);
            final AEADParameters aeadParams = new AEADParameters(
                    new KeyParameter(key.getEncoded()), 128, iv, "aad".getBytes(StandardCharsets.UTF_8));
            
            cipher.init(true, aeadParams);
            byte[] plaintext = "test".getBytes(StandardCharsets.UTF_8);
            final byte[] encryptedData = new byte[cipher.getOutputSize(plaintext.length)];
            int outputLen = cipher.processBytes(plaintext, 0, plaintext.length, encryptedData, 0);
            cipher.doFinal(encryptedData, outputLen);

            cipher.init(false, aeadParams);
            plaintext = new byte[cipher.getOutputSize(encryptedData.length)];
            outputLen = cipher.processBytes(encryptedData, 0, encryptedData.length, plaintext, 0);
            cipher.doFinal(plaintext, outputLen);
            decrypted = Strings.fromUTF8ByteArray(plaintext);
            
        } catch (final IllegalStateException | InvalidCipherTextException e) {
            log.error("Round trip encryption/decryption test unsuccessful", e);
            throw new DataSealerException("Round trip encryption/decryption test unsuccessful", e);
        }

        if (decrypted == null || !"test".equals(decrypted)) {
            log.error("Round trip encryption/decryption test unsuccessful. Decrypted text did not match");
            throw new DataSealerException("Round trip encryption/decryption test unsuccessful");
        }
    }

}
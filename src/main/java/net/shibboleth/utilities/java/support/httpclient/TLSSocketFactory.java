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

package net.shibboleth.utilities.java.support.httpclient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.apache.http.HttpHost;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An implementation of HttpClient {@link LayeredConnectionSocketFactory} that is a factory
 * for TLS sockets.
 * 
 * <p>
 * This class is functionally modeled on {@link org.apache.http.conn.ssl.SSLConnectionSocketFactory},
 * but provides better support for subclassing, as well as specific additional features:
 * <ul>
 *   <li>Factory hostname verifier defaults to {@link StrictHostnameVerifier} rather than 
 *       {@link BrowserCompatHostnameVerifier}</li>
 *   <li>Per-request specification of enabled TLS protocols and cipher suites via {@link HttpContext} attributes.</li>
 *   <li>Per-request specification of hostname verifier via {@link HttpContext} attribute.</li>
 * </ul>
 * </p> 
 */
@ThreadSafe
public class TLSSocketFactory implements LayeredConnectionSocketFactory {
    
    /** HttpContext key for a a list of TLS protocols to enable on the socket.  
     * Must be an instance of {@link List}&lt;{@link String}&gt;. */
    public static final String CONTEXT_KEY_TLS_PROTOCOLS = "javasupport.TLSProtocols";
    
    /** HttpContext key for a a list of TLS cipher suites to enable on the socket.  
     * Must be an instance of {@link List}&lt;{@link String}&gt;. */
    public static final String CONTEXT_KEY_TLS_CIPHER_SUITES = "javasupport.TLSCipherSuites";
    
    /** HttpContext key for an instance of {@link X509HostnameVerifier}. */
    public static final String CONTEXT_KEY_HOSTNAME_VERIFIER = "javasupport.HostnameVerifier";

    /** Protocol: TLS. */
    public static final String TLS = "TLS";
    
    /** Protocol: SSL. */
    public static final String SSL = "SSL";
    
    /** Protocol: SSLv2. */
    public static final String SSLV2 = "SSLv2";

    /** Hostname verifier which passes all hostnames. */
    public static final X509HostnameVerifier ALLOW_ALL_HOSTNAME_VERIFIER = new AllowAllHostnameVerifier();

    /** Hostname verifier which implements a policy similar to most browsers. */
    public static final X509HostnameVerifier BROWSER_COMPATIBLE_HOSTNAME_VERIFIER = new BrowserCompatHostnameVerifier();

    /** Hostname verifier which implements a strict policy. */
    public static final X509HostnameVerifier STRICT_HOSTNAME_VERIFIER = new StrictHostnameVerifier();
    
    /** Logger. */
    private final Logger log = LoggerFactory.getLogger(TLSSocketFactory.class);

    /** Socket factory. */
    private final SSLSocketFactory socketfactory;
    
    /** Hostname verifier. */
    private final X509HostnameVerifier hostnameVerifier;
    
    /** Factory-wide supported protocols. */
    private final String[] supportedProtocols;
    
    /** Factory-wide supported cipher suites. */
    private final String[] supportedCipherSuites;

    /**
     * Constructor.
     *
     * @param sslContext the effective SSLContext instance
     */
    public TLSSocketFactory(
            @Nonnull final SSLContext sslContext) {
        this(sslContext, STRICT_HOSTNAME_VERIFIER);
    }

    /**
     * Constructor.
     *
     * @param sslContext the effective SSLContext instance
     * @param verifier the effective hostname verifier
     */
    public TLSSocketFactory(
            @Nonnull final SSLContext sslContext, 
            @Nullable final X509HostnameVerifier verifier) {
        this(Args.notNull(sslContext, "SSL context").getSocketFactory(), null, null, verifier);
    }

    /**
     * Constructor.
     *
     * @param sslContext the effective SSLContext instance
     * @param protocols the factory-wide enabled TLS protocols
     * @param cipherSuites the factory-wide enabled TLS cipher suites
     * @param verifier the effective hostname verifier
     */
    public TLSSocketFactory(
            @Nonnull final SSLContext sslContext,
            @Nullable final String[] protocols,
            @Nullable final String[] cipherSuites,
            @Nullable final X509HostnameVerifier verifier) {
        this(Args.notNull(sslContext, "SSL context").getSocketFactory(), protocols, cipherSuites, verifier);
    }

    /**
     * Constructor.
     *
     * @param factory the effective SSL socket factory
     * @param verifier the effective hostname verifier
     */
    public TLSSocketFactory(
            @Nonnull final SSLSocketFactory factory, 
            @Nullable final X509HostnameVerifier verifier) {
        this(factory, null, null, verifier);
    }

    /**
     * Constructor.
     *
     * @param factory the effective SSL socket factory
     * @param protocols the factory-wide enabled TLS protocols
     * @param cipherSuites the factory-wide enabled TLS cipher suites
     * @param verifier the effective hostname verifier
     */
    public TLSSocketFactory(
            @Nonnull final SSLSocketFactory factory,
            @Nullable final String[] protocols,
            @Nullable final String[] cipherSuites,
            @Nullable final X509HostnameVerifier verifier) {
        socketfactory = Args.notNull(factory, "SSL socket factory");
        supportedProtocols = protocols;
        supportedCipherSuites = cipherSuites;
        hostnameVerifier = verifier != null ? verifier : STRICT_HOSTNAME_VERIFIER;
    }

    /**
     * Get the JSSE socket factory instance.
     * 
     * @return the socket factory
     */
    @Nonnull protected SSLSocketFactory getSocketfactory() {
        return socketfactory;
    }
    
    /**
     * Get the configured hostname verifier.
     * 
     * @return the hostname verifier
     */
    @Nonnull protected X509HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    /**
     * Get the configured factory-wide supported protocols.
     * 
     * @return the configured protocols
     */
    @Nullable protected String[] getSupportedProtocols() {
        return supportedProtocols;
    }

    /**
     * Get the configured factory-wide supported cipher suites.
     * 
     * @return the configured cipher suites
     */
    @Nullable protected String[] getSupportedCipherSuites() {
        return supportedCipherSuites;
    }

    /**
     * Performs any custom initialization for a newly created SSLSocket
     * (before the SSL handshake happens).
     *
     * The default implementation is a no-op, but could be overridden to, e.g.,
     * call {@link javax.net.ssl.SSLSocket#setEnabledCipherSuites(String[])}.
     * 
     * @param socket the SSL socket instance being prepared
     * @param context the current HttpContext instance 
     * 
     * @throws IOException if there is an error customizing the socket
     */
    protected void prepareSocket(@Nonnull final SSLSocket socket, @Nullable final HttpContext context) 
            throws IOException {
        
    }

    /** {@inheritDoc} */
    @Nonnull public Socket createSocket(@Nullable final HttpContext context) throws IOException {
        log.trace("In createSocket");
        return SocketFactory.getDefault().createSocket();
    }

    // Checkstyle: ParameterNumber OFF
    /** {@inheritDoc} */
    public Socket connectSocket(
            final int connectTimeout,
            @Nullable final Socket socket,
            @Nonnull final HttpHost host,
            @Nonnull final InetSocketAddress remoteAddress,
            @Nullable final InetSocketAddress localAddress,
            @Nullable final HttpContext context) throws IOException {
        
        log.trace("In connectSocket");
        
        Args.notNull(host, "HTTP host");
        Args.notNull(remoteAddress, "Remote address");
        
        final Socket sock = socket != null ? socket : createSocket(context);
        if (localAddress != null) {
            sock.bind(localAddress);
        }
        try {
            if (connectTimeout > 0 && sock.getSoTimeout() == 0) {
                sock.setSoTimeout(connectTimeout);
            }
            sock.connect(remoteAddress, connectTimeout);
        } catch (final IOException ex) {
            try {
                sock.close();
            } catch (final IOException ignore) {
            }
            throw ex;
        }
        // Setup SSL layering if necessary
        if (sock instanceof SSLSocket) {
            final SSLSocket sslsock = (SSLSocket) sock;
            sslsock.startHandshake();
            verifyHostname(sslsock, host.getHostName(), context);
            return sock;
        } else {
            return createLayeredSocket(sock, host.getHostName(), remoteAddress.getPort(), context);
        }
    }
    // Checkstyle: ParameterNumber ON

    /** {@inheritDoc} */
    public Socket createLayeredSocket(
            @Nonnull final Socket socket,
            @Nonnull @NotEmpty final String target,
            final int port,
            @Nullable final HttpContext context) throws IOException {
        
        log.trace("In createLayeredSocket");
        
        final SSLSocket sslsock = (SSLSocket) getSocketfactory().createSocket(
                socket,
                target,
                port,
                true);
        
        final String[] contextProtocols = getListAttribute(context, CONTEXT_KEY_TLS_PROTOCOLS);
        if (contextProtocols != null) {
            sslsock.setEnabledProtocols(contextProtocols);
        } else if (getSupportedProtocols() != null) {
            sslsock.setEnabledProtocols(getSupportedProtocols());
        } else {
            // If supported protocols are not explicitly set, remove all SSL protocol versions
            final String[] allProtocols = sslsock.getSupportedProtocols();
            final List<String> enabledProtocols = new ArrayList<>(allProtocols.length);
            for (final String protocol: allProtocols) {
                if (!protocol.startsWith("SSL")) {
                    enabledProtocols.add(protocol);
                }
            }
            sslsock.setEnabledProtocols(enabledProtocols.toArray(new String[enabledProtocols.size()]));
        }
        
        final String[] contextCipherSuites = getListAttribute(context, CONTEXT_KEY_TLS_CIPHER_SUITES);
        if (contextCipherSuites != null) {
            sslsock.setEnabledCipherSuites(contextCipherSuites);
        } else if (getSupportedCipherSuites() != null) {
            sslsock.setEnabledCipherSuites(getSupportedCipherSuites());
        }
        
        prepareSocket(sslsock, context);
        sslsock.startHandshake();
        logSocketInfo(sslsock);
        verifyHostname(sslsock, target, context);
        return sslsock;
    }
    
    /**
     * Log various diagnostic information from the {@link SSLSocket} and {@link SSLSession}.
     * 
     * @param socket the SSLSocket instance
     */
    private void logSocketInfo(final SSLSocket socket) {
        final SSLSession session = socket.getSession();
        if (log.isDebugEnabled()) {
            log.debug("Connected to: {}", socket.getRemoteSocketAddress());
            
            log.debug("Supported protocols: {}", (Object)socket.getSupportedProtocols());
            log.debug("Enabled protocols:   {}", (Object)socket.getEnabledProtocols());
            log.debug("Selected protocol:   {}", session.getProtocol());
            
            log.debug("Supported cipher suites: {}", (Object)socket.getSupportedCipherSuites());
            log.debug("Enabled cipher suites:   {}", (Object)socket.getEnabledCipherSuites());
            log.debug("Selected cipher suite:   {}", session.getCipherSuite());
        }
        
        if (log.isTraceEnabled()) {
            try {
                log.trace("Peer principal: {}", session.getPeerPrincipal());
                log.trace("Peer certificates: {}", (Object)session.getPeerCertificates());
                log.trace("Local principal: {}", session.getLocalPrincipal());
                log.trace("Local certificates: {}", (Object)session.getLocalCertificates());
            } catch (final SSLPeerUnverifiedException e) {
                log.warn("SSL exception enumerating peer certificates", e);
            }
        }
    }

    /**
     * Get a normalized String array from a context attribute holding a {@link List}&lt;{@link String}&gt;.
     * 
     * @param context the current HttpContext
     * @param contextKey the attribute context key
     * 
     * @return a String array, or null
     */
    @Nullable protected String[] getListAttribute(@Nullable final HttpContext context,
            @Nonnull final String contextKey) {
        if (context == null) {
            return null;
        }
        final List<String> values = new ArrayList<>(StringSupport.normalizeStringCollection(
                (List<String>) context.getAttribute(contextKey)));
        if (values != null && !values.isEmpty()) {
            return values.toArray(new String[values.size()]);
        } else {
            return null;
        }
    }

    /**
     * Verify the peer's socket hostname against the supplied expected name.
     * 
     * @param sslsock the SSL socket being prepared
     * @param hostname the expected hostname
     * @param context the current HttpContext instance
     * 
     * @throws IOException if peer failed hostname verification, or if there was an error during verification
     */
    protected void verifyHostname(@Nonnull final SSLSocket sslsock, @Nonnull final String hostname, 
            @Nullable final HttpContext context) throws IOException {
        
        try {
            X509HostnameVerifier verifier = null;
            if (context != null) {
                verifier = (X509HostnameVerifier) context.getAttribute(CONTEXT_KEY_HOSTNAME_VERIFIER);
            }
            if (verifier == null) {
                verifier = getHostnameVerifier(); 
            }
            verifier.verify(hostname, sslsock);
        } catch (final IOException iox) {
            // close the socket before re-throwing the exception
            try {
                sslsock.close();
            } catch (final Exception x) {
                /*ignore*/
            }
            throw iox;
        }
    }

}

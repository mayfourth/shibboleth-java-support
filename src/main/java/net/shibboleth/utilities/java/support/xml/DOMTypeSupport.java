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

package net.shibboleth.utilities.java.support.xml;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;

/** Set of helper methods for working with DOM data types. */
public final class DOMTypeSupport {

    /** JAXP DatatypeFactory. */
    private static DatatypeFactory dataTypeFactory;

    /** Baseline for duration calculations (comes from XML Schema standard). */
    private static Calendar baseline;    
    
    /** Constructor. */
    private DOMTypeSupport() {
    }

    /**
     * Converts a lexical dateTime, as defined by XML Schema 1.0, into milliseconds since the epoch.
     * 
     * @param dateTime lexical date/time, may not be null
     * 
     * @return the date/time expressed as milliseconds since the epoch
     */
    public static long dateTimeToLong(@Nonnull final String dateTime) {
        final String trimmedString =
                Constraint.isNotNull(StringSupport.trimOrNull(dateTime), "Lexical dateTime may not be null or empty");

        final XMLGregorianCalendar calendar = dataTypeFactory.newXMLGregorianCalendar(trimmedString);
        return calendar.toGregorianCalendar().getTimeInMillis();
    }

    /**
     * Converts a lexical duration, as defined by XML Schema 1.0, into milliseconds.
     * 
     * @param duration lexical duration representation
     * 
     * @return duration in milliseconds
     */
    public static long durationToLong(final String duration) {
        return dataTypeFactory.newDuration(duration).getTimeInMillis(baseline);
    }

    /**
     * Converts a lexical duration, as defined by XML Schema 1.0, into milliseconds.
     * 
     * @param duration JAXP duration representation
     * 
     * @return duration in milliseconds
     */
    public static long durationToLong(final Duration duration) {
        return duration.getTimeInMillis(baseline);
    }
    
    /**
     * Gets a static instance of a JAXP DatatypeFactory.
     * 
     * @return the factory or null if the factory could not be created
     */
    public static DatatypeFactory getDataTypeFactory() {
        return dataTypeFactory;
    }

    /**
     * Gets the XSI type for a given element if it has one.
     * 
     * @param e the element
     * 
     * @return the type or null
     */
    @Nullable public static QName getXSIType(@Nullable final Element e) {
        if (hasXSIType(e)) {
            final Attr attribute = e.getAttributeNodeNS(XMLConstants.XSI_NS, "type");
            final String attributeValue = attribute.getTextContent().trim();
            return QNameSupport.constructQName(e, attributeValue);
        }
        return null;
    }

    /**
     * Checks if the given element has an xsi:type defined for it.
     * 
     * @param e the DOM element
     * 
     * @return true if there is a type, false if not
     */
    public static boolean hasXSIType(@Nullable final Element e) {
        if (e != null) {
            if (e.getAttributeNodeNS(XMLConstants.XSI_NS, "type") != null) {
                return true;
            }
        }

        return false;
    }

    /**
     * Converts a numerical date/time, given in milliseconds since the epoch, to a lexical dateTime defined by XML
     * Schema 1.0.
     * 
     * @param dateTime the date time to be converted
     * 
     * @return the lexical representation of the date/time
     */
    @Nonnull public static String longToDateTime(final long dateTime) {
        final GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(dateTime);

        return dataTypeFactory.newXMLGregorianCalendar(calendar).normalize().toXMLFormat();
    }

    /**
     * Converts a duration in milliseconds to a lexical duration, as defined by XML Schema 1.0.
     * 
     * @param duration the duration
     * 
     * @return the lexical representation
     */
    @Nonnull public static String longToDuration(final long duration) {
        return dataTypeFactory.newDuration(duration).toString();
    }

    static {
        try {
            dataTypeFactory = DatatypeFactory.newInstance();
            baseline = new GregorianCalendar(1696, 9, 1, 0, 0, 0);
        } catch (final DatatypeConfigurationException e) {
            throw new RuntimeException("JVM is required to support XML DatatypeFactory but it does not", e);
        }
    }
}
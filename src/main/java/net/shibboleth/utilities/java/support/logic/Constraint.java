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

package net.shibboleth.utilities.java.support.logic;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A helper class for evaluating certain constraints. Any violation will thrown a {@link ConstraintViolationException}.
 */
public final class Constraint {

    /** Constructor. */
    private Constraint() {
    }

    /**
     * Checks that the given collection is empty. If the collection is not empty an {@link ConstraintViolationException}
     * is thrown.
     * 
     * @param <T> type of items in the collection
     * @param collection collection to check
     * @param message message used in the {@link ConstraintViolationException}
     * 
     * @return the checked input
     */
    @Nonnull public static <T> Collection<T> isEmpty(@Nonnull final Collection<T> collection,
            @Nonnull final String message) {
        if (collection != null && !collection.isEmpty()) {
            throw new ConstraintViolationException(message);
        }

        return collection;
    }
    
    /**
     * Checks that the given boolean is false. If not an {@link ConstraintViolationException} is thrown.
     * 
     * @param b boolean to check
     * @param message message used in {@link ConstraintViolationException}
     * 
     * @return the checked boolean
     */
    public static boolean isFalse(final boolean b, @Nonnull final String message) {
        if (b) {
            throw new ConstraintViolationException(message);
        }

        return b;
    }

    /**
     * Checks that the given number is greater than a given threshold. If the number is not greater than the threshold
     * an {@link ConstraintViolationException} is thrown.
     * 
     * @param threshold the threshold
     * @param number the number to be checked
     * @param message message used in the {@link ConstraintViolationException}
     * 
     * @return the checked input
     */
    public static long isGreaterThan(final long threshold, final long number, @Nonnull final String message) {
        if (number <= threshold) {
            throw new ConstraintViolationException(message);
        }

        return number;
    }

    /**
     * Checks that the given number is greater than, or equal to, a given threshold. If the number is not greater than,
     * or equal to, the threshold an {@link ConstraintViolationException} is thrown.
     * 
     * @param threshold the threshold
     * @param number the number to be checked
     * @param message message used in the {@link ConstraintViolationException}
     * 
     * @return the checked input
     */
    public static long isGreaterThanOrEqual(final long threshold, final long number, @Nonnull final String message) {
        if (number < threshold) {
            throw new ConstraintViolationException(message);
        }

        return number;
    }

    /**
     * Checks that the given number is less than a given threshold. If the number is not less than the threshold an
     * {@link ConstraintViolationException} is thrown.
     * 
     * @param threshold the threshold
     * @param number the number to be checked
     * @param message message used in the {@link ConstraintViolationException}
     * 
     * @return the checked input
     */
    public static long isLessThan(final long threshold, final long number, @Nonnull final String message) {
        if (number >= threshold) {
            throw new ConstraintViolationException(message);
        }

        return number;
    }

    /**
     * Checks that the given number is less than, or equal to, a given threshold. If the number is not less than, or
     * equal to, the threshold an {@link ConstraintViolationException} is thrown.
     * 
     * @param threshold the threshold
     * @param number the number to be checked
     * @param message message used in the {@link ConstraintViolationException}
     * 
     * @return the checked input
     */
    public static long isLessThanOrEqual(final long threshold, final long number, @Nonnull final String message) {
        if(number > threshold){
            throw new ConstraintViolationException(message);
        }
        
        return number;
    }

    /**
     * Checks that the given collection is not empty. If the collection is empty an {@link ConstraintViolationException}
     * is thrown.
     * 
     * @param <T> type of items in the collection
     * @param collection collection to check
     * @param message message used in the {@link ConstraintViolationException}
     * 
     * @return the checked input
     */
    @Nonnull public static <T> Collection<T> isNotEmpty(@Nonnull final Collection<T> collection,
            @Nonnull final String message) {
        if (collection == null || collection.isEmpty()) {
            throw new ConstraintViolationException(message);
        }

        return collection;
    }

    /**
     * Checks that the given array is not empty. If the array is empty an {@link ConstraintViolationException}
     * is thrown.
     * 
     * @param <T> type of items in the array
     * @param array array to check
     * @param message message used in the {@link ConstraintViolationException}
     * 
     * @return the checked input
     */
    @Nonnull public static <T> T[] isNotEmpty(@Nonnull final T[] array,
            @Nonnull final String message) {
        if (array == null || array.length == 0) {
            throw new ConstraintViolationException(message);
        }

        return array;
    }

    /**
     * Checks that the given byte array is not empty. If the array is empty an {@link ConstraintViolationException}
     * is thrown.
     * 
     * @param array array to check
     * @param message message used in the {@link ConstraintViolationException}
     * 
     * @return the checked input
     */
    @Nonnull public static byte[] isNotEmpty(@Nonnull final byte[] array,
            @Nonnull final String message) {
        if (array == null || array.length == 0) {
            throw new ConstraintViolationException(message);
        }

        return array;
    }    
    
    /**
     * Checks that the given string is not empty. If the string is empty a {@link ConstraintViolationException}
     * is thrown.
     * 
     * @param string string to check
     * @param message message used in the {@link ConstraintViolationException}
     * 
     * @return the checked input
     */
    @Nonnull public static String isNotEmpty(@Nonnull final String string,
            @Nonnull final String message) {
        if (string == null || string.length() == 0) {
            throw new ConstraintViolationException(message);
        }

        return string;
    }

    /**
     * Checks that the given object is not null. If the object is null an {@link ConstraintViolationException} is
     * thrown.
     * 
     * @param <T> object type
     * @param obj object to check
     * @param message message used in {@link ConstraintViolationException}
     * 
     * @return the checked input
     */
    @Nonnull public static <T> T isNotNull(@Nullable final T obj, @Nonnull final String message) {
        if (obj == null) {
            throw new ConstraintViolationException(message);
        }

        return obj;
    }

    /**
     * Checks that the given object is null. If the object is not null an {@link ConstraintViolationException} is
     * thrown.
     * 
     * @param <T> object type
     * @param obj object to check
     * @param message message used in {@link ConstraintViolationException}
     * 
     * @return the checked input
     */
    @Nullable public static <T> T isNull(@Nullable final T obj, @Nonnull final String message) {
        if (obj != null) {
            throw new ConstraintViolationException(message);
        }

        return obj;

    }

    /**
     * Checks that the given boolean is true. If not an {@link ConstraintViolationException} is thrown.
     * 
     * @param b boolean to check
     * @param message message used in {@link ConstraintViolationException}
     * 
     * @return the checked boolean
     */
    public static boolean isTrue(final boolean b, @Nonnull final String message) {
        if (!b) {
            throw new ConstraintViolationException(message);
        }

        return b;
    }

    /**
     * Checks that the array does not contain any null elements.
     * 
     * @param <T> type of elements in the array
     * @param array array to check
     * @param message message used in the {@link ConstraintViolationException}
     * 
     * @return the given array
     */
    @Nonnull public static <T> T[] noNullItems(@Nonnull final T[] array, @Nonnull final String message) {
        if (array == null) {
            throw new ConstraintViolationException(message);
        }

        for (final T element : array) {
            if (element == null) {
                throw new ConstraintViolationException(message);
            }
        }

        return array;
    }

    /**
     * Checks that the collection does not contain any null elements.
     *
     * @param <T> type of collection to inspect.
     * @param collection to check.
     * @param message message used in the {@link ConstraintViolationException}
     *
     * @return the given array
     */
    @Nonnull public static <T extends Collection<?>> T noNullItems(@Nonnull final T collection,
            @Nonnull final String message) {
        if (collection == null) {
            throw new ConstraintViolationException(message);
        }

        for (final Object element : collection) {
            if (element == null) {
                throw new ConstraintViolationException(message);
            }
        }

        return collection;
    }

    /**
     * Checks that the given number is in the exclusive range. If the number is not in the range an
     * {@link ConstraintViolationException} is thrown.
     * 
     * @param lowerTheshold lower bound of the range
     * @param upperThreshold upper bound of the range
     * @param number number to check
     * @param message message used in the {@link ConstraintViolationException}
     * 
     * @return the checked input
     */
    public static long numberInRangeExclusive(final long lowerTheshold, final long upperThreshold, final long number,
            @Nonnull final String message) {
        if (number <= lowerTheshold || number >= upperThreshold) {
            throw new ConstraintViolationException(message);
        }

        return number;
    }

    /**
     * Checks that the given number is in the inclusive range. If the number is not in the range an
     * {@link ConstraintViolationException} is thrown.
     * 
     * @param lowerTheshold lower bound of the range
     * @param upperThreshold upper bound of the range
     * @param number number to check
     * @param message message used in the {@link ConstraintViolationException}
     * 
     * @return the checked input
     */
    public static long numberInRangeInclusive(final long lowerTheshold, final long upperThreshold, final long number,
            @Nonnull final String message) {
        if (number < lowerTheshold || number > upperThreshold) {
            throw new ConstraintViolationException(message);
        }

        return number;
    }
}
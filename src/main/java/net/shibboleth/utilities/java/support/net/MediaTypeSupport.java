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

package net.shibboleth.utilities.java.support.net;

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.net.MediaType;

import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Support methods for Guava {@link MediaType}.
 */
public final class MediaTypeSupport {
    
    /** Function to strip MediaType parameters. */
    private static final Function<MediaType, MediaType> STRIP_PARAMS = new StripMediaTypeParametersFunction();
    
    /** * Constructor. */
    private MediaTypeSupport() { }
    
    /**
     * Validate the specified Content-Type. 
     * 
     * <p>
     * 2 strategies are supported for evaluating a content type:
     * <ol>
     * <li>
     * If isOneOfStrategy is true, then the {@link MediaType} parsed from the content type value is compared to each 
     * of the specified valid types via {@link MediaType#is(MediaType)}. If any pass, the type is considered
     * valid.  This allows use of MediaType's support for wildcard and parameter evaluation.
     * </li>
     * <li>
     * If isOneOfStrategy is false, then the {@link MediaType} parsed from the value is stripped 
     * of its parameters, as is each of the valid types.  Then a simple evaluation is done that the 
     * specified content type is equal to one of the passed types. In this case, only literal types and subtypes 
     * should be passed as valid types; wildcards should not be used.
     * </li>
     * </ol>
     * </p>
     * 
     * @param contentType the contentType to be validated
     * @param validTypes the set of valid media types
     * @param noContentTypeIsValid flag whether the case of a missing/empty Content-Type header is considered valid
     * @param isOneOfStrategy flag for the strategy used in the validation (see above for details)
     * @return true if the content type is valid, false if not
     */
    public static boolean validateContentType(final String contentType, final Set<MediaType> validTypes, 
            final boolean noContentTypeIsValid, final boolean isOneOfStrategy) {
        
        final String contentTypeValue = StringSupport.trimOrNull(contentType);
        if (contentTypeValue != null) {
            if (isOneOfStrategy) {
                final MediaType mediaType = MediaType.parse(contentTypeValue);
                for (final MediaType validType : validTypes) {
                    if (mediaType.is(validType)) {
                        return true;
                    }
                }
                return false;
            } else {
                final MediaType mediaType = MediaType.parse(contentTypeValue).withoutParameters();
                final Set<MediaType> validTypesWithoutParameters = new HashSet<>();
                validTypesWithoutParameters.addAll(Collections2.filter(
                        Collections2.transform(validTypes, STRIP_PARAMS), 
                        Predicates.notNull()));
                return validTypesWithoutParameters.contains(mediaType);
            }
        } else {
            return noContentTypeIsValid;
        }
        
    }

}

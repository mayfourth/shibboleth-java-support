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

package net.shibboleth.utilities.java.support.resolver;

import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.collection.ClassIndexedSet;

/** This class holds instances of {@link Criterion} which are used in resolution or evaluation operations. */
public class CriteriaSet extends ClassIndexedSet<Criterion> implements Criterion {

    /** Constructor. */
    public CriteriaSet() {
        super();
    }

    /**
     * A convenience constructor for constructing and adding criteria.
     * 
     * @param criteria criteria to add, may be null or contain null values
     */
    public CriteriaSet(@Nullable final Criterion... criteria) {
        super();

        if (criteria == null || criteria.length == 0) {
            return;
        }

        for (final Criterion criterion : criteria) {
            if (criterion == null) {
                continue;
            }
            add(criterion);
        }
    }
}
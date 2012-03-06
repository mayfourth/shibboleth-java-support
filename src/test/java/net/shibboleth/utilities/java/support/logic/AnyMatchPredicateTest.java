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

import java.util.Collections;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

/** {@link AnyMatchPredicate} unit test. */
public class AnyMatchPredicateTest {

    @Test public void testApply(){
        AnyMatchPredicate<String> predicate = new AnyMatchPredicate<String>(Predicates.equalTo("foo"));
        
        if (predicate.apply(null)) {
            Assert.fail();
        }

        if (predicate.apply(Collections.EMPTY_LIST)) {
            Assert.fail();
        }

        if (!predicate.apply(Lists.newArrayList("foo"))) {
            Assert.fail();
        }

        if (!predicate.apply(Lists.newArrayList("foo", "foo"))) {
            Assert.fail();
        }

        if (!predicate.apply(Lists.newArrayList("foo", "bar", "foo"))) {
            Assert.fail();
        }
        
        if (predicate.apply(Lists.newArrayList("bar", "baz"))) {
            Assert.fail();
        }
    }
}
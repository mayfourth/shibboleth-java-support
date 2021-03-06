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

package net.shibboleth.utilities.java.support.component;

/**
 * A {@link Component} which needs to be initialized prior to any "real" use. Real use usually means, for example,
 * calling business logic but does not usually cover getting/setting properties.
 */
public interface InitializableComponent extends Component {

    /**
     * Gets whether this component is initialized.
     * 
     * @return true iff this component is initialized
     */
    public boolean isInitialized();
    
    /**
     * Initializes the component.
     * 
     * @throws ComponentInitializationException thrown if there is a problem initializing the component
     */
    public void initialize() throws ComponentInitializationException;
}
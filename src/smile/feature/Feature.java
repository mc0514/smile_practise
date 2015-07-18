/*******************************************************************************
 * Copyright (c) 2010 Haifeng Li
 *   
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package smile.feature;

import com.smile.data.Attribute;

/**
 * Feature generator.
 * 
 * @param <T> the type of input object.
 * 
 * @author Haifeng Li
 */
public interface Feature <T> {
    /**
     * Returns the variable attributes of generated features.
     * Note that these are NOT the original variable attributes of objects.
     * @return the variable attributes of generated features
     */
    public Attribute[] attributes();
    
    /**
     * Generates a feature for given object.
     * @param object an object of interest.
     * @param id the index of feature to be generated.
     * @return a feature value.
     */
    public double f(T object, int id);
}

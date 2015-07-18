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
 * Sequence feature generator.
 * 
 * @param <T> the type of input object.
 * 
 * @author Haifeng Li
 */
public interface SequenceFeature <T> {
    /**
     * Returns the variable attributes of generated features.
     * @return the variable attributes of generated features
     */
    public Attribute[] attributes();
    
    /**
     * Generates the feature set of sequence at given index.
     * @param sequence a sequence of interest.
     * @param index the index of feature to be generated.
     * @return a feature value.
     */
    public double[] f(T[] sequence, int index);    
}

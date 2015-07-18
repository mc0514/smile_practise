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
package smile.classification;

import java.util.Arrays;

import com.smile.data.Attribute;
import com.smile.data.NumericAttribute;
import com.smile.math.Math;
import smile.regression.RegressionTree;
import smile.util.SmileUtils;
import smile.validation.Accuracy;
import smile.validation.ClassificationMeasure;

/**
 * Gradient boosting for classification. Gradient boosting is typically used
 * with decision trees (especially CART regression trees) of a fixed size as
 * base learners. For this special case Friedman proposes a modification to
 * gradient boosting method which improves the quality of fit of each base
 * learner.
 * <p>
 * Generic gradient boosting at the t-th step would fit a regression tree to
 * pseudo-residuals. Let J be the number of its leaves. The tree partitions
 * the input space into J disjoint regions and predicts a constant value in
 * each region. The parameter J controls the maximum allowed
 * level of interaction between variables in the model. With J = 2 (decision
 * stumps), no interaction between variables is allowed. With J = 3 the model
 * may include effects of the interaction between up to two variables, and
 * so on. Hastie et al. comment that typically 4 &le; J &le; 8 work well
 * for boosting and results are fairly insensitive to the choice of in
 * this range, J = 2 is insufficient for many applications, and J > 10 is
 * unlikely to be required.
 * <p>
 * Fitting the training set too closely can lead to degradation of the model's
 * generalization ability. Several so-called regularization techniques reduce
 * this over-fitting effect by constraining the fitting procedure.
 * One natural regularization parameter is the number of gradient boosting
 * iterations T (i.e. the number of trees in the model when the base learner
 * is a decision tree). Increasing T reduces the error on training set,
 * but setting it too high may lead to over-fitting. An optimal value of T
 * is often selected by monitoring prediction error on a separate validation
 * data set.
 * <p>
 * Another regularization approach is the shrinkage which times a parameter
 * &eta; (called the "learning rate") to update term.
 * Empirically it has been found that using small learning rates (such as
 * &eta; < 0.1) yields dramatic improvements in model's generalization ability
 * over gradient boosting without shrinking (&eta; = 1). However, it comes at
 * the price of increasing computational time both during training and
 * prediction: lower learning rate requires more iterations.
 * <p>
 * Soon after the introduction of gradient boosting Friedman proposed a
 * minor modification to the algorithm, motivated by Breiman's bagging method.
 * Specifically, he proposed that at each iteration of the algorithm, a base
 * learner should be fit on a subsample of the training set drawn at random
 * without replacement. Friedman observed a substantional improvement in
 * gradient boosting's accuracy with this modification.
 * <p>
 * Subsample size is some constant fraction f of the size of the training set.
 * When f = 1, the algorithm is deterministic and identical to the one
 * described above. Smaller values of f introduce randomness into the
 * algorithm and help prevent over-fitting, acting as a kind of regularization.
 * The algorithm also becomes faster, because regression trees have to be fit
 * to smaller datasets at each iteration. Typically, f is set to 0.5, meaning
 * that one half of the training set is used to build each base learner.
 * <p>
 * Also, like in bagging, sub-sampling allows one to define an out-of-bag
 * estimate of the prediction performance improvement by evaluating predictions
 * on those observations which were not used in the building of the next
 * base learner. Out-of-bag estimates help avoid the need for an independent
 * validation dataset, but often underestimate actual performance improvement
 * and the optimal number of iterations.
 * <p>
 * Gradient tree boosting implementations often also use regularization by
 * limiting the minimum number of observations in trees' terminal nodes.
 * It's used in the tree building process by ignoring any splits that lead
 * to nodes containing fewer than this number of training set instances.
 * Imposing this limit helps to reduce variance in predictions at leaves.
 * 
 * <h2>References</h2>
 * <ol>
 * <li> J. H. Friedman. Greedy Function Approximation: A Gradient Boosting Machine, 1999.</li>
 * <li> J. H. Friedman. Stochastic Gradient Boosting, 1999.</li>
 * </ol>
 * 
 * @author Haifeng Li
 */
public class GradientTreeBoost implements Classifier<double[]> {
    /**
     * The number of classes.
     */
    private int k = 2;
    /**
     * Forest of regression trees for binary classification.
     */
    private RegressionTree[] trees;
    /**
     * Forest of regression trees for multi-class classification.
     */
    private RegressionTree[][] forest;
    /**
     * Variable importance. Every time a split of a node is made on variable
     * the impurity criterion for the two descendent nodes is less than the
     * parent node. Adding up the decreases for each individual variable over
     * all trees in the forest gives a simple variable importance.
     */
    private double[] importance;
    /**
     * The intercept.
     */
    private double b = 0.0;
    /**
     * The shrinkage parameter in (0, 1] controls the learning rate of procedure.
     */
    private double shrinkage = 0.005;
    /**
     * The number of leaves in each tree.
     */
    private int J = 6;
    /**
     * The number of trees.
     */
    private int T = 500;
    /**
     * The sampling rate for stochastic tree boosting.
     */
    private double f = 0.7;
    
    /**
     * Trainer for GradientTreeBoost classifiers.
     */
    public static class Trainer extends ClassifierTrainer<double[]> {
        /**
         * The number of trees.
         */
        private int T = 500;
        /**
         * The shrinkage parameter in (0, 1] controls the learning rate of procedure.
         */
        private double shrinkage = 0.005;
        /**
         * The number of leaves in each tree.
         */
        private int J = 6;
        /**
         * The sampling rate for stochastic tree boosting.
         */
        private double f = 0.7;
        
        /**
         * Constructor.
         * 
         * @param T the number of trees.
         */
        public Trainer(int T) {
            if (T < 1) {
                throw new IllegalArgumentException("Invlaid number of trees: " + T);
            }

            this.T = T;
        }

        /**
         * Constructor.
         * 
         * @param attributes the attributes of independent variable.
         * @param T the number of trees.
         */
        public Trainer(Attribute[] attributes, int T) {
            super(attributes);

            if (T < 1) {
                throw new IllegalArgumentException("Invlaid number of trees: " + T);
            }

            this.T = T;
        }
        
        /**
         * Sets the number of trees in the random forest.
         * @param T the number of trees.
         */
        public void setNumTrees(int T) {
            if (T < 1) {
                throw new IllegalArgumentException("Invlaid number of trees: " + T);
            }

            this.T = T;
        }
        
        /**
         * Sets the maximum number of leaf nodes in the tree.
         * @param J the maximum number of leaf nodes in the tree.
         */
        public void setMaximumLeafNodes(int J) {
            if (J < 2) {
                throw new IllegalArgumentException("Invalid number of leaf nodes: " + J);
            }
            
            this.J = J;
        }
        
        /**
         * Sets the shrinkage parameter in (0, 1] controls the learning rate of procedure.
         * @param shrinkage the learning rate.
         */
        public void setShrinkage(double shrinkage) {
            if (shrinkage <= 0 || shrinkage > 1) {
                throw new IllegalArgumentException("Invalid shrinkage: " + shrinkage);
            }

            this.shrinkage = shrinkage;
        }

        /**
         * Sets the sampling rate for stochastic tree boosting.
         * @param f the sampling rate for stochastic tree boosting.
         */
        public void setSamplingRates(double f) {
            if (f <= 0 || f > 1) {
                throw new IllegalArgumentException("Invalid sampling fraction: " + f);
            }

            this.f = f;
        }
        
        @Override
        public GradientTreeBoost train(double[][] x, int[] y) {
            return new GradientTreeBoost(attributes, x, y, T, J, shrinkage, f);
        }
    }
    
    /**
     * Constructor. Learns a gradient tree boosting for classification.
     * @param x the training instances. 
     * @param y the class labels.
     * @param T the number of iterations (trees).
     */
    public GradientTreeBoost(double[][] x, int[] y, int T) {
        this(null, x, y, T);
    }
    
    /**
     * Constructor. Learns a gradient tree boosting for classification.
     *
     * @param x the training instances. 
     * @param y the class labels.
     * @param T the number of iterations (trees).
     * @param J the number of leaves in each tree.
     * @param shrinkage the shrinkage parameter in (0, 1] controls the learning rate of procedure.
     * @param f the sampling rate for stochastic tree boosting.
     */
    public GradientTreeBoost(double[][] x, int[] y, int T, int J, double shrinkage, double f) {
        this(null, x, y, T, J, shrinkage, f);
    }

    /**
     * Constructor. Learns a gradient tree boosting for classification.
     * 
     * @param attributes the attribute properties.
     * @param x the training instances. 
     * @param y the class labels.
     * @param T the number of iterations (trees).
     */
    public GradientTreeBoost(Attribute[] attributes, double[][] x, int[] y, int T) {
        this(attributes, x, y, T, 6, x.length < 2000 ? 0.005 : 0.05, 0.7);
    }
    
    /**
     * Constructor. Learns a gradient tree boosting for classification.
     *
     * @param attributes the attribute properties.
     * @param x the training instances. 
     * @param y the class labels.
     * @param T the number of iterations (trees).
     * @param J the number of leaves in each tree.
     * @param shrinkage the shrinkage parameter in (0, 1] controls the learning rate of procedure.
     * @param f the sampling fraction for stochastic tree boosting.
     */
    public GradientTreeBoost(Attribute[] attributes, double[][] x, int[] y, int T, int J, double shrinkage, double f) {
        if (x.length != y.length) {
            throw new IllegalArgumentException(String.format("The sizes of X and Y don't match: %d != %d", x.length, y.length));
        }
        
        if (shrinkage <= 0 || shrinkage > 1) {
            throw new IllegalArgumentException("Invalid shrinkage: " + shrinkage);            
        }

        if (f <= 0 || f > 1) {
            throw new IllegalArgumentException("Invalid sampling fraction: " + f);            
        }
        
        if (attributes == null) {
            int p = x[0].length;
            attributes = new Attribute[p];
            for (int i = 0; i < p; i++) {
                attributes[i] = new NumericAttribute("V" + (i + 1));
            }
        }
        
        this.T = T;
        this.J = J;
        this.shrinkage = shrinkage;
        this.f = f;
        this.k = Math.max(y) + 1;

        if (k < 2) {
            throw new IllegalArgumentException("Only one class or negative class labels.");
        }
        
        importance = new double[attributes.length];
        if (k == 2) {
            train2(attributes, x, y);
            for (RegressionTree tree : trees) {
                double[] imp = tree.importance();
                for (int i = 0; i < imp.length; i++) {
                    importance[i] += imp[i];
                }
            }
        } else {
            traink(attributes, x, y);
            for (RegressionTree[] grove : forest) {
                for (RegressionTree tree : grove) {
                    double[] imp = tree.importance();
                    for (int i = 0; i < imp.length; i++) {
                        importance[i] += imp[i];
                    }
                }
            }
        }
    }

    /**
     * Returns the variable importance. Every time a split of a node is made
     * on variable the impurity criterion for the two descendent nodes is less
     * than the parent node. Adding up the decreases for each individual
     * variable over all trees in the forest gives a simple measure of variable
     * importance.
     *
     * @return the variable importance
     */
    public double[] importance() {
        return importance;
    }
    
    /**
     * Train L2 tree boost.
     */
    private void train2(Attribute[] attributes, double[][] x, int[] y) {        
        int n = x.length;
        int N = (int) Math.round(n * f);

        int[] y2 = new int[n];
        for (int i = 0; i < n; i++) {
            if (y[i] == 1) {
                y2[i] = 1;
            } else {
                y2[i] = -1;
            }
        }

        y = y2;
        
        double[] h = new double[n]; // current F(x_i)
        double[] response = new double[n]; // response variable for regression tree.

        double mu = Math.mean(y);
        b = 0.5 * Math.log((1 + mu) / (1 - mu));

        for (int i = 0; i < n; i++) {
            h[i] = b;
        }

        int[][] order = SmileUtils.sort(attributes, x);
        RegressionTree.NodeOutput output = new L2NodeOutput(response);
        trees = new RegressionTree[T];

        int[] perm = new int[n];
        int[] samples = new int[n];
        for (int i = 0; i < n; i++) {
            perm[i] = i;
        }

        for (int m = 0; m < T; m++) {
            Arrays.fill(samples, 0);

            Math.permutate(perm);
            for (int i = 0; i < N; i++) {
                samples[perm[i]] = 1;
            }

            for (int i = 0; i < n; i++) {
                response[i] = 2.0 * y[i] / (1 + Math.exp(2 * y[i] * h[i]));
            }

            trees[m] = new RegressionTree(attributes, x, response, J, order, samples, output);

            for (int i = 0; i < n; i++) {
                h[i] += shrinkage * trees[m].predict(x[i]);
            }
        }
    }

    /**
     * Train L-k tree boost.
     */
    private void traink(Attribute[] attributes, double[][] x, int[] y) {        
        int n = x.length;
        int N = (int) Math.round(n * f);
        
        double[][] h = new double[k][n]; // boost tree output.
        double[][] p = new double[k][n]; // posteriori probabilities.
        double[][] response = new double[k][n]; // pseudo response.
        
        int[][] order = SmileUtils.sort(attributes, x);        
        forest = new RegressionTree[k][T];

        RegressionTree.NodeOutput[] output = new LKNodeOutput[k];
        for (int i = 0; i < k; i++) {
            output[i] = new LKNodeOutput(response[i]);
        }

        int[] perm = new int[n];
        int[] samples = new int[n];
        for (int i = 0; i < n; i++) {
            perm[i] = i;
        }        
        
        for (int m = 0; m < T; m++) {
            for (int i = 0; i < n; i++) {
                double max = Double.NEGATIVE_INFINITY;
                for (int j = 0; j < k; j++) {
                    if (max < h[j][i]) {
                        max = h[j][i];
                    }
                }
                
                double Z = 0.0;
                for (int j = 0; j < k; j++) {
                    p[j][i] = Math.exp(h[j][i] - max);
                    Z += p[j][i];
                }

                for (int j = 0; j < k; j++) {
                    p[j][i] /= Z;
                }
            }
            
            for (int j = 0; j < k; j++) {
                for (int i = 0; i < n; i++) {
                    if (y[i] == j) {
                        response[j][i] = 1.0;
                    } else {
                        response[j][i] = 0.0;
                    }
                    response[j][i] -= p[j][i];
                }

                Arrays.fill(samples, 0);
                Math.permutate(perm);
                for (int i = 0; i < N; i++) {
                    samples[perm[i]] = 1;
                }

                forest[j][m] = new RegressionTree(attributes, x, response[j], J, order, samples, output[j]);

                for (int i = 0; i < n; i++) {
                    h[j][i] += shrinkage * forest[j][m].predict(x[i]);
                }
            }
        }
    }

    /**
     * Class to calculate node output for two-class logistic regression.
     */
    class L2NodeOutput implements RegressionTree.NodeOutput {

        /**
         * Pseudo response to fit.
         */
        double[] y;
        /**
         * Constructor.
         * @param y pseudo response to fit.
         */
        public L2NodeOutput(double[] y) {
            this.y = y;
        }
        
        @Override
        public double calculate(int[] samples) {
            double nu = 0.0;
            double de = 0.0;
            for (int i = 0; i < samples.length; i++) {
                if (samples[i] > 0) {
                    double abs = Math.abs(y[i]);
                    nu += y[i];
                    de += abs * (2.0 - abs);
                }
            }
            
            return nu / de;
        }        
    }
    
    
    /**
     * Class to calculate node output for multi-class logistic regression.
     */
    class LKNodeOutput implements RegressionTree.NodeOutput {

        /**
         * Responses to fit.
         */
        double[] y;
        /**
         * Constructor.
         * @param response response to fit.
         */
        public LKNodeOutput(double[] response) {
            this.y = response;
        }
        
        @Override
        public double calculate(int[] samples) {
            int n = 0;
            double nu = 0.0;
            double de = 0.0;
            for (int i = 0; i < samples.length; i++) {
                if (samples[i] > 0) {
                    n++;
                    double abs = Math.abs(y[i]);
                    nu += y[i];
                    de += abs * (1.0 - abs);
                }
            }
            
            if (de < 1E-10) {
                return nu / n;
            }
            
            return ((k-1.0) / k) * (nu / de);
        }        
    }
    
    /**
     * Returns the number of trees in the model.
     * 
     * @return the number of trees in the model 
     */
    public int size() {
        return trees.length;
    }
    
    /**
     * Trims the tree model set to a smaller size in case of over-fitting.
     * Or if extra decision trees in the model don't improve the performance,
     * we may remove them to reduce the model size and also improve the speed of
     * prediction.
     * 
     * @param T the new (smaller) size of tree model set.
     */
    public void trim(int T) {
        if (k == 2) {
            if (T > trees.length) {
                throw new IllegalArgumentException("The new model size is larger than the current size.");
            }

            if (T <= 0) {
                throw new IllegalArgumentException("Invalid new model size: " + T);
            }

            if (T < trees.length) {
                trees = Arrays.copyOf(trees, T);
            }
        } else {
            if (T > forest[0].length) {
                throw new IllegalArgumentException("The new model size is larger than the current one.");
            }

            if (T <= 0) {
                throw new IllegalArgumentException("Invalid new model size: " + T);
            }

            if (T < forest[0].length) {
                for (int i = 0; i < forest.length; i++) {
                    forest[i] = Arrays.copyOf(forest[i], T);
                }
            }
        }
    }
    
    @Override
    public int predict(double[] x) {
        if (k == 2) {
            double y = b;
            for (int i = 0; i < T; i++) {
                y += shrinkage * trees[i].predict(x);
            }
            
            return y > 0 ? 1 : 0;
        } else {
            double max = Double.NEGATIVE_INFINITY;
            int y = -1;
            for (int j = 0; j < k; j++) {
                double yj = 0.0;
                for (int i = 0; i < T; i++) {
                    yj += shrinkage * forest[j][i].predict(x);
                }
                
                if (yj > max) {
                    max = yj;
                    y = j;
                }
            }

            return y;            
        }
    }
    
    @Override
    public int predict(double[] x, double[] posteriori) {
        if (posteriori.length != k) {
            throw new IllegalArgumentException(String.format("Invalid posteriori vector size: %d, expected: %d", posteriori.length, k));
        }

        if (k == 2) {
            double y = b;
            for (int i = 0; i < T; i++) {
                y += shrinkage * trees[i].predict(x);
            }

            posteriori[0] = 1.0 / (1.0 + Math.exp(2*y));
            posteriori[1] = 1.0 - posteriori[0];

            if (y > 0) {
                return 1;
            } else {
                return 0;
            }
        } else {
            double max = Double.NEGATIVE_INFINITY;
            int y = -1;
            for (int j = 0; j < k; j++) {
                posteriori[j] = 0.0;
                
                for (int i = 0; i < T; i++) {
                    posteriori[j] += shrinkage * forest[j][i].predict(x);
                }
                
                if (posteriori[j] > max) {
                    max = posteriori[j];
                    y = j;
                }
            }

            double Z = 0.0;
            for (int i = 0; i < k; i++) {
                posteriori[i] = Math.exp(posteriori[i] - max);
                Z += posteriori[i];
            }

            for (int i = 0; i < k; i++) {
                posteriori[i] /= Z;
            }
            
            return y;            
        }
    }
    
    /**
     * Test the model on a validation dataset.
     * 
     * @param x the test data set.
     * @param y the test data response values.
     * @return accuracies with first 1, 2, ..., decision trees.
     */
    public double[] test(double[][] x, int[] y) {
        double[] accuracy = new double[T];

        int n = x.length;
        int[] label = new int[n];

        Accuracy measure = new Accuracy();
        
        if (k == 2) {
            double[] prediction = new double[n];
            Arrays.fill(prediction, b);
            for (int i = 0; i < T; i++) {
                for (int j = 0; j < n; j++) {
                    prediction[j] += shrinkage * trees[i].predict(x[j]);
                    label[j] = prediction[j] > 0 ? 1 : 0;
                }
                accuracy[i] = measure.measure(y, label);
            }
        } else {
            double[][] prediction = new double[n][k];
            for (int i = 0; i < T; i++) {
                for (int j = 0; j < n; j++) {
                    for (int l = 0; l < k; l++) {
                        prediction[j][l] += shrinkage * forest[l][i].predict(x[j]);
                    }
                    label[j] = Math.whichMax(prediction[j]);
                }

                accuracy[i] = measure.measure(y, label);
            }
        }
        
        return accuracy;
    }
    
    /**
     * Test the model on a validation dataset.
     * 
     * @param x the test data set.
     * @param y the test data labels.
     * @param measures the performance measures of classification.
     * @return performance measures with first 1, 2, ..., decision trees.
     */
    public double[][] test(double[][] x, int[] y, ClassificationMeasure[] measures) {
        int m = measures.length;
        double[][] results = new double[T][m];

        int n = x.length;
        int[] label = new int[n];

        if (k == 2) {
            double[] prediction = new double[n];
            Arrays.fill(prediction, b);
            for (int i = 0; i < T; i++) {
                for (int j = 0; j < n; j++) {
                    prediction[j] += shrinkage * trees[i].predict(x[j]);
                    label[j] = prediction[j] > 0 ? 1 : 0;
                }

                for (int j = 0; j < m; j++) {
                    results[i][j] = measures[j].measure(y, label);
                }
            }
        } else {
            double[][] prediction = new double[n][k];
            for (int i = 0; i < T; i++) {
                for (int j = 0; j < n; j++) {
                    for (int l = 0; l < k; l++) {
                        prediction[j][l] += shrinkage * forest[l][i].predict(x[j]);
                    }
                    label[j] = Math.whichMax(prediction[j]);
                }

                for (int j = 0; j < m; j++) {
                    results[i][j] = measures[j].measure(y, label);
                }
            }

        }
        
        return results;
    }
}

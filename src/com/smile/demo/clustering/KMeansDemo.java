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

package com.smile.demo.clustering;

import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JFrame;

import smile.clustering.KMeans;
import com.smile.plot.Palette;
import com.smile.plot.PlotCanvas;
import com.smile.plot.ScatterPlot;

/**
 *
 * @author Haifeng Li
 */
@SuppressWarnings("serial")
public class KMeansDemo extends ClusteringDemo {
    public KMeansDemo() {
    }

    @Override
    public JComponent learn() {
        long clock = System.currentTimeMillis();
        KMeans kmeans = new KMeans(dataset[datasetIndex], clusterNumber, 100, 4);
        System.out.format("K-Means clusterings %d samples in %dms\n", dataset[datasetIndex].length, System.currentTimeMillis()-clock);

        PlotCanvas plot = ScatterPlot.plot(dataset[datasetIndex], kmeans.getClusterLabel(), pointLegend, Palette.COLORS);
        plot.points(kmeans.centroids(), '@');
        return plot;
    }

    @Override
    public String toString() {
        return "K-Means";
    }

    public static void main(String argv[]) {
        ClusteringDemo demo = new KMeansDemo();
        JFrame f = new JFrame("K-Means");
        f.setSize(new Dimension(1000, 1000));
        f.setLocationRelativeTo( null );
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.getContentPane().add(demo);
        f.setVisible(true);
    }
}

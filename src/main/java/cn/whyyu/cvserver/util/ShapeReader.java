package cn.whyyu.cvserver.util;


import cn.whyyu.cvserver.path.structure.TopologyGraph;
import cn.whyyu.cvserver.path.structure.Vertex;
import com.google.common.geometry.S2LatLng;
import com.google.common.geometry.S2Point;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 读取shapefile文件的工具类，负责将数据转化为算法需要的数据结构
 */
public class ShapeReader {
    /**
     * 读取摄像头点数据
     * @param file shp文件
     * @return 摄像头点索引
     */
    public static PointIndex readPoint(File file) {
        Map<String, Object> map = new HashMap<>();
        PointIndex pointIndex = new PointIndex();
        try {
            map.put("url", file.toURI().toURL());
            DataStore dataStore = DataStoreFinder.getDataStore(map);
            //字符转码，防止中文乱码
            ((ShapefileDataStore) dataStore).setCharset(StandardCharsets.UTF_8);
            String typeName = dataStore.getTypeNames()[0];

            FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);
            FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures();
            FeatureIterator<SimpleFeature> features = collection.features();
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                Point point = (Point) feature.getDefaultGeometry();
                pointIndex.add(point);
            }
            features.close();
            dataStore.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pointIndex;
    }

    /**
     * 读取拓扑路网数据，并将数据构建为TopologyGraph
     * @param file 拓扑路网shp文件
     * @return 拓扑节点转化为S2Point的Set
     */
    public static Set<Vertex> readLineString(File file) {
        Map<String, Object> map = new HashMap<>();
        Set<Vertex> vertexSet = new HashSet<>();
        Map<S2Point, String> dataIndexMap = new HashMap<>();
        try {
            map.put("url", file.toURI().toURL());
            DataStore dataStore = DataStoreFinder.getDataStore(map);
            //字符转码，防止中文乱码
            ((ShapefileDataStore) dataStore).setCharset(StandardCharsets.UTF_8);
            String typeName = dataStore.getTypeNames()[0];

            FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);
            FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures();
            FeatureIterator<SimpleFeature> features = collection.features();
            int id = 0;
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                MultiLineString lineString = (MultiLineString) feature.getDefaultGeometry();
                Coordinate[] coordinates = lineString.getCoordinates();
                S2LatLng start = S2LatLng.fromDegrees(coordinates[0].getY(),
                        coordinates[0].getX());
                S2Point startPoint = start.toPoint();
                // 嵌入拓扑结构，计算最短路径(注意重复添加判别依据是dataIndex)
                Vertex startVertex = new Vertex(String.valueOf(vertexSet.size()), startPoint);
                if (!vertexSet.contains(startVertex)) {
                    dataIndexMap.put(startVertex, String.valueOf(vertexSet.size()));
                    vertexSet.add(startVertex);
                    TopologyGraph.insertVertex(startVertex);
                }
                S2LatLng end = S2LatLng.fromDegrees(coordinates[1].getY(),
                        coordinates[1].getX());
                S2Point endPoint = end.toPoint();
                Vertex endVertex = new Vertex(String.valueOf(vertexSet.size()), endPoint);
                if(!vertexSet.contains(endVertex)) {
                    dataIndexMap.put(endVertex, String.valueOf(vertexSet.size()));
                    vertexSet.add(endVertex);
                    TopologyGraph.insertVertex(endVertex);
                }
                double distance = start.toPoint().getDistance(end.toPoint());
                String startIndex = dataIndexMap.get(startVertex);
                String endIndex = dataIndexMap.get(endVertex);
                TopologyGraph.insertEdge(startIndex, endIndex, distance);
            }
            features.close();
            dataStore.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vertexSet;
    }

    public static void main(String[] args) {
        String path = "C:/Users/YH/Desktop/unicomRailwayStation/pathway/pathway.shp";
        Set<Vertex> nodeSet = ShapeReader.readLineString(new File(path));
        System.out.println(nodeSet);

        // Set的equals条件只检验坐标是否相等，而不管dataIndex
//        Set<Vertex> vertexSet = new HashSet<>();
//        S2Point start = S2LatLng.fromDegrees(120.0, 30.0).toPoint();
//        S2Point end = S2LatLng.fromDegrees(120.0, 30.0).toPoint();
//        Vertex vertex1 = new Vertex("1", start);
//        Vertex vertex2 = new Vertex("2", end);
//        vertexSet.add(vertex1);
//        vertexSet.add(vertex2);
//        System.out.println(vertexSet.size());
//        System.out.println(vertexSet.contains(vertex2));

    }
}

package cn.whyyu.cvserver.util;


import cn.whyyu.cvserver.path.structure.TopologyGraph;
import cn.whyyu.cvserver.path.structure.Vertex;
import com.google.common.geometry.S2ClosestPointQuery;
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
import org.locationtech.proj4j.ProjCoordinate;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 读取shapefile文件的工具类，负责将数据转化为算法需要的数据结构
 */
public class ShapeReader {
//    /**
//     * 读取摄像头点数据
//     * @param file shp文件
//     * @return 摄像头点索引
//     */
//    @Deprecated
//    public static PointIndex readPoint(File file) {
//        Map<String, Object> map = new HashMap<>();
//        PointIndex pointIndex = new PointIndex();
//        try {
//            map.put("url", file.toURI().toURL());
//            DataStore dataStore = DataStoreFinder.getDataStore(map);
//            //字符转码，防止中文乱码
//            ((ShapefileDataStore) dataStore).setCharset(StandardCharsets.UTF_8);
//            String typeName = dataStore.getTypeNames()[0];
//
//            FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);
//            FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures();
//            FeatureIterator<SimpleFeature> features = collection.features();
//            while (features.hasNext()) {
//                SimpleFeature feature = features.next();
//                Point point = (Point) feature.getDefaultGeometry();
//                pointIndex.add(point);
//            }
//            features.close();
//            dataStore.dispose();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return pointIndex;
//    }

    /**
     * 读取拓扑路网数据，并将数据构建为TopologyGraph
     * @param file 拓扑路网shp文件
     * @return 拓扑节点转化为S2Point的Set
     */
    public static Set<Vertex> readLineString(File file) {
        // 存储文件列表
        Map<String, Object> map = new HashMap<>();
        // 拓扑路网中的点集合(可用于避免添加重复点，也便于后续生成点索引查询最近点)
        Set<Vertex> vertexSet = new HashSet<>();
        // 点到dataIndex的映射，主要是在插入边时，若遇到重复点能快速找到对应的dataIndex(仅根据x,y作为key)
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
                // 数据中可能会出现部分值为NaN，必须略过这部分错值
                if (Double.isNaN(coordinates[0].getY()) || Double.isNaN(coordinates[0].getX())
                || Double.isNaN(coordinates[1].getY()) || Double.isNaN(coordinates[1].getX())) {
                    continue;
                }

                ProjCoordinate wgs84CoordinateStart = CoordinateTransformer.geoToWgs(
                        coordinates[0].getX(), coordinates[0].getY());
                S2LatLng start = S2LatLng.fromDegrees(wgs84CoordinateStart.y,
                        wgs84CoordinateStart.x);
                S2Point startPoint = start.toPoint();
                // Set的equals条件只检验坐标是否相等，而不管dataIndex，所以先赋值一个默认dataIndex,dataIndexMap同理
                Vertex startVertex = new Vertex("0", startPoint);
                if (!vertexSet.contains(startVertex)) {
                    String dataIndex = String.valueOf(id);
                    startVertex.dataIndex = dataIndex;
                    dataIndexMap.put(startVertex, dataIndex);
                    vertexSet.add(startVertex);
                    TopologyGraph.insertVertex(startVertex);
                    id++;
                }

                ProjCoordinate wgs84CoordinateEnd = CoordinateTransformer.geoToWgs(
                        coordinates[1].getX(), coordinates[1].getY());
                S2LatLng end = S2LatLng.fromDegrees(wgs84CoordinateEnd.y,
                        wgs84CoordinateEnd.x);
                S2Point endPoint = end.toPoint();
                Vertex endVertex = new Vertex("0", endPoint);
                if(!vertexSet.contains(endVertex)) {
                    String dataIndex = String.valueOf(id);
                    endVertex.dataIndex = dataIndex;
                    dataIndexMap.put(endVertex, dataIndex);
                    vertexSet.add(endVertex);
                    TopologyGraph.insertVertex(endVertex);
                    id++;
                }

                double distance = startPoint.getDistance(endPoint);
                String startIndex = dataIndexMap.get(startVertex);
                String endIndex = dataIndexMap.get(endVertex);
                TopologyGraph.insertEdge(startIndex, endIndex, distance);
            }
            features.close();
            dataStore.dispose();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("缺少拓扑路径数据");
        }
        return vertexSet;
    }

    public static void main(String[] args) {
        String path = "C:\\Users\\YH\\Desktop\\unicomRailwayStation\\newShapeFile\\shapeFile\\square\\road.shp";
        Set<Vertex> nodeSet = ShapeReader.readLineString(new File(path));
        PointIndex<String> nodeIndex = new PointIndex<>();
        for (Vertex vertex : nodeSet) {
            nodeIndex.add(vertex, vertex.dataIndex);
        }
        S2ClosestPointQuery.Result<String> closestStartNode = nodeIndex.findClosestPoint(S2LatLng.fromDegrees(30.618258, 114.249633).toPoint());
        System.out.println(nodeSet.size());

        // Set和Map的equals条件只检验坐标是否相等，而不管dataIndex
//        Set<Vertex> vertexSet = new HashSet<>();
//        S2Point start = S2LatLng.fromDegrees(120.0, 30.0).toPoint();
//        S2Point end = S2LatLng.fromDegrees(120.0, 30.0).toPoint();
//        Vertex vertex1 = new Vertex("1", start);
//        Vertex vertex2 = new Vertex("2", end);
//        vertexSet.add(vertex1);
//        vertexSet.add(vertex2);
//        System.out.println(vertexSet.size());
//        System.out.println(vertexSet.contains(vertex2));
//
//        Map<S2Point, String> dataIndexMap = new HashMap<>();
//        dataIndexMap.put(vertex1, "1");
//        dataIndexMap.put(vertex2, "2");
//        System.out.println(dataIndexMap.size());
//        System.out.println(dataIndexMap.get(vertex1));
//        System.out.println(dataIndexMap.get(vertex2));
    }
}

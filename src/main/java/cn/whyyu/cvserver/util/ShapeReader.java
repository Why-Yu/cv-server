package cn.whyyu.cvserver.util;


import com.google.common.geometry.S2LatLng;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ShapeReader {
    public static void read(File file) {
        Map<String, Object> map = new HashMap<>();
        try {
            map.put("url", file.toURI().toURL());

            DataStore dataStore = DataStoreFinder.getDataStore(map);
            //字符转码，防止中文乱码
            ((ShapefileDataStore) dataStore).setCharset(StandardCharsets.UTF_8);
            String typeName = dataStore.getTypeNames()[0];
            System.out.println(typeName);
            FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);
            FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures();
            FeatureIterator<SimpleFeature> features = collection.features();
            PointIndex pointIndex = new PointIndex();
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
    }

    public static void main(String[] args) {
        String path = "F:\\OSM数据处理\\outputShapefile\\macau\\macau_point.shp";
        ShapeReader.read(new File(path));
    }
}

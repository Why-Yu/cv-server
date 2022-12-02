package cn.whyyu.cvserver.util;

import cn.whyyu.cvserver.path.structure.TopologyGraph;
import cn.whyyu.cvserver.path.structure.Vertex;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.geometry.S2LatLng;

import java.util.List;

/**
 * 负责将最短路径的结果序列转化为GeoJson格式
 * 利用jackson完成
 */
public class GeoJsonTransformer {
    /**
     * 负责将最短路径的结果序列转化为GeoJson格式
     * @param result 最短路径计算器得出的结果
     * @return GeoJson格式的结果
     */
    public String result2GeoJson(List<String> result) {
        ObjectMapper mapper = new ObjectMapper();
        // 生成根节点
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("type", "FeatureCollection");

        // 生成Features数组
        ArrayNode features = mapper.createArrayNode();
        // 每一个点创建一个feature
        for (String dataIndex : result) {
            ObjectNode feature = mapper.createObjectNode();
            feature.put("type", "Feature");

            // 创建Geometry对象
            ObjectNode geometry = mapper.createObjectNode();
            geometry.put("type", "Point");
            ArrayNode coordinates = mapper.createArrayNode();
            Vertex vertex = TopologyGraph.getVertex(dataIndex);
            S2LatLng s2LatLng = new S2LatLng(vertex);
            coordinates.add(s2LatLng.lngDegrees());
            coordinates.add(s2LatLng.latDegrees());
            geometry.set("coordinates", coordinates);

            feature.set("geometry", geometry);
            features.add(feature);
        }
        rootNode.set("features", features);
        return rootNode.toString();
    }

}

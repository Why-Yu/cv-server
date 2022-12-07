package cn.whyyu.cvserver.controller;

import cn.whyyu.cvserver.path.PathCalculator;
import cn.whyyu.cvserver.path.Query;
import cn.whyyu.cvserver.path.structure.Vertex;
import cn.whyyu.cvserver.util.CommonResult;
import cn.whyyu.cvserver.util.GeoJsonTransformer;
import cn.whyyu.cvserver.util.PointIndex;
import cn.whyyu.cvserver.util.ShapeReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.geometry.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/camera")
public class CameraController {
    private Set<Vertex> vertexSet;
    private PointIndex cameraPointIndex;
    private PointIndex nodeIndex;

    /**
     * 第一次执行会读取文件初始化，所以速度可能稍慢
     * @param startLat 起始纬度
     * @param startLng 起始经度
     * @param endLat 终止纬度
     * @param endLng 终止经度
     * @return ObjectNode[]
     * ObjectNode[0]为预测的三个摄像头的点
     * ObjectNode[1] 为当前预测的轨迹
     */
    @RequestMapping("/getCamera")
    public CommonResult<ObjectNode> getCamera(@RequestParam double startLat, @RequestParam double startLng,
                                              @RequestParam double endLat, @RequestParam double endLng) {
        // 没有初始化的话，先执行一次初始化
        if(cameraPointIndex == null) {
            cameraPointIndex = ShapeReader.readPoint(new File(
                    "C:/Users/YH/Desktop/unicomRailwayStation/camera/camera.shp"));
            vertexSet = ShapeReader.readLineString(new File(
                    "C:/Users/YH/Desktop/unicomRailwayStation/pathway/pathway.shp"));
           nodeIndex = new PointIndex();
           for (Vertex vertex : vertexSet) {
               nodeIndex.add(vertex);
           }
        }

        S2Point start = S2LatLng.fromDegrees(startLat, startLng).toPoint();
        S2Point target = S2LatLng.fromDegrees(endLat, endLng).toPoint();
        List<S2ClosestPointQuery.Result<String>> closestCameras = cameraPointIndex.findClosestPoints(
                target, 3);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode objectNodeResult = mapper.createObjectNode();
        objectNodeResult.set("cameraCandidates", GeoJsonTransformer.points2GeoJson(closestCameras));

        S2ClosestPointQuery.Result<String> closestStartNode = nodeIndex.findClosestPoint(start);
        PathCalculator pathCalculator = new PathCalculator();
        ArrayNode pathArray = mapper.createArrayNode();
        for (S2ClosestPointQuery.Result<String> result : closestCameras) {
            S2ClosestPointQuery.Result<String> closestEndNode = nodeIndex.findClosestPoint(result.entry().point);
            Query query = new Query(closestStartNode.entry().data, closestEndNode.entry().data,
                    closestStartNode.entry().point, closestEndNode.entry().point);
            List<String> resultList = pathCalculator.getAstarShortestPath(query);
            ObjectNode objectNode = GeoJsonTransformer.result2GeoJson(resultList);
            pathArray.add(objectNode);
        }
        objectNodeResult.set("cameraPath", pathArray);
        return CommonResult.success(objectNodeResult);
    }
}

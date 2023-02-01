package cn.whyyu.cvserver.controller;

import cn.whyyu.cvserver.entity.Camera;
import cn.whyyu.cvserver.path.PathCalculator;
import cn.whyyu.cvserver.path.Query;
import cn.whyyu.cvserver.path.structure.Vertex;
import cn.whyyu.cvserver.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.geometry.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/camera")
public class CameraController {
    private PointIndex<Camera> cameraPointIndex;
    private PointIndex<String> nodeIndex;

    /**
     * 第一次执行会读取文件初始化，所以速度可能稍慢
     * @param startLat 起始纬度
     * @param startLng 起始经度
     * @return ObjectNode[]
     * ObjectNode[0]为预测的三个摄像头的点
     * ObjectNode[1] 为当前预测的轨迹
     */
    @RequestMapping("/getCamera")
    public CommonResult<ObjectNode> getCamera(@RequestParam double startLat, @RequestParam double startLng,
                                              @RequestParam(required = false) Integer maxPoints) {
        // 没有初始化的话，先执行一次初始化
        if(cameraPointIndex == null) {
            cameraPointIndex = new PointIndex<>();
            File file = new File("C:\\Users\\YH\\Desktop\\unicomRailwayStation\\camera.xlsx");
            try {
                ArrayList<Camera> excelData = ExcelReader.readExcel(file, Camera.class);
                for (Camera camera : excelData) {
                    if (camera.getLevel() > 0) {
                        cameraPointIndex.add(S2LatLng.fromDegrees(Double.parseDouble(camera.getLat()),
                                Double.parseDouble(camera.getLng())).toPoint(), camera);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Set<Vertex> vertexSet = ShapeReader.readLineString(new File(
                    "/home/shapeFile/pathway.shp"));
           nodeIndex = new PointIndex<>();
           for (Vertex vertex : vertexSet) {
               nodeIndex.add(vertex, vertex.dataIndex);
           }
        }

        S2Point start = S2LatLng.fromDegrees(startLat, startLng).toPoint();
        if (maxPoints == null) {
            maxPoints = 4;
        } else if (maxPoints > 9) {
            maxPoints = 10;
        }
        // 由于设计接口的时候没有考虑到需要一个参数以排除出发的摄像头(已经经过了)
        // 所以简单的多找一个点，并排除最近的那个(显然很多情况下这不一定对)
        List<S2ClosestPointQuery.Result<Camera>> closestCameras = cameraPointIndex.findClosestPoints(
                start, maxPoints);
        closestCameras.remove(0);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode objectNodeResult = mapper.createObjectNode();
        objectNodeResult.set("cameraCandidates", GeoJsonTransformer.points2GeoJson(closestCameras));

        // 找到出发点
        S2ClosestPointQuery.Result<String> closestStartNode = nodeIndex.findClosestPoint(start);
        PathCalculator pathCalculator = new PathCalculator();
        ArrayNode pathArray = mapper.createArrayNode();
        for (S2ClosestPointQuery.Result<Camera> result : closestCameras) {
            // 找到终点
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

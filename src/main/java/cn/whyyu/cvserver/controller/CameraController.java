package cn.whyyu.cvserver.controller;

import cn.whyyu.cvserver.entity.Camera;
import cn.whyyu.cvserver.path.PathCalculator;
import cn.whyyu.cvserver.path.Query;
import cn.whyyu.cvserver.path.structure.TopologyGraph;
import cn.whyyu.cvserver.path.structure.Vertex;
import cn.whyyu.cvserver.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.geometry.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

@RestController
@RequestMapping("/camera")
public class CameraController {
    private PointIndex<Camera> cameraGroundIndex;
    private PointIndex<Camera> cameraUnderIndex;
    private PointIndex<String> nodeIndex;
    private int levelNow = 0;

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
                                              @RequestParam(required = false) Integer level,
                                              @RequestParam(required = false) Integer maxPoints) {
        // 没有初始化的话，先执行一次初始化
        if(cameraGroundIndex == null) {
            cameraGroundIndex = new PointIndex<>();
            cameraUnderIndex = new PointIndex<>();
            // "/home/shapeFile/camera.xlsx"
            // C:/Users/YH/Desktop/unicomRailwayStation/shapeFile/camera.xlsx
            File file = new File("C:/Users/YH/Desktop/unicomRailwayStation/shapeFile/camera.xlsx");
            try {
                // 载入地上下摄像头数据
                ArrayList<Camera> excelData = ExcelReader.readExcel(file, Camera.class);
                for (Camera camera : excelData) {
                    if (camera.getLevel() > 0) {
                        cameraGroundIndex.add(S2LatLng.fromDegrees(Double.parseDouble(camera.getLat()),
                                Double.parseDouble(camera.getLng())).toPoint(), camera);
                    } else {
                        cameraUnderIndex.add(S2LatLng.fromDegrees(Double.parseDouble(camera.getLat()),
                                Double.parseDouble(camera.getLng())).toPoint(), camera);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        // 拓扑数据初始化
        if (level == null) {
            // 默认载入地上拓扑数据(levelNow表示当前的数据结构状态有-1,0,1三种)
            if (levelNow == 0) {
                int result = registerTopo(1);
                if (result == -1) {
                    return CommonResult.failed("缺少拓扑数据文件");
                }
                levelNow = 1;
            }
        } else if (level != 1 && level != -1) {
            return CommonResult.failed("输入的楼层有误，请重新确认");
        } else { // 检查拓扑数据是否需要更改
            if (level != levelNow) {
                if (levelNow == 1) {
                    registerTopo(-1);
                    levelNow = -1;
                } else {
                    registerTopo(1);
                    levelNow = 1;
                }
            }
        }

        S2Point start = S2LatLng.fromDegrees(startLat, startLng).toPoint();
        if (maxPoints == null || maxPoints == 0) {
            maxPoints = 4;
        } else if (maxPoints > 9) {
            maxPoints = 10;
        }
        // 由于设计接口的时候没有考虑到需要一个参数以排除出发的摄像头(已经经过了)
        // 所以简单的多找一个点，并排除最近的那个(显然很多情况下这不一定对)
        PointIndex<Camera> cameraPointIndex;
        if (level == null || level == 1) {
            cameraPointIndex = cameraGroundIndex;
        } else {
            cameraPointIndex = cameraUnderIndex;
        }
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

    @RequestMapping("/getAllCamera")
    public CommonResult<ObjectNode> getAllCamera(@RequestParam int level) {
        // "/home/shapeFile/camera.xlsx"
        // C:/Users/YH/Desktop/unicomRailwayStation/shapeFile/camera.xlsx
        File file = new File("C:/Users/YH/Desktop/unicomRailwayStation/shapeFile/camera.xlsx");
        ArrayList<Camera> excelData = new ArrayList<>();
        try {
            excelData = ExcelReader.readExcel(file, Camera.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ArrayList<Camera> filterCameraList = new ArrayList<>();
        for (Camera camera : excelData) {
            if (camera.getLevel() == level) {
                filterCameraList.add(camera);
            }
        }
        return CommonResult.success(GeoJsonTransformer.cameraList2GeoJson(filterCameraList));
    }

    @RequestMapping("/getFixedCamera")
    public CommonResult<ObjectNode> getFixedCamera(@RequestParam double startLat, @RequestParam double startLng) {
        if (levelNow == 0) {
            // 载入地上拓扑数据
            int result = registerTopo(1);
            if (result == -1) {
                return CommonResult.failed("缺少拓扑数据文件");
            }
            levelNow = 1;
        }
        if (30.617211 < startLat &&  startLat< 30.617985
        && 114.251544 < startLng && startLng < 114.252703) {
            S2Point start = S2LatLng.fromDegrees(startLat, startLng).toPoint();
            S2ClosestPointQuery.Result<String> closestStartNode = nodeIndex.findClosestPoint(start);
            // 选择的固定终止点
            S2ClosestPointQuery.Result<String> closestEndNode = nodeIndex.findClosestPoint(
                    S2LatLng.fromDegrees(30.6177713, 114.2512744).toPoint());
            Query query = new Query(closestStartNode.entry().data, closestEndNode.entry().data,
                    closestStartNode.entry().point, closestEndNode.entry().point);
            PathCalculator pathCalculator = new PathCalculator();
            List<String> resultList = pathCalculator.getAstarShortestPath(query);
            ObjectNode objectNodeResult = GeoJsonTransformer.result2GeoJson(resultList);
            return CommonResult.success(objectNodeResult);
        } else if (30.618016 < startLat &&  startLat< 30.618511
                && 114.250701 < startLng && startLng < 114.251286) {
            S2Point start = S2LatLng.fromDegrees(startLat, startLng).toPoint();
            S2ClosestPointQuery.Result<String> closestStartNode = nodeIndex.findClosestPoint(start);
            // 选择的固定终止点
            S2ClosestPointQuery.Result<String> closestEndNode = nodeIndex.findClosestPoint(
                    S2LatLng.fromDegrees(30.6182061, 114.2503368).toPoint());
            Query query = new Query(closestStartNode.entry().data, closestEndNode.entry().data,
                    closestStartNode.entry().point, closestEndNode.entry().point);
            PathCalculator pathCalculator = new PathCalculator();
            List<String> resultList = pathCalculator.getAstarShortestPath(query);
            ObjectNode objectNodeResult = GeoJsonTransformer.result2GeoJson(resultList);
            return CommonResult.success(objectNodeResult);
        } else {
            return CommonResult.failed("输入的经纬度不在范围内");
        }
    }

    private int registerTopo(int level) {
        Set<Vertex> vertexSet = new HashSet<>();
        if (level == 1) {
            TopologyGraph.reset();
            // "/home/shapeFile/square/road.shp"
            // C:/Users/YH/Desktop/unicomRailwayStation/shapeFile/square/road.shp
            vertexSet = ShapeReader.readLineString(new File(
                    "C:/Users/YH/Desktop/unicomRailwayStation/newShapeFile/shapeFile/square/road.shp"));
        } else if (level == -1){
            TopologyGraph.reset();
            // "/home/shapeFile/parkingLot/road.shp"
            // C:/Users/YH/Desktop/unicomRailwayStation/shapeFile/parkingLot/road.shp
            vertexSet = ShapeReader.readLineString(new File(
                    "C:/Users/YH/Desktop/unicomRailwayStation/newShapeFile/shapeFile/parkingLot/road.shp"));
        }
        if (vertexSet.isEmpty()) {
            return -1;
        } else {
            nodeIndex = new PointIndex<>();
            for (Vertex vertex : vertexSet) {
                nodeIndex.add(vertex, vertex.dataIndex);
            }
            return 1;
        }
    }

}

package cn.whyyu.cvserver.controller;

import cn.whyyu.cvserver.path.structure.TopologyGraph;
import cn.whyyu.cvserver.util.CommonResult;
import cn.whyyu.cvserver.util.PointIndex;
import cn.whyyu.cvserver.util.ShapeReader;
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
    private Set<S2Point> nodes;
    private PointIndex cameraPointIndex;
    private PointIndex nodeIndex;

//    @RequestMapping("/getCamera")
//    public CommonResult<String> getCamera(@RequestParam double lat, @RequestParam double lng) {
//        if(cameraPointIndex == null) {
//            cameraPointIndex = ShapeReader.readPoint(new File(
//                    "C:/Users/YH/Desktop/unicomRailwayStation/camera/camera.shp"));
//           nodes = ShapeReader.readLineString(new File(
//                    "C:/Users/YH/Desktop/unicomRailwayStation/pathway/pathway.shp"));
//           nodeIndex = new PointIndex();
//           for (S2Point node : nodes) {
//               nodeIndex.add(node);
//           }
//        }
//
//        S2Point target = S2LatLng.fromDegrees(lat, lng).toPoint();
//        List<S2ClosestPointQuery.Result<Boolean>> closestCameras = cameraPointIndex.findClosestPoints(
//                target, 3);
//        for (S2ClosestPointQuery.Result<Boolean> result : closestCameras) {
//            List<S2ClosestPointQuery.Result<Boolean>> closestNode = nodeIndex.findClosestPoints(result.entry().point, 1);
//
//        }
//
//        return CommonResult.success("1");
//    }
}

package cn.whyyu.cvserver.util;

import cn.whyyu.cvserver.entity.Camera;
import cn.whyyu.cvserver.path.structure.Vertex;
import com.google.common.geometry.S2ClosestPointQuery;
import com.google.common.geometry.S2LatLng;
import com.google.common.geometry.S2Point;
import com.google.common.geometry.S2PointIndex;
import org.locationtech.jts.geom.Point;

import java.util.List;

/**
 * 对S2PointIndex和S2ClosestPointQuery的再封装
 */
public class PointIndex<E> {
    private final S2PointIndex<E> s2PointIndex  = new S2PointIndex<>();

    public void add(S2Point s2Point, E data) {
        s2PointIndex.add(s2Point, data);
    }

    public void add(Vertex vertex, E data) {
        s2PointIndex.add(vertex, data);
    }

    /**
     * 每次重新创建query可保证S2PointIndex有序，详见源码reset()
     * @param target 当前用户的地理位置
     * @return 距离用户最近的几个点
     */
    public List<S2ClosestPointQuery.Result<E>> findClosestPoints(
            S2Point target, int maxPoints) {
        S2ClosestPointQuery<E> query = new S2ClosestPointQuery<>(s2PointIndex);
        query.setMaxPoints(maxPoints);
        return query.findClosestPoints(target);
    }

    public S2ClosestPointQuery.Result<E> findClosestPoint(S2Point target) {
        S2ClosestPointQuery<E> query = new S2ClosestPointQuery<>(s2PointIndex);
        return query.findClosestPoint(target);
    }

    public int size() {
        return s2PointIndex.numPoints();
    }

}

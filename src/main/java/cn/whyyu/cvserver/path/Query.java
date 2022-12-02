package cn.whyyu.cvserver.path;

import com.google.common.geometry.S2Point;
import lombok.Data;

@Data
public class Query {
    // 选择Sting是因为，可以让ID有各种表示方式，可以让类更具有泛用性，
    // 其实用int也是可以的
    public String sourceID;
    public String targetID;
    // query利用S2Point存储起终点
    public S2Point source;
    public S2Point target;

    public Query() {

    }

    public Query(String sourceID, String targetID, S2Point source, S2Point target) {
        this.sourceID = sourceID;
        this.targetID = targetID;
        this.source = source;
        this.target = target;
    }

    /**
     * 获得与当前query起终点相反的query
     */
    public Query getOppositeQuery() {
        return new Query(targetID, sourceID, target, source);
    }
}

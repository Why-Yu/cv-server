package cn.whyyu.cvserver.path.structure;

import com.google.common.geometry.S2Point;

/**
 * 邻接链表中的首节点抽象
 */
public class Vertex extends S2Point {
    public String dataIndex;
    public NextNode nextNode;

    public Vertex(){}

    // 输入主键以及表示三维向量的三个夹角，得到具体的xyz三维坐标生成S2Point
    public Vertex(String dataIndex, double phi, double theta, double cosphi){
        super(Math.cos(theta) * cosphi, Math.sin(theta) * cosphi, Math.sin(phi));
        this.dataIndex = dataIndex;
    }

    public Vertex(String dataIndex, S2Point s2Point) {
        super(s2Point.x, s2Point.y, s2Point.z);
        this.dataIndex = dataIndex;
    }

    public String getDataIndex() {
        return dataIndex;
    }

    public void setDataIndex(String dataIndex) {
        this.dataIndex = dataIndex;
    }

    public NextNode getNextNode() {
        return nextNode;
    }

    public void setNextNode(NextNode nextNode) {
        this.nextNode = nextNode;
    }

    @Override
    public String toString() {
        return "Vertex{" +
                toDegreesString() +
                ", dataIndex='" + dataIndex + '\'' + '}';
    }
}

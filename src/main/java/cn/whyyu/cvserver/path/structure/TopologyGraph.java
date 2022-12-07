package cn.whyyu.cvserver.path.structure;

import com.google.common.geometry.S2LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 路网拓扑结构的表达
 * 底层通过实现邻接链表结构
 */
public strictfp class TopologyGraph {
    /**
     * 初始容量4096，减少储存空间的频繁调整
     */
    private static List<Vertex> vertices = new ArrayList<>(4096);
    private static Map<String, Integer> dataIndexToVerticesIndex = new HashMap<>(4096);

    public static Vertex getVertex(String dataIndex) {
        int verticesIndex = dataIndexToVerticesIndex.get(dataIndex);
        return vertices.get(verticesIndex);
    }

    /**
     * 往拓扑路网中嵌入节点，且会依据dataIndex防止重复添加
     * @param dataIndex 节点的唯一id
     * @param s2LatLng 节点的经纬度
     */
    public static Vertex insertVertex(String dataIndex, S2LatLng s2LatLng) {
        if(!dataIndexToVerticesIndex.containsKey(dataIndex)){
            dataIndexToVerticesIndex.put(dataIndex, vertices.size());
            Vertex vertex = new Vertex(dataIndex, s2LatLng.latRadians(), s2LatLng.lngRadians(),
                    Math.cos(s2LatLng.latRadians()));
            vertices.add(vertex);
            return vertex;
        }
        return null;
    }

    public static void insertVertex(Vertex vertex) {
        if(!dataIndexToVerticesIndex.containsKey(vertex.dataIndex)) {
            dataIndexToVerticesIndex.put(vertex.dataIndex, vertices.size());
            vertices.add(vertex);
        }
    }

    public static void deleteVertex(String dataIndex) {
        //在构建的时候其实是用不上的，但写着玩,但添加操作简单，删除操作一般都很麻烦
        if(dataIndexToVerticesIndex.containsKey(dataIndex)){
            int verticesIndex = dataIndexToVerticesIndex.get(dataIndex);
            vertices.set(verticesIndex, new Vertex());

            NextNode previous;
            NextNode current;
            for(Vertex vertex : vertices){
                NextNode nextNode = vertex.nextNode;
                if(nextNode == null) continue;
                if(nextNode.dataIndex.equals(dataIndex) && nextNode.nextNode == null){
                    vertex.nextNode = null;
                    continue;
                }
                //这时nextNode等于链条中的第一个点
                current = nextNode;
                while (current != null){
                    previous = current;
                    current = current.nextNode;
                    if(current.dataIndex.equals(dataIndex)){
                        previous.nextNode = current.nextNode;
                        break;
                    }
                }
            }
            dataIndexToVerticesIndex.remove(dataIndex);
        }
    }

    //插入新边时，直接插在Vertex后面，而不是一行链表的最后面
    //不能插入环
    //代码简化，不再需要对nextNode是否为null做检查
    public static void insertEdge(String dataIndex1, String dataIndex2, double weight) {
        if(dataIndexToVerticesIndex.containsKey(dataIndex1) &&
                dataIndexToVerticesIndex.containsKey(dataIndex2) && !dataIndex1.equals(dataIndex2)){
            //-----拒绝重复添加边
            int tempVerticesIndex = dataIndexToVerticesIndex.get(dataIndex1);
            NextNode tempNextNode = vertices.get(tempVerticesIndex).nextNode;
            while (tempNextNode != null){
                if(tempNextNode.dataIndex.equals(dataIndex2)) return;
                tempNextNode = tempNextNode.nextNode;
            }
            //-----
            int verticesIndex1 = dataIndexToVerticesIndex.get(dataIndex1);
            NextNode newNode = new NextNode(dataIndex2, weight);
            newNode.nextNode = vertices.get(verticesIndex1).nextNode;
            vertices.get(verticesIndex1).nextNode = newNode;

            int verticesIndex2 = dataIndexToVerticesIndex.get(dataIndex2);
            newNode = new NextNode(dataIndex1, weight);
            newNode.nextNode = vertices.get(verticesIndex2).nextNode;
            vertices.get(verticesIndex2).nextNode = newNode;
        }
    }

    public static void deleteEdge(String dataIndex1, String dataIndex2) {
        if(dataIndexToVerticesIndex.containsKey(dataIndex1) &&
                dataIndexToVerticesIndex.containsKey(dataIndex2)){
            int verticesIndex1 = dataIndexToVerticesIndex.get(dataIndex1);
            NextNode current = vertices.get(verticesIndex1).nextNode;
            NextNode previous = null;
            //这步的判断是必须的，此时恰好第一个nextNode就是想要的点，单独处理，防止我们引用空指针previous,下同
            if(current.dataIndex.equals(dataIndex2)) {
                vertices.get(verticesIndex1).nextNode = current.nextNode;
                current = null;
            }
            while(current != null && !current.dataIndex.equals(dataIndex2)){
                previous = current;
                current = current.nextNode;
            }
            if(current != null) previous.nextNode = current.nextNode;

            int verticesIndex2 = dataIndexToVerticesIndex.get(dataIndex2);
            current = vertices.get(verticesIndex2).nextNode;
            previous = null;
            if(current.dataIndex.equals(dataIndex1)) {
                vertices.get(verticesIndex2).nextNode = current.nextNode;
                current = null;
            }
            while(current != null && !current.dataIndex.equals(dataIndex1)){
                previous = current;
                current = current.nextNode;
            }
            if(current != null) previous.nextNode = current.nextNode;
        }
    }

    /**
     获取所有不在closeMap中并与当前节点相邻的节点(形成Node列表返回给路径计算器)
     */
    public static List<Node> getUnclosedLinkedNode(Node node, HashMap<String, Double> closeMap) {
        int verticesIndex = dataIndexToVerticesIndex.get(node.dataIndex);
        NextNode current = vertices.get(verticesIndex).nextNode;
        int tempVerticesIndex;
        Vertex tempVertex;
        // 因为路网是稀疏图，一般来说每个节点不会拥有超过4个邻接节点(4个表明此节点刚好位于十字路口中心)
        // 默认ArrayList的大小是10，通过此操作我们应该能省下一些资源
        List<Node> nodeList = new ArrayList<>(5);
        // 检查所有相邻节点
        while(current != null){
            tempVerticesIndex = dataIndexToVerticesIndex.get(current.dataIndex);
            tempVertex = vertices.get(tempVerticesIndex);
            // 如果closeMap没有包含此节点
            if (!closeMap.containsKey(current.dataIndex)) {
                Node tempNode = new Node(tempVertex.dataIndex, tempVertex, node.gCost + current.weight);
                nodeList.add(tempNode);
            }
            current = current.nextNode;
        }
        return nodeList;
    }

    public static double getWeight(String pointS, String pointT) {
        int verticesIndex = dataIndexToVerticesIndex.get(pointS);
        NextNode current = vertices.get(verticesIndex).nextNode;
        while (!current.dataIndex.equals(pointT)) {
            current = current.nextNode;
        }
        return current.weight;
    }

    public static int getNumOfVertices() {
        return vertices.size();
    }

    public static List<Vertex> getVertices() {
        return vertices;
    }

    public static void setVertices(List<Vertex> vertices) {
        TopologyGraph.vertices = vertices;
    }

    public static Map<String, Integer> getDataIndexToVerticesIndex() {
        return dataIndexToVerticesIndex;
    }

    public static void setDataIndexToVerticesIndex(Map<String, Integer> dataIndexToVerticesIndex) {
        TopologyGraph.dataIndexToVerticesIndex = dataIndexToVerticesIndex;
    }

    @Override
    public String toString() {
        return "TopologyNetwork{" +
                "vertices=" + vertices +
                '}';
    }
}

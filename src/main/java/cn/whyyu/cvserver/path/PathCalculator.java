package cn.whyyu.cvserver.path;

import cn.whyyu.cvserver.path.structure.BinaryMinHeap;
import cn.whyyu.cvserver.path.structure.Node;
import cn.whyyu.cvserver.path.structure.TopologyGraph;
import com.google.common.geometry.S2Point;
import lombok.extern.slf4j.Slf4j;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 路径计算器
 * 依据构建的拓扑结构，获取最短路径
 */
@Slf4j
public class PathCalculator {
    // openMap需要存储节点，因为节点的相关信息在计算时是要使用的
    public HashMap<String, Node> openMap;
    // 为节省内存，closeMap无需再存储节点，因为我们的启发函数满足一致性要求，closeMap是不会reopen更新的
    // 也方便直接转化为landmarkState
    public HashMap<String, Double> closeMap;
    // 最小二叉堆，方便我们每次取出花费最小的节点
    public BinaryMinHeap<Node> minHeap;
    // pathCalculator当然可以重复使用，不然怎么叫计算器，所以需要这个变量来判断是否需要刷新openMap;closeMap;minHeap
    private boolean isUsed = false;
    // pathCalculator中Map以及Heap的初始大小
    private final int capacity;

    public PathCalculator() {
        this.capacity = 64;
        this.openMap = new HashMap<>(capacity);
        this.closeMap = new HashMap<>(capacity);
        this.minHeap = new BinaryMinHeap<>(Node.class, capacity / 2);
    }

    public PathCalculator(int capacity) {
        this.capacity = capacity;
        this.openMap = new HashMap<>(capacity);
        this.closeMap = new HashMap<>(capacity);
        this.minHeap = new BinaryMinHeap<>(Node.class, capacity / 2);
    }

    public List<String> getAstarShortestPath(Query query) {
        if (isUsed) {
            refresh();
        }
        // ----- 初始化
        List<String> resultList = new ArrayList<>(30);
        Node currentNode = new Node(query.sourceID, query.source, 0.0);
        String goalID = query.targetID;
        S2Point goal = query.target;
        // 初始化过程中就遗漏了一步，没有把起始点加入到closeMap中
        closeMap.put(currentNode.dataIndex, 0.0);

        // -----

//        double total = 0;
        // ----- 算法开始
        while (!currentNode.dataIndex.equals(goalID)) {
            for (Node node : TopologyGraph.getUnclosedLinkedNode(currentNode, closeMap)) {
                if (!openMap.containsKey(node.dataIndex)) {
                    double heuristics = getDistance(node, goal);
//                    total += 1;
                    node.total = node.gCost + heuristics;
                    node.parent = currentNode;
                    openMap.put(node.dataIndex, node);
                    minHeap.add(node);
                } else {
                    if (openMap.get(node.dataIndex).gCost > node.gCost) {
                        Node revisedNode = openMap.get(node.dataIndex);
                        revisedNode.total = revisedNode.total - (revisedNode.gCost - node.gCost);
                        revisedNode.gCost = node.gCost;
                        revisedNode.parent = currentNode;
                        minHeap.swim(revisedNode);
                    }
                }
            }
            if (!openMap.isEmpty()) {
                currentNode = minHeap.delMin();
                closeMap.put(currentNode.dataIndex, currentNode.gCost);
                openMap.remove(currentNode.dataIndex);
            } else {
                return new ArrayList<>();
            }
        }
        // -----

        isUsed = true;
        // 生成结果路径
        while (currentNode != null) {
            resultList.add(currentNode.dataIndex);
            currentNode = currentNode.parent;
        }
//        log.info("A*:" + String.valueOf(total) + "   query:" + query.sourceID);
//        log.info("A*:" + String.valueOf(resultList));
        return resultList;
    }

    public HashMap<String, Double> getCloseMap() {
        return closeMap;
    }

    /*
    注意是创建新的变量，而不是clear()清空
    这样可以防止以后代码扩展时，使用同一个pathCalculator意外的清空了可能需要保存的之前的计算结果
    至于内存回收，就让JVM去做吧
     */
    private void refresh() {
        this.openMap = new HashMap<>(capacity);
        this.closeMap = new HashMap<>(capacity);
        this.minHeap = new BinaryMinHeap<>(Node.class, capacity / 2);
        this.isUsed = false;
    }

    private double getDistance(Node source, S2Point target) {
        return target.getDistance(source.position);
    }

}

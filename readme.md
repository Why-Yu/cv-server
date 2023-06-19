### 概述

> 主要用于在线实时监控视频流图片的截取，传输给目标检测算法执行检测，但很遗憾没用上)D

* CameraController

没啥好说的，就是项目需要一个轨迹预测服务，预测目标下一个将出现的摄像头，缺少历史数据，用简单的拓扑关系做的

* TestController

VideoPush对监控进行推流，VideoPull获取监控视频流并生成一个个图片，利用线程池多线程实现，任务实现Runnable接口，redis负责消息传送，ftp负责图片文件传递

* VideoController

获得视频监控的推流地址
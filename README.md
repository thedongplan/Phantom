# ss-im

## 介绍
牛逼IM系统

## 软件架构

### 系统整体流程图
![](https://www.processon.com/view/link/5db6cb2ce4b0c5553743ab0d)

### 认证流程图

![](http://assets.processon.com/chart_image/5dc53e6ce4b005b5778bd235.png)

### 单聊、群聊

1. 离线消息

离线消息读取频率高

写扩散策略：发送消息之后，先把离线消息写入redis中，然后推送消息，推送成功，就删除消息。
群聊的消息在保存离线消息的时候，为群里每一个人都保一条记录，这样可以保证每个人的消息保存在redis cluster中的不同机器上，
根据senderid哈希，缓解每台机器的压力。

基于Redis的SortedSet存储离线消息


2. 历史消息

历史消息落地数据库，走读扩散原则。就是一条群消息保存到数据库中是一条记录，每个人去拉取群消息的时候，就读到这条消息。

因为历史消息是非常低频的访问行为，所以这个走读扩散原则。

3. 消息分库分表方案

单聊消息，根据senderid做分区key。同步AB的消息，先找出A发送给B的消息，然后找出B发送给A的消息。都会路由到同一个数据库的同一个表

群聊消息：根据grouid做分区key。同一个群聊的消息都路由到同一个数据库的同一个表中。

4. messageId唯一ID生成

采用Snowflake算法生成唯一ID


### 消息timeline模型

整个消息发送流程图如下：

![](http://assets.processon.com/chart_image/5dc905e1e4b0ffd214440983.png)

为了应对多端同步的功能，离线消息保存在一个timeline模型中。timeline可以理解为一个消息队列，对于同一个用户的消息，保存在同一个消息队列中。

每个消息有一个seqId，在队列中消息按照seqId递增来排序(后面的消息seqId比较大)，新的消息保存在队列尾部，

可以根据seqId随机定位到具体的某条消息进行读取，也可以指定范围读取任意的消息。


客户端(Web/APP)本地保存最大的消息ID，每次过来拉取消息的时候，带上本地最大的ID，这样一来就可以获取到比客户端最大消息ID更大的消息。

#### 缺陷分析：

消息可能会发送到不同的分发系统、不同的线程(分布式存储)、不同的kafka partition、会导致极端情况下数据不一致。Timeline模型数据不连续或者不完整

比如有AB两条消息，他们的时间非常相近，B > A,但是由于一些原因，B先进入了离线消息Timeline，此时客户端刚好过来拉取离线消息，此时就会发现丢了A消息。


#### 解决方案

对于同一个用户的消息hash到同一个分发系统、同一个线程、同一个partition、可以保证消息有序。。

但是对于群消息，由于需要做写扩散、极端情况下还是无法保证有序、此时可以在Timeline中为每个消息进行严格的消息递增id，比如1/2/3/4、这样，客户端拉取消息的时候

如果发现消息ID不顺序递增，可以发起重试请求。


### zookeeper工作流程

1. 客户端获取分发系统ip地址场景：

2. 接入系统获取分发系统地址列表场景：

### 单点登录方案




### 容错性分析

1. 客户端网络环境不好，挂了怎么处理

1.1 客户端挂了，接入系统会移除session、包括redis中的session。等待客户端重新连接。
1.2 在客户端挂了这段时间，分发系统发送消息的时候会发现推送不成功，此时要放入离线消息


2. 接入系统挂了怎么处理

2.1 对于分发系统来说，通过zookeeper感知到，需要移除分发系统实例。但是此时会发现有些消息推送对应的接入系统找不到了，
此时消息进入队列或者离线消息。等待客户端重新连接的时候，发起认证，更新了接入系统的地址，再将消息推送过去

2.2 对于客户端来说，分发系统挂了，则继续从zookeeper中查找下一个可用的接入系统地址，发起连接，重新认证。

3. 分发系统挂了怎么处理

3.1 对于接入系统来说，通过zookeeper感知到，需要移除分发系统的实例。



#### 参与贡献

中华石杉


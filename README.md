

# 思路&历程

内存占用 50MB --> 25MB --> ?? 

# 0x00
1. 构建: TierTree, 节点使用Node, char + parent引用 + List<Node>(子节点)
2. 搜索: 采用逐节点递归向下查找
3. 打印: 采用当前节点递归向上拼接StringBuffer, reverse之后, toString. 对每个节点采用同样操作. 


# 0x01
上一个方案, 打印涉及到 递归拼接 + 
 
1. 构建: TierTree, 节点使用Node, String + index + List<Node>(子节点) 
   1. 其中 String[index]可以无损替换之前的char, 存储了String, 这样就不用单独存储. 所有节点都引用同一个String. 应该更节省内存空间
2. 搜索: 采用逐节点递归向下查找
3. 打印: 直接将


# 0x02
发现上个方案, 内存消耗反而变差. 
分析下原因: 
如果String数量很多, 差异很大. 
决定回到 0x00 的方案, 但修改 char(2个字节) --> byte(1个字节) 
减少内存占用

1. 构建: TierTree, 节点使用Node, byte + parent引用 + List<Node>(子节点)


# 0xFE

* 看了最终答题解析, 思考问题可能出现在: 过多地创建了Java(即Node)对象.
空对象占用大小: 在32位系统下：
java空对象占8个字节，对象的引用占4个字节。
所以上面那条语句所占的空间是4byte+8byte=12byte.
java中的内存是以8字节的倍数来分配的，所以分配的内存是16byte.

* 所以使用RadixTree可以减少Node对象的数量, 可以减少空间占用

# 0xFF
其他优化思路:
* 多线程查找 + 打印, 优化速度
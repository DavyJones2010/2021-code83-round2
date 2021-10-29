// 请不要修改包名
package com.aliyun.code83.round2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.FunctionComputeLogger;
import com.aliyun.fc.runtime.FunctionInitializer;
import com.aliyun.fc.runtime.StreamRequestHandler;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import org.apache.commons.io.IOUtils;

/**
 * <pre>
 * 代码API名称前缀搜索
 *
 * 请不要修改类名CodePrefixSearchRequest
 * </pre>
 *
 * @author DevStudio
 * @author Alibaba Cloud AI Coding Assistant
 */
public class CodePrefixSearchRequest implements StreamRequestHandler, FunctionInitializer {

    /**
     * <pre>
     * 搜索代码API名称前缀
     *
     * 输入数据格式：
     * {
     * "prefixs":["",""],  // 字符串前缀
     * "oss_endpoint": "", // OSS访问endpoint,
     * "oss_key": "",  // OSS数据集访问地址
     * "bucket": "",  // OSS Bucket
     * "access_key": "", // OSS 访问AK
     * "access_secret": "" // OSS 访问SK
     * }
     *
     * 输出格式:
     * { "prefix1": [""], "prefix2": ["", ""] }
     *
     * 数据示例：
     * 输入：
     *  {"prefixs":["Buffer","Str"], "oss_endpoint": "*****", "oss_key": "*****" "bucket": "*****", "access_key": "*****", "access_secret": "*****"}
     *
     * 输出：
     * {
     *  "Buffer":["BufferedReader", "BufferedInputStream"]，
     *  "Str":["String", "StringBuilder"]，
     * }
     *
     * 注：请不要修改方法名
     * </pre>
     *
     * @param input   输入流
     * @param output  输出流
     * @param context 上下文信息
     * @throws IOException IO异常
     */
    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        FunctionComputeLogger logger = context.getLogger();
        logger.debug(String.format("RequestID is %s %n", context.getRequestId()));
        String inputJson = IOUtils.toString(input, "UTF-8");
        if (inputJson == null || "".equals(inputJson)) {
            error(output, "invalid input json string.");
            return;
        }
        // 解析输入JSON数据
        JSONObject params = JSON.parseObject(inputJson);
        if (params == null) {
            error(output, "invalid input json data.");
            return;
        }
        // 创建OSS Client
        OSS ossClient = createOSSClient(params);
        // 实现算法并返回结果
        Map<String, List<String>> result = calculate(ossClient, params);
        // 输出结果
        output.write(JSON.toJSONString(result).getBytes());
        output.flush();
    }

    @Override
    public void initialize(Context context) throws IOException {
        FunctionComputeLogger logger = context.getLogger();
        logger.debug(String.format("RequestID is %s %n", context.getRequestId()));
    }

    /**
     * 创建OSS Client
     *
     * @param params 输入参数
     * @return
     */
    @SuppressWarnings("unused")
    private OSS createOSSClient(JSONObject params) {
        String endpoint = params.getString("oss_endpoint");
        String accessKeyId = params.getString("access_key");
        String secretAccessKey = params.getString("access_secret");
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, secretAccessKey);
        return ossClient;
    }

    /**
     * <pre>
     * 计算前缀字符串查找
     *
     * 输出格式:
     * { "prefix1": [""], "prefix2": ["", ""] }
     *
     * 示例：
     * {
     *    "Buffer":["BufferedReader", "BufferedInputStream"]，
     *    "Str":["String", "StringBuilder"]，
     * }
     * </pre>
     *
     * @param ossClient OSS Client
     * @param params    输入参数
     * @return 计算结果
     * @throws IOException IO异常
     */
    @SuppressWarnings("unused")
    private Map<String, List<String>> calculate(OSS ossClient, JSONObject params) throws IOException {
        // OSS文件地址
        String ossFileKey = params.getString("oss_key");
        // OSS Bucket
        String ossBucket = params.getString("bucket");
        // 待计算的前缀字符串集合
        JSONArray prefixs = params.getJSONArray("prefixs");
        // 计算结果，key为前缀字符串，value为匹配前缀的字符串集合
        Map<String, List<String>> result = new HashMap<>();

        OSSObject ossObject = ossClient.getObject(ossBucket, ossFileKey);
        BufferedReader reader = new BufferedReader(new InputStreamReader(ossObject.getObjectContent()));
        TierTree tierTree = new TierTree();
        while (true) {
            String line = reader.readLine();
            if (line == null) {break;}
            tierTree.addToTree(line);
        }

        // 数据读取完成后，获取的流必须关闭，否则会造成连接泄漏，导致请求无连接可用，程序无法正常工作。
        reader.close();
        // 关闭OSSClient。
        ossClient.shutdown();
        List<String> empty = new ArrayList<>(0);
        for (Object prefix : prefixs) {
            Node search = tierTree.search((String)prefix);
            if (null == search) {
                result.put((String)prefix, empty);
            } else {
                List<String> traverse = search.traverse();
                result.put((String)prefix, traverse);
            }
        }
        return result;
    }

    /**
     * 输出错误信息
     *
     * @param output  输出流
     * @param message 错误信息
     */
    private void error(OutputStream output, String message) throws IOException {
        Map<String, String> msg = new HashMap<>();
        msg.put("errorMessage", message);
        output.write(JSON.toJSONString(msg).getBytes());
        output.flush();
    }

    class TierTree {
        List<Node> subs = new ArrayList<>(128);

        void addToTree(String bytes) {
            if (bytes.length() == 0) {
                return;
            }
            Node n = new Node(bytes, 0);
            int idx = subs.indexOf(n);
            if (idx >= 0) {
                // 当前Node已经包含在TierTree中, 则将子节点加入
                Node subTree = subs.get(idx);
                if (0 == (bytes.length() - 1)) {
                    // 字符串结尾
                    subTree.isLeaf = true;
                } else {
                    subTree.addToTree(bytes, 1);
                }
            } else {
                subs.add(n);
                if (bytes.length() == 1) {
                    // 字符串结尾
                    n.isLeaf = true;
                } else {
                    // 字符串中间, 则构建下一层
                    n.addToTree(bytes, 1);
                }
            }
        }

        Node search(String bytes) {
            if (bytes.length() == 0) {
                return null;
            }
            int idx = 0;
            Node n = new Node(bytes, idx);
            // 目标字符串尚未结束
            int i = subs.indexOf(n);
            if (i >= 0) {
                Node subNode = subs.get(i);
                return subNode.search(bytes, idx);
            } else {
                return null;
            }
        }
    }

    class Node {
        // 全路径字符串
        String fullPath;
        // 从0开始
        int height;
        boolean isLeaf;
        List<Node> subs;

        Node(String fullPath, int height) {
            this.fullPath = fullPath;
            this.height = height;
        }

        Node search(String bytes, int idx) {
            Node n = new Node(bytes, idx);
            if (this.equals(n)) {
                if (idx == (bytes.length() - 1)) {
                    // 如果匹配到字符串结尾, 则将当前节点返回
                    return this;
                } else {
                    // 目标字符串尚未结束
                    Node nextNode = new Node(bytes, idx + 1);
                    if (subs == null) {
                        return null;
                    } else {
                        int i = subs.indexOf(nextNode);
                        if (i >= 0) {
                            Node subNode = subs.get(i);
                            return subNode.search(bytes, idx + 1);
                        } else {
                            return null;
                        }
                    }
                }
            }
            return null;
        }

        // 将节点加到this的下一层
        public void addToTree(String bytes, int idx) {
            Node n = new Node(bytes, idx);
            if (subs == null) {
                subs = new ArrayList<>();
            }
            int i = subs.indexOf(n);
            if (i >= 0) {
                Node subTree = subs.get(i);
                if (idx == (bytes.length() - 1)) {
                    // 字符串结尾
                    subTree.isLeaf = true;
                } else {
                    subTree.addToTree(bytes, idx + 1);
                }
            } else {
                subs.add(n);
                if (idx == (bytes.length() - 1)) {
                    // 字符串结尾
                    n.isLeaf = true;
                } else {
                    // 字符串中间, 则构建下一层
                    n.addToTree(bytes, idx + 1);
                }
            }
        }

        List<String> traverse() {
            List<String> strs = null;
            if (this.isLeaf) {
                strs = new ArrayList<>(1);
                strs.add(this.toString());
                return strs;
            }
            if (this.subs != null) {
                strs = new ArrayList<>();
                for (Node sub : this.subs) {
                    strs.addAll(sub.traverse());
                }
            }
            return strs;
        }

        @Override
        public String toString() {
            return fullPath.substring(0, height + 1);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {return true;}
            if (o == null || getClass() != o.getClass()) {return false;}
            Node node = (Node)o;
            return fullPath.charAt(height) == node.fullPath.charAt(height);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fullPath, height);
        }
    }
}
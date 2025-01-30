import erangel.connector.http.HttpRequest;
import erangel.connector.http.HttpResponse;
import erangel.connector.http.HttpResponseStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletOutputStream;
import java.io.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HttpResponseStreamTestTwo {

    private HttpResponse response;
    private HttpResponseStream responseStream;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void setUp() {
        response = new HttpResponse();
        outputStream = new ByteArrayOutputStream();
        HttpRequest httpRequest = new HttpRequest();
        httpRequest.setProtocol("HTTP/1.1");
        httpRequest.setUri("/test/resource");
        httpRequest.setMethod("GET");
        httpRequest.setHeaders(createMockHeaders());
        httpRequest.setResponse(response);
        response.setRequest(httpRequest);
        response.setStream(outputStream); // 将模拟流设置到响应对象中
    }

    private Map<String, List<String>> createMockHeaders() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("text/plain"));
        headers.put("User-Agent", Collections.singletonList("MockTestClient/1.0"));
        return headers;
    }

    @Test
    void testChunkedTransferEncodingExactChunkSize() throws IOException {
        response.setAllowChunking(true); // 启用 Chunked 传输
        ServletOutputStream outputStream = response.getOutputStream();

        // 准备 1024 字节的内容
        byte[] chunk = new byte[1024];
        Arrays.fill(chunk, (byte) 'X'); // 使用 'X' 填充

        // 写入数据并刷新
        outputStream.write(chunk);
        response.flushBuffer();
        response.finishResponse();

        // 验证输出结果
        String result = this.outputStream.toString();
        System.out.println(result);

        // 验证 1024 字节块大小的正确性
        assertTrue(result.contains("400\r\n")); // 十六进制 1024 为 400
        assertTrue(result.contains(new String(chunk))); // 验证数据是否正确写入
        // 验证结束符
        assertTrue(result.contains("0\r\n\r\n"));
    }

    @Test
    void testChunkedTransferEncodingPartialChunk() throws IOException {
        response.setAllowChunking(true); // 启用 Chunked 传输
        ServletOutputStream outputStream = response.getOutputStream();

        // 准备非完整 Chunk（小于 1024 字节）
        byte[] partialChunk = new byte[512];
        Arrays.fill(partialChunk, (byte) 'Y');

        // 写入数据并刷新
        outputStream.write(partialChunk);
        response.flushBuffer();
        response.finishResponse();

        // 验证输出结果
        String result = this.outputStream.toString();
        System.out.println(result);

        // 验证块大小和数据内容
        assertTrue(result.contains("200\r\n")); // 512 十六进制为 200
        assertTrue(result.contains(new String(partialChunk)));
        // 验证结束符
        assertTrue(result.contains("0\r\n\r\n"));
    }

    @Test
    void testChunkedTransferEncodingMultipleChunks() throws IOException {
        response.setAllowChunking(true); // 启用 Chunked 传输
        ServletOutputStream outputStream = response.getOutputStream();

        // 准备超过 1024 字节的数据
        byte[] buffer = new byte[4096];
        Arrays.fill(buffer, (byte) 'Z'); // 使用 'Z' 填充

        // 写入数据并刷新
        outputStream.write(buffer);
        response.flushBuffer();
        response.finishResponse();

        // 验证输出结果
        String result = this.outputStream.toString();
        System.out.println(result);

        // 验证分块正确性：4 个 1024 字节块
        assertTrue(result.contains("400\r\n")); // 每个块大小为 1024 字节
        assertEquals(4, countOccurrences(result, "400\r\n")); // 验证有 4 次块头出现
        // 验证结束符
        assertTrue(result.contains("0\r\n\r\n"));
    }

    @Test
    void testBufferedWrite() throws IOException {
        response.setAllowChunking(false); // 无需开启 Chunked 传输（普通写入）
        ServletOutputStream outputStream = response.getOutputStream();

        // 准备 8192 字节（等于缓冲区大小）的数据
        byte[] buffer = new byte[8192];
        Arrays.fill(buffer, (byte) 'B');

        // 写入数据并刷新
        outputStream.write(buffer);
        response.flushBuffer();
        response.finishResponse();

        // 验证输出结果
        String result = this.outputStream.toString();
        System.out.println(result);

        // 普通写入不含 Chunked 编码
        assertTrue(result.contains(new String(buffer))); // 验证是否包含完整数据
    }

    @Test
    void testChunkedTransferWithOverflow() throws IOException {
        response.setAllowChunking(true); // 启用 Chunked 传输
        ServletOutputStream outputStream = response.getOutputStream();

        // 准备超过缓冲区大小的数据（10 KB）
        byte[] buffer = new byte[10240];
        Arrays.fill(buffer, (byte) 'C'); // 使用 'C' 填充

        // 写入数据并刷新
        outputStream.write(buffer);
        response.flushBuffer();
        response.finishResponse();

        // 验证输出结果
        String result = this.outputStream.toString();
        System.out.println(result);

        // 验证分块正确性
        // 应有 10 个块头，每个块为 1024 字节（chunk size: 400 十六进制）
        assertEquals(10, countOccurrences(result, "400\r\n")); // 10 次 `400\r\n`
        // 验证结束块标志
        assertTrue(result.contains("0\r\n\r\n")); // 验证终止块是否写入
    }

    @Test
    void testChunkedTransferWithLargeIrregularBlock() throws IOException {
        // 获取类所在目录
        String classPath = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        File classFile = new File(classPath, this.getClass().getPackageName().replace(".", File.separator));
        if (!classFile.exists()) {
            classFile.mkdirs(); // 如果目录不存在，则创建该目录
        }

        // 构造文件路径，文件名为函数名
        String functionName = new Object() {
        }.getClass().getEnclosingMethod().getName(); // 获取方法名
        File logFile = new File(classFile, functionName + ".txt"); // 生成类似 `/com/example/testChunkedTransferWithLargeIrregularBlock.txt`

        // 重定向控制台输出到文件
        PrintStream originalOut = System.out; // 保存默认控制台输出流
        try (PrintStream fileOut = new PrintStream(logFile)) {
            System.setOut(fileOut); // 将控制台输出重定向到文件

            // 测试代码逻辑
            response.setAllowChunking(true);
            ServletOutputStream outputStream = response.getOutputStream();

            // 准备1.5 MB = 1,572,864 字节
            int blockSize = 1572864;
            byte[] buffer = new byte[blockSize];
            Arrays.fill(buffer, (byte) 'A');

            // 写入大块数据并刷新
            outputStream.write(buffer);
            response.flushBuffer();
            response.finishResponse();

            // 验证输出结果
            String result = this.outputStream.toString();
            System.out.println(result); // 输出内容将写入函数名对应的文件中

            // 验证分块正确性
            int chunkSizeHex = 1024; // 每个块为 1024 字节
            String chunkHeader = Integer.toHexString(chunkSizeHex) + "\r\n";
            assertEquals(1536, countOccurrences(result, chunkHeader)); // 验证每个分块头的出现次数

            // 验证终止块是否正确
            assertTrue(result.contains("0\r\n\r\n")); // 检查终止块
        } finally {
            // 恢复默认控制台输出流
            System.setOut(originalOut);
        }

        System.out.println("日志已输出到: " + logFile.getAbsolutePath());
    }
    @Test
    void testChunkedTransferWithIrregularBlock() throws IOException {
        // 获取类所在目录
        String classPath = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        File classFile = new File(classPath, this.getClass().getPackageName().replace(".", File.separator));
        if (!classFile.exists()) {
            classFile.mkdirs(); // 如果目录不存在，则创建该目录
        }

        // 构造文件路径，文件名为函数名
        String functionName = new Object() {
        }.getClass().getEnclosingMethod().getName(); // 获取方法名
        File logFile = new File(classFile, functionName + ".txt");
        // 重定向控制台输出到文件
        PrintStream originalOut = System.out; // 保存默认控制台输出流
        try (PrintStream fileOut = new PrintStream(logFile)) {
            System.setOut(fileOut); // 将控制台输出重定向到文件

            // 测试代码逻辑
            response.setAllowChunking(true);
            ServletOutputStream outputStream = response.getOutputStream();

            // 准备一个大小不是 1024 倍数的块，例如 1500 字节
            int blockSize = 1500;
            byte[] buffer = new byte[blockSize];
            Arrays.fill(buffer, (byte) 'A');

            // 写入不规则大小的块数据并刷新
            outputStream.write(buffer);
            response.flushBuffer();
            response.finishResponse();

            // 验证输出结果
            String result = this.outputStream.toString();
            System.out.println(result); // 输出内容将写入函数名对应的文件中

            // 验证分块正确性
            String chunkHeader1 = "400" + "\r\n";
            String chunkHeader2= "1dc"+ "\r\n";
            assertTrue(result.contains(chunkHeader1)); // 验证分块头是否正确
            assertTrue(result.contains(chunkHeader2));

            // 验证终止块是否正确
            assertTrue(result.contains("0\r\n\r\n")); // 检查终止块
        } finally {
            // 恢复默认的控制台输出流
            System.setOut(originalOut);
        }

        System.out.println("日志已输出到: " + logFile.getAbsolutePath());
    }

    /**
     * 工具方法：统计字符串中子串出现的次数
     *
     * @param source 原始字符串
     * @param target 子串
     * @return 出现次数
     */
    private int countOccurrences(String source, String target) {
        int count = 0;
        int idx = 0;
        while ((idx = source.indexOf(target, idx)) != -1) {
            count++;
            idx += target.length();
        }
        return count;
    }
}
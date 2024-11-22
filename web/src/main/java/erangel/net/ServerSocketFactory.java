package erangel.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * 描述工厂类共同特征的接口来创建服务器套接字。
 * 该接口的一个具体实现将通过 <code>setFactory()</code> 方法被分配给某个连接器。
 *
 * @author LILINJIAN
 * @version 2024/11/20 17:23
 */
public interface ServerSocketFactory {

    /**
     * 返回一个服务器套接字，
     * 该套接字使用主机上的所有网络接口，并绑定到指定的端口。
     * 该套接字配置了提供给此工厂的套接字选项（例如接受超时）。
     *
     * @param port 监听的端口
     * @throws IOException IO异常或者网络异常
     */
    public ServerSocket createSocket(int port) throws IOException;

    /**
     * 返回一个服务器套接字，该套接字使用主机上的所有网络接口，
     * 绑定到指定的端口，并规定连接排队数量（backlog）。
     * 该套接字配置了提供给此工厂的套接字选项（例如接受超时）。
     *
     * @param port    监听的端口
     * @param backlog 多少连接可排队
     * @throws IOException IO异常或网络异常
     */
    public ServerSocket createSocket(int port, int backlog) throws IOException;

    /**
     * 返回一个服务器套接字，该套接字仅使用本地主机上指定的网络接口，
     * 绑定到指定的端口，并规定连接排队数量（backlog）。
     * 该套接字配置了提供给此工厂的套接字选项（例如接受超时）。
     *
     * @param port      监听的端口
     * @param backlog   多少连接可排队
     * @param ifAddress 要使用的网络接口地址
     * @throws IOException IO异常或网络异常
     */
    public ServerSocket createSocket(int port, int backlog, InetAddress ifAddress) throws IOException;
}

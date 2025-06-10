package livonia.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * 默认服务器套接字工厂，返回未修饰的服务器套接字。
 *
 * @author LILINJIAN
 * @version 2024/11/20 17:23
 */
public final class DefaultServerSocketFactory implements ServerSocketFactory {

    /**
     * 返回一个服务器套接字，
     * 该套接字使用主机上的所有网络接口，并绑定到指定的端口。
     *
     * @param port 监听的端口
     * @throws IOException IO异常或者网络异常
     */
    public ServerSocket createSocket(int port) throws IOException {
        return (new ServerSocket(port));
    }

    /**
     * 返回一个服务器套接字，
     * 该套接字使用主机上的所有网络接口，绑定到指定的端口，
     * 并规定连接排队数量（backlog）。
     *
     * @param port    监听的端口
     * @param backlog 多少连接可排队
     * @throws IOException IO异常或网络异常
     */
    public ServerSocket createSocket(int port, int backlog) throws IOException {
        return (new ServerSocket(port, backlog));
    }

    /**
     * 返回一个服务器套接字，
     * 该套接字仅使用本地主机上指定的网络接口，绑定到指定的端口，
     * 并规定连接排队数量（backlog）。
     *
     * @param port      监听的端口
     * @param backlog   多少连接可排队
     * @param ifAddress 要使用的网络接口地址
     * @throws IOException IO异常或网络异常
     */
    public ServerSocket createSocket(int port, int backlog, InetAddress ifAddress) throws IOException {
        return (new ServerSocket(port, backlog, ifAddress));
    }
}

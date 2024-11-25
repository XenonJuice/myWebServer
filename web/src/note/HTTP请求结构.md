### HTTP 请求示例

#### 请求行
- **请求方法**: `POST`
- **请求路径**: `/protected-resource`
- **协议版本**: `HTTP/1.1`

#### 请求头
| 请求头名称          | 值                                       | 说明                          |
|---------------------|------------------------------------------|-------------------------------|
| Host                | `www.example.com`                        | 目标主机名                    |
| User-Agent          | `Mozilla/5.0 (Windows NT 10.0; ...)`     | 客户端标识                    |
| Authorization       | `Bearer <token>`                         | 认证信息（Bearer Token）      |
| Accept              | `application/json`                       | 客户端期望的响应格式          |
| Accept-Language     | `en-US,en;q=0.9`                         | 客户端语言偏好                |
| Accept-Encoding     | `gzip, deflate, br`                      | 支持的压缩方式                |
| Content-Type        | `application/json`                       | 请求体数据类型                |
| Content-Length      | `72`                                     | 请求体数据字节长度            |
| Cookie              | `sessionId=abc123xyz; userPref=darkMode` | 客户端携带的 Cookie           |
| Custom-Header       | `customValue123`                         | 自定义请求头字段              |
| Connection          | `keep-alive`                             | 保持连接                      |

#### 请求体
```json
{
  "username": "john_doe",
  "password": "secure_password"
}

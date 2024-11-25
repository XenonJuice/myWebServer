### HTTP 响应示例

#### 状态行
- **协议版本**: `HTTP/1.1`
- **状态码**: `200` (OK)
- **描述**: 请求已成功处理

#### 响应头
| 响应头名称                    | 值                                                               | 说明                           |
|-------------------------------|------------------------------------------------------------------|--------------------------------|
| Date                          | `Wed, 20 Nov 2024 12:00:00 GMT`                                  | 服务器生成响应的时间           |
| Server                        | `Apache/2.4.41 (Ubuntu)`                                         | 服务器软件信息                 |
| Content-Type                  | `application/json; charset=UTF-8`                                | 响应体数据类型                 |
| Content-Length                | `123`                                                            | 响应体字节长度                 |
| Cache-Control                 | `no-cache, no-store, must-revalidate`                            | 缓存控制策略                   |
| Set-Cookie                    | `sessionId=def456uvw; Path=/; Secure; HttpOnly`                  | 设置新 Cookie (sessionId)      |
| Set-Cookie                    | `userPref=lightMode; Path=/; Expires=Thu, 21 Nov 2024 12:00:00 GMT` | 设置新 Cookie (userPref)       |
| Strict-Transport-Security     | `max-age=31536000; includeSubDomains`                            | 强制 HTTPS 策略                |
| Connection                    | `keep-alive`                                                     | 保持连接                       |

#### 响应体
```json
{
  "status": "success",
  "message": "Authenticated successfully",
  "data": {
    "userId": "12345",
    "roles": ["admin", "editor"],
    "token": "eyJhbGciOiJIUzI1NiIsInR5c..."
  }
}

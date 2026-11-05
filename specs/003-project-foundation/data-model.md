# 数据与配置

无新增持久化实体或DDL。

- `ApiResponse<T>`：code为固定字符串，message为安全固定说明，data为结果或null，timestamp为UTC Instant。
- 错误码与HTTP状态绑定，原RuntimeFailure映射保留SafeErrorMapper的固定文案。
- 请求关联标识只保存在MDC与内部请求属性，不新增响应字段或头；不采纳外部不可信标识。
- 消息长度按UTF-8字节计数，默认上限32768；空值与必填约束分开。
- 管理服务使用127.0.0.1独立端口，默认9091；文档仅local-docs模式开启，JSON日志由json-logs模式开启。

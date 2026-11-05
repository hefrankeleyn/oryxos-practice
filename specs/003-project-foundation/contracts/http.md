# HTTP基础契约

本功能不增加业务Controller。测试fixture的路由不得出现在产品JAR。

| 状态 | code | 场景 |
| --- | --- | --- |
| 200 | OK | 成功结果通过ApiResponse.success构造 |
| 400 | BAD_REQUEST | 参数格式或校验失败 |
| 404 | NOT_FOUND | 业务资源或业务路径不存在 |
| 500 | INTERNAL_ERROR | 未分类内部异常 |
| 503 | PROVIDER_UNAVAILABLE | Provider故障 |
| 504 | GATEWAY_TIMEOUT | 显式请求超时错误 |

成功与错误始终使用code/message/data/timestamp；不展示原始异常及输入值。
RuntimeFailure其他类别保留内部安全错误码及固定文案，按类别映射400/403/500/503。
业务异常处理限定/api/v1及项目业务控制器；Actuator、Prometheus、OpenAPI和Swagger保持原协议。
管理入口：独立回环端口的/actuator/health、/actuator/info、/actuator/prometheus；健康详情不公开。
文档：默认关闭/v3/api-docs与/swagger-ui.html；local-docs模式在回环主端口开启。

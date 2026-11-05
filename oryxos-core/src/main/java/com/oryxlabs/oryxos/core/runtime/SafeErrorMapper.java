package com.oryxlabs.oryxos.core.runtime;

import java.util.UUID;

/** 用户确认的固定错误文案。原始异常、路径值、工具参数均不进入呈现结果。 */
public final class SafeErrorMapper {
  private SafeErrorMapper() {}

  public static String message(RuntimeFailure.Code code) {
    return switch (code) {
      case CONFIGURATION -> "配置无效，请检查指定字段。";
      case CONTEXT_READ -> "上下文文件读取失败，请检查文件及访问权限。";
      case PROVIDER -> "模型调用失败，请检查服务配置或稍后重试。";
      case PROTOCOL -> "模型响应格式无效。";
      case TOOL_VALIDATION -> "工具请求无效或未获允许。";
      case TOOL_EXECUTION -> "工具执行失败。";
      case SANDBOX_DENIED -> "Sandbox 拒绝此操作。";
      case ITERATION_LIMIT -> "已达到最大迭代次数，本次处理已停止。";
      case AUDIT_UNAVAILABLE -> "审计写入失败，本次处理已停止；已发生的操作不会自动重放。";
    };
  }

  public static String render(RuntimeFailure failure, UUID id) {
    return message(failure.code())
        + (failure.field().isEmpty() ? "" : "（字段：" + failure.field() + "）")
        + (id == null ? "" : "（调用编号：" + id + "）");
  }
}

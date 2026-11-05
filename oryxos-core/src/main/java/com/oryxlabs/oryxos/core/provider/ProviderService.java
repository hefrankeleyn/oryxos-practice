package com.oryxlabs.oryxos.core.provider;

/** 单次同步模型调用端口；调用方应使用带审计的装饰器，不执行工具或自动重试。 */
@FunctionalInterface
public interface ProviderService {
  ProviderResponse call(ProviderRequest request);
}

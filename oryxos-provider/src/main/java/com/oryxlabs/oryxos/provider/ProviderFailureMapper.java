package com.oryxlabs.oryxos.provider;

import com.oryxlabs.oryxos.core.runtime.RuntimeFailure;
/** 只转受控类别，不将 HTTP 响应或原始 cause 带入终端和审计。 */
public final class ProviderFailureMapper {
 private ProviderFailureMapper(){}
 public static RuntimeFailure map(RuntimeException failure){return failure instanceof RuntimeFailure safe?safe:new RuntimeFailure(RuntimeFailure.Code.PROVIDER);}
}

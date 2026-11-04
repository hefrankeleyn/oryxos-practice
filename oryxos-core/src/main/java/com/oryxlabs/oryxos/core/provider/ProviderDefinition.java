package com.oryxlabs.oryxos.core.provider;

import java.net.URI;
import java.time.Duration;
import com.oryxlabs.oryxos.core.runtime.RuntimeFailure;
/** 客户端启动快照；密钥仅在内存使用，toString 永不输出密钥。 */
public record ProviderDefinition(String name,URI baseUrl,String apiKey,Duration connectTimeout,Duration readTimeout){
 public ProviderDefinition {
  if(name==null||!name.matches("[A-Za-z0-9_-]+")||baseUrl==null||!java.util.Set.of("http","https").contains(baseUrl.getScheme())
    ||baseUrl.getHost()==null||baseUrl.getUserInfo()!=null||baseUrl.getQuery()!=null||baseUrl.getFragment()!=null||apiKey==null||apiKey.isBlank()
    ||connectTimeout==null||readTimeout==null||connectTimeout.isNegative()||connectTimeout.isZero()||readTimeout.isNegative()||readTimeout.isZero())throw new RuntimeFailure(RuntimeFailure.Code.CONFIGURATION,"oryxos.providers");
 }
 @Override public String toString(){return "ProviderDefinition[redacted]";}
}

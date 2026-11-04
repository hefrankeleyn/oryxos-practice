package com.oryxlabs.oryxos.core.model;
import org.junit.jupiter.api.Test;
import com.oryxlabs.oryxos.core.runtime.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class CoreModelsTest {
 @Test void snapshotsRejectTraversalInvalidLimitsAndDuplicateTools(){
  for(String name:List.of("..","a/b","a\\b","."))assertThatThrownBy(()->new ProfileSnapshot(name,"p","m",null,List.of(),List.of(),10,20,"")).isInstanceOf(RuntimeFailure.class);
  assertThatThrownBy(()->new ProfileSnapshot("a","p","m",Double.NaN,List.of(),List.of(),10,20,"")).isInstanceOf(RuntimeFailure.class);
  assertThatThrownBy(()->new ProfileSnapshot("a","p","m",null,List.of("x","x"),List.of(),10,20,"")).isInstanceOf(RuntimeFailure.class);
 }
 @Test void identityIsStructuredAndInstancesNeverReuseIdentity(){
  assertThat(new SessionKey("a:b","c","d")).isNotEqualTo(new SessionKey("a","b:c","d"));
  var k=new SessionKey("cli","local","agent");assertThat(new Session(k).id()).isNotEqualTo(new Session(k).id());
 }
 @Test void errorMapperDoesNotRetainUntrustedCauseOrFieldValues(){
  var failure=new RuntimeFailure(RuntimeFailure.Code.PROVIDER,"key=secret");
  assertThat(SafeErrorMapper.render(failure,null)).isEqualTo("模型调用失败，请检查服务配置或稍后重试。");
  assertThat(failure.getCause()).isNull();
 }
}

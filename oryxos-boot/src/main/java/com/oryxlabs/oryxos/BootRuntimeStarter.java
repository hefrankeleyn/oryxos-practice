package com.oryxlabs.oryxos;
import com.oryxlabs.oryxos.core.runtime.*;
import com.oryxlabs.oryxos.core.provider.ProviderDefinition;
import com.oryxlabs.oryxos.cli.ConfigLoader;
import org.springframework.boot.*;
import org.springframework.context.support.GenericApplicationContext;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import java.io.*;
/** chat 专用 NON_WEB 生命周期；配置先安全校验再启动，句柄关闭时释放整个上下文。 */
public final class BootRuntimeStarter implements RuntimeStarter {
 private final Function<String,String> environment;
 public BootRuntimeStarter(){this(System::getenv);}
 BootRuntimeStarter(Function<String,String> environment){this.environment=environment;}
 public RuntimeHandle start(Path home,String profile){
  var raw=System.getProperty("spring.config.additional-location",environment.apply("SPRING_CONFIG_ADDITIONAL_LOCATION"));
  if(raw==null||!raw.startsWith("file:"))throw new RuntimeFailure(RuntimeFailure.Code.CONFIGURATION,"runtime.yaml");
  var definitions=new ConfigLoader().load(Path.of(raw.substring(5)),environment);
  var inputs=new Inputs(home,profile,definitions,environment);
  var app=new SpringApplication(OryxOsApplication.class);app.setWebApplicationType(WebApplicationType.NONE);
  app.setBannerMode(Banner.Mode.OFF);app.setLogStartupInfo(false);app.setRegisterShutdownHook(false);
  app.setDefaultProperties(Map.of("logging.config","classpath:oryxos-cli-logback.xml","logging.level.root","WARN"));
  app.addInitializers(context->((GenericApplicationContext)context).registerBean(Inputs.class,()->inputs));
  try {
   var context=app.run("--oryxos.runtime.enabled=true","--oryxos.home="+home.toAbsolutePath());
   try{
    var components=context.getBean(RuntimeConfiguration.RuntimeComponents.class);
    return new RuntimeHandle(){
     public int interact(Reader input,PrintWriter out,PrintWriter error){return components.channel().interact(input,out,error);}
     public void close(){context.close();}
    };
   }catch(RuntimeException e){context.close();throw e;}
  }catch(RuntimeException failure){
   for(Throwable t=failure;t!=null;t=t.getCause())if(t instanceof RuntimeFailure safe)throw safe;
   throw new RuntimeFailure(RuntimeFailure.Code.CONFIGURATION,"runtime");
  }
 }
 record Inputs(Path home,String profile,Map<String,ProviderDefinition> providers,Function<String,String> environment){}
}

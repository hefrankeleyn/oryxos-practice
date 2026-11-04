package com.oryxlabs.oryxos.channel.cli;

import com.oryxlabs.oryxos.core.agent.AgentService;
import com.oryxlabs.oryxos.core.model.Session;
import com.oryxlabs.oryxos.core.runtime.*;
import java.io.*;
/** 顺序 stdin/stdout 接入；空白与退出不创建调用，不输出内部调试信息。 */
public final class CliChannel {
 private final AgentService service;private final Session session;
 public CliChannel(AgentService service,Session session){this.service=service;this.session=session;}
 public int interact(Reader source,PrintWriter out,PrintWriter error){
  var input=new BufferedReader(source);int result=0;
  try{
   String line;while((line=input.readLine())!=null){if(line.equals("/quit"))break;if(line.isBlank())continue;
    try{String response=service.process(session,line);out.println(response);out.flush();}
    catch(RuntimeFailure failure){var turns=session.turns();error.println(SafeErrorMapper.render(failure,turns.isEmpty()?null:turns.getLast().id()));error.flush();result=1;}
   }
  }catch(IOException e){error.println(SafeErrorMapper.message(RuntimeFailure.Code.CONTEXT_READ));error.flush();return 1;}
  return result;
 }
}

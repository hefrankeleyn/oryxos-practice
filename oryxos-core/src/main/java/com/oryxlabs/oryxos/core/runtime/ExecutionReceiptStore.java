package com.oryxlabs.oryxos.core.runtime;

import com.oryxlabs.oryxos.core.audit.AuditPersistenceException;
import com.oryxlabs.oryxos.core.tool.ToolResult;
import java.util.*;
/** 原子认领；进行中或结果未知的请求永不重新认领，不提供跨重启 exactly-once。 */
public final class ExecutionReceiptStore {
 private final Map<ToolRequestIdentity,ExecutionReceipt> receipts=new HashMap<>();
 public synchronized ExecutionReceipt claim(ToolRequestIdentity key){
  var existing=receipts.get(key);if(existing!=null){if(existing.state()!=ExecutionReceipt.State.COMPLETED)throw new AuditPersistenceException();return existing;}
  var receipt=new ExecutionReceipt();receipt.claim();receipts.put(key,receipt);return receipt;
 }
 public synchronized void complete(ToolRequestIdentity key,ToolResult result){var receipt=receipts.get(key);if(receipt==null||receipt.state()!=ExecutionReceipt.State.CLAIMED)throw new AuditPersistenceException();receipt.complete(result);}
}


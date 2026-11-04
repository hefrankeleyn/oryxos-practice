package com.oryxlabs.oryxos.storage;

import com.oryxlabs.oryxos.core.audit.*;
import com.oryxlabs.oryxos.core.runtime.SafeErrorMapper;
import jakarta.persistence.*;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/** 每次 begin/complete 使用独立短事务；远程操作必须位于这两个已提交事务之外。 */
public final class JpaAuditStore implements AuditStore {
    private static final Logger log=LoggerFactory.getLogger(JpaAuditStore.class);
    private final EntityManagerFactory factory;
    private final TransactionTemplate transaction;
    public JpaAuditStore(EntityManagerFactory factory,PlatformTransactionManager manager){
        this.factory=factory;transaction=new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    @Override public void begin(InvocationAudit a) {
        commit(()->{
            AuditEntity row;
            if(a.kind()==InvocationAudit.Kind.LLM){
                var llm=new LlmCallEntity();llm.setProvider(a.provider());llm.setModel(a.model());row=llm;
            }else{
                var tool=new ToolInvocationEntity();tool.setLlmCallId(a.llmCallId().toString());tool.setToolCallId(a.toolCallId());tool.setToolName(a.toolName());row=tool;
            }
            row.setId(a.id().toString());row.setSessionId(a.sessionId().toString());row.setProfileName(a.profileName());
            row.setRunId(a.runId().toString());row.setIteration(a.iteration());row.setStatus("PENDING");row.setStartedAt(a.startedAt().toString());
            entityManager().persist(row);
        });
    }
    @Override public void complete(InvocationAudit a,InvocationAudit.Completion c) {
        commit(()->{
            boolean llm=a.kind()==InvocationAudit.Kind.LLM;
            if(llm&&c.status()==InvocationAudit.Status.REJECTED)throw new AuditPersistenceException();
            String fields=llm?", a.promptTokens=:prompt, a.completionTokens=:completion, a.totalTokens=:total":", a.executed=:executed";
            // 条件更新将 PENDING 前置检查与写入合为一个数据库动作，防止并发覆盖终态。
            var query=entityManager().createQuery("update "+(llm?"LlmCallEntity":"ToolInvocationEntity")+
                " a set a.status=:status, a.success=:success, a.completedAt=:completed, a.durationMs=:duration, a.errorCode=:code, a.errorMessage=:message"+fields+
                " where a.id=:id and a.status='PENDING'");
            query.setParameter("status",c.status().name()).setParameter("success",c.status()==InvocationAudit.Status.SUCCEEDED)
                .setParameter("completed",c.completedAt().toString()).setParameter("duration",c.durationMs())
                .setParameter("code",c.error()==null?null:c.error().name()).setParameter("message",c.error()==null?null:SafeErrorMapper.message(c.error()))
                .setParameter("id",a.id().toString());
            if(llm)query.setParameter("prompt",c.promptTokens()).setParameter("completion",c.completionTokens()).setParameter("total",c.totalTokens());
            else query.setParameter("executed",c.executed());
            if(query.executeUpdate()!=1)throw new AuditPersistenceException();
        });
    }
    private EntityManager entityManager(){return java.util.Objects.requireNonNull(EntityManagerFactoryUtils.getTransactionalEntityManager(factory));}
    /** execute 返回在 commit 之后；提交异常不携带原始 SQL/cause，不假定回滚了外部操作。 */
    private void commit(Runnable action){
        try { transaction.executeWithoutResult(status->{action.run();entityManager().flush();}); }
        catch(RuntimeException failure){log.error("审计事务未确认提交");throw new AuditPersistenceException();}
    }
}

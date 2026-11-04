package com.oryxlabs.oryxos.core.model;

import java.nio.file.Path;
import java.util.List;
import com.oryxlabs.oryxos.core.runtime.RuntimeFailure;
import static com.oryxlabs.oryxos.core.runtime.RuntimeFailure.Code.CONFIGURATION;
/** 启动时固定运行配置；正文与 Bootstrap 内容不在快照中，避免配置意外热更新。 */
public record ProfileSnapshot(String name, String providerName, String model, Double temperature,
        List<String> tools, List<Path> bootstrap, int maxIterations, int maxHistoryTurns, String identityPrompt) {
    public ProfileSnapshot {
        if (name==null || !name.matches("[\\p{L}\\p{N}_-]+") || providerName==null || providerName.isBlank() || model==null || model.isBlank()
                || maxIterations<1 || maxHistoryTurns<1 || temperature!=null && !Double.isFinite(temperature))
            throw new RuntimeFailure(CONFIGURATION,"agent");
        tools=List.copyOf(tools); bootstrap=List.copyOf(bootstrap);
        if(tools.stream().distinct().count()!=tools.size()) throw new RuntimeFailure(CONFIGURATION,"agent.tools");
        identityPrompt=identityPrompt==null ? "" : identityPrompt;
    }
    @Override public String toString() { return "ProfileSnapshot[redacted]"; }
}

CREATE TABLE IF NOT EXISTS llm_calls (
    id TEXT PRIMARY KEY NOT NULL,
    session_id TEXT NOT NULL, profile_name TEXT NOT NULL CHECK(length(profile_name)>0), run_id TEXT NOT NULL,
    iteration INTEGER NOT NULL CHECK(iteration>=1),
    status TEXT NOT NULL CHECK(status IN ('PENDING','SUCCEEDED','FAILED')),
    success INTEGER CHECK(success IN (0,1)),
    started_at TEXT NOT NULL, completed_at TEXT,
    duration_ms INTEGER CHECK(duration_ms>=0), error_code TEXT, error_message TEXT,
    provider TEXT NOT NULL CHECK(length(provider)>0), model TEXT NOT NULL CHECK(length(model)>0),
    prompt_tokens INTEGER CHECK(prompt_tokens>=0), completion_tokens INTEGER CHECK(completion_tokens>=0), total_tokens INTEGER CHECK(total_tokens>=0),
    CHECK((status='PENDING' AND success IS NULL AND completed_at IS NULL AND duration_ms IS NULL AND error_code IS NULL AND error_message IS NULL)
       OR (status='SUCCEEDED' AND success IS NOT NULL AND success=1 AND completed_at IS NOT NULL AND duration_ms IS NOT NULL AND error_code IS NULL AND error_message IS NULL)
       OR (status IN ('FAILED','REJECTED') AND success IS NOT NULL AND success=0 AND completed_at IS NOT NULL AND duration_ms IS NOT NULL AND error_code IS NOT NULL AND error_message IS NOT NULL))
);
CREATE TABLE IF NOT EXISTS tool_invocations (
    id TEXT PRIMARY KEY NOT NULL,
    session_id TEXT NOT NULL, profile_name TEXT NOT NULL CHECK(length(profile_name)>0), run_id TEXT NOT NULL,
    iteration INTEGER NOT NULL CHECK(iteration>=1),
    status TEXT NOT NULL CHECK(status IN ('PENDING','SUCCEEDED','FAILED','REJECTED')),
    success INTEGER CHECK(success IN (0,1)),
    started_at TEXT NOT NULL, completed_at TEXT,
    duration_ms INTEGER CHECK(duration_ms>=0), error_code TEXT, error_message TEXT,
    llm_call_id TEXT NOT NULL, tool_call_id TEXT NOT NULL CHECK(length(tool_call_id)>0), tool_name TEXT NOT NULL CHECK(length(tool_name)>0),
    executed INTEGER CHECK(executed IN (0,1)),
    UNIQUE(run_id,llm_call_id,tool_call_id),
    CHECK((status='PENDING' AND executed IS NULL) OR (status IN ('SUCCEEDED','FAILED') AND executed IS NOT NULL AND executed=1) OR (status='REJECTED' AND executed IS NOT NULL AND executed=0)),
    CHECK((status='PENDING' AND success IS NULL AND completed_at IS NULL AND duration_ms IS NULL AND error_code IS NULL AND error_message IS NULL)
       OR (status='SUCCEEDED' AND success IS NOT NULL AND success=1 AND completed_at IS NOT NULL AND duration_ms IS NOT NULL AND error_code IS NULL AND error_message IS NULL)
       OR (status IN ('FAILED','REJECTED') AND success IS NOT NULL AND success=0 AND completed_at IS NOT NULL AND duration_ms IS NOT NULL AND error_code IS NOT NULL AND error_message IS NOT NULL))
);

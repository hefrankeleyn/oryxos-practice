/**
 * 核心能力五：Web Service（REST API，前缀 {@code /api/v1}）。
 *
 * <p>职责：
 * <ul>
 * <li>六个 Controller：Session、Agent、Profile、Memory、Tool、System；只做参数校验、响应包装、错误处理，业务委托核心层；</li>
 * <li>{@code GlobalExceptionHandler}：统一 {@code ApiResponse} 信封（code / message / data / timestamp）；</li>
 * <li>约束：单条消息 ≤ 32KB、历史最多返回 100 条、Agent 调用 60 秒超时返回 504、Provider 故障返回 503。</li>
 * </ul>
 */
package com.oryxlabs.oryxos.web;

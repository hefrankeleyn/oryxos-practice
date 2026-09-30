/**
 * 命令行入口：Picocli 12 个子命令与 {@code ConfigLoader}。
 *
 * <p>命令：{@code init}、{@code status}、{@code chat}、{@code serve}、{@code gateway}、
 * {@code profile list|create|show|delete}、{@code provider list}、{@code tool list}、{@code session list}。
 *
 * <p>约束：不需要 Spring 上下文的命令（如 init、profile list）直接操作文件以保证启动速度；
 * {@code ConfigLoader} 解析 {@code ${ENV_VAR}} 占位符，密钥不落地。
 */
package com.oryxlabs.oryxos.cli;

/**
 * CLI Channel：{@code oryxos chat} 的交互实现。
 *
 * <p>职责：{@code CliChannel} 读取 stdin、写 stdout，维护当前 Session，每条输入调用
 * {@code AgentService.process}，支持 {@code /quit} 退出。
 */
package com.oryxlabs.oryxos.channel.cli;

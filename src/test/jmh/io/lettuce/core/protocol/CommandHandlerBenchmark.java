/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.protocol;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.output.ValueOutput;

/**
 * Benchmark for {@link CommandHandler}. Test cases:
 * <ul>
 * <li>netty (in-eventloop) writes</li>
 * </ul>
 *
 * @author Mark Paluch
 */
@State(Scope.Benchmark)
public class CommandHandlerBenchmark {

    private static final ByteArrayCodec CODEC = new ByteArrayCodec();
    private static final ClientOptions CLIENT_OPTIONS = ClientOptions.create();
    private static final EmptyContext CHANNEL_HANDLER_CONTEXT = new EmptyContext();
    private static final byte[] KEY = "key".getBytes();
    private final EmptyPromise PROMISE = new EmptyPromise();

    private CommandHandler commandHandler;
    private Command command;

    @Setup
    public void setup() {

        commandHandler = new CommandHandler(CLIENT_OPTIONS, EmptyClientResources.INSTANCE, new DefaultEndpoint(CLIENT_OPTIONS));
        command = new Command(CommandType.GET, new ValueOutput<>(CODEC), new CommandArgs(CODEC).addKey(KEY));

        commandHandler.setState(CommandHandler.LifecycleState.CONNECTED);
    }

    @Benchmark
    public void measureNettyWrite() throws Exception {

        commandHandler.write(CHANNEL_HANDLER_CONTEXT, command, PROMISE);

        // Prevent OOME
        commandHandler.getStack().clear();
    }
}

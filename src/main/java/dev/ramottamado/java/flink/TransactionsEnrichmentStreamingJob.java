/*
 * Copyright 2021 Tamado Sitohang <ramot@ramottamado.dev>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.ramottamado.java.flink;

import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import dev.ramottamado.java.flink.functions.EnrichEnrichedTransactionsWithCustomersJoinFunction;
import dev.ramottamado.java.flink.functions.EnrichTransactionsWithCustomersJoinFunction;
import dev.ramottamado.java.flink.schema.CustomersBean;
import dev.ramottamado.java.flink.schema.EnrichedTransactionsBean;
import dev.ramottamado.java.flink.schema.TransactionsBean;

/**
 * The abstract class {@link TransactionsEnrichmentStreamingJob} provides base class, logic and pipeline for enriching
 * {@link TransactionsBean} data using Flink. The core pipeline and functionality is encapsulated here, while subclasses
 * have to implement input and output methods. Check {@link KafkaTransactionsEnrichmentStreamingJob} for the
 * implementation using data stream from Kafka.
 *
 * @see KafkaTransactionsEnrichmentStreamingJob
 */
public abstract class TransactionsEnrichmentStreamingJob {
    protected static ParameterTool params;
    protected StreamExecutionEnvironment env;

    /**
     * Method to get {@link TransactionsBean} data stream.
     *
     * @return                  the {@link TransactionsBean} data stream
     * @throws RuntimeException if input cannot be read.
     */
    protected abstract DataStream<TransactionsBean> readTransactionsCdcStream() throws RuntimeException;

    /**
     * Method to get {@link CustomersBean} data stream.
     *
     * @return                  the {@link CustomersBean} data stream
     * @throws RuntimeException if input cannot be read.
     */
    protected abstract DataStream<CustomersBean> readCustomersCdcStream() throws RuntimeException;

    /**
     * Method to write {@link EnrichedTransactionsBean} data stream to sink.
     *
     * @param  enrichedTrxStream the {@link EnrichedTransactionsBean} data stream
     * @throws RuntimeException  if output cannot be written
     */
    protected abstract void writeEnrichedTransactionsOutput(DataStream<EnrichedTransactionsBean> enrichedTrxStream)
            throws RuntimeException;

    /**
     * Method to get {@link StreamExecutionEnvironment}.
     *
     * @return                  the {@link StreamExecutionEnvironment} to run the pipeline
     * @throws RuntimeException if something wrong happened
     */
    protected abstract StreamExecutionEnvironment createExecutionEnvironment()
            throws RuntimeException;

    /**
     * The core logic and pipeline for enriching {@link TransactionsBean} data stream using data from
     * {@link CustomersBean}.
     *
     * @return                  the Flink {@link StreamExecutionEnvironment} environment
     * @throws RuntimeException if input/output cannot be read/write
     */
    public final StreamExecutionEnvironment createApplicationPipeline() throws RuntimeException {
        env = createExecutionEnvironment();

        KeyedStream<CustomersBean, String> keyedCustomersCdcStream = readCustomersCdcStream()
                .keyBy(CustomersBean::getAcctNumber);

        KeyedStream<TransactionsBean, String> keyedTransactionsStream = readTransactionsCdcStream()
                .keyBy(TransactionsBean::getSrcAcct);

        KeyedStream<EnrichedTransactionsBean, String> enrichedTrxStream = keyedTransactionsStream
                .connect(keyedCustomersCdcStream)
                .process(new EnrichTransactionsWithCustomersJoinFunction())
                .uid("enriched_transactions")
                .keyBy(EnrichedTransactionsBean::getDestAcctAsKey);

        SingleOutputStreamOperator<EnrichedTransactionsBean> enrichedTrxStream2 = enrichedTrxStream
                .connect(keyedCustomersCdcStream)
                .process(new EnrichEnrichedTransactionsWithCustomersJoinFunction())
                .uid("enriched_transactions_2");

        writeEnrichedTransactionsOutput(enrichedTrxStream2);

        return env;
    }
}

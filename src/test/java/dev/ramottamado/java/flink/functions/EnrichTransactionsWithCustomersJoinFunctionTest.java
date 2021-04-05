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

package dev.ramottamado.java.flink.functions;

import java.time.Instant;

import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.operators.co.KeyedCoProcessOperator;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.util.KeyedTwoInputStreamOperatorTestHarness;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import dev.ramottamado.java.flink.schema.CustomersBean;
import dev.ramottamado.java.flink.schema.EnrichedTransactionsBean;
import dev.ramottamado.java.flink.schema.TransactionsBean;

public class EnrichTransactionsWithCustomersJoinFunctionTest {
    private EnrichTransactionsWithCustomersJoinFunction enrichTransactionsWithCustomersJoinFunction;
    private KeyedTwoInputStreamOperatorTestHarness<String, TransactionsBean, CustomersBean, EnrichedTransactionsBean> testHarness;
    private CustomersBean testCustomer;
    private TransactionsBean testTrx;
    private EnrichedTransactionsBean testEnrichedTrx;

    @Before
    public void prepareTest() {
        testCustomer = new CustomersBean();
        testCustomer.setAcctNumber("0001");
        testCustomer.setCif("001");
        testCustomer.setCity("Bandung");
        testCustomer.setFirstName("Tamado");
        testCustomer.setLastName("Sitohang");

        testTrx = new TransactionsBean();
        testTrx.setAmount(10000.0);
        testTrx.setDestAcct("0002");
        testTrx.setSrcAcct("0001");
        testTrx.setTrxTimestamp(Instant.parse("2021-01-01T12:00:00.00Z"));
        testTrx.setTrxType("TRANSFER");

        testEnrichedTrx = new EnrichedTransactionsBean();
        testEnrichedTrx.setAmount(10000.0);
        testEnrichedTrx.setDestAcct("0002");
        testEnrichedTrx.setSrcAcct("0001");
        testEnrichedTrx.setTrxTimestamp(Instant.parse("2021-01-01T12:00:00.00Z"));
        testEnrichedTrx.setTrxType("TRANSFER");
        testEnrichedTrx.setCif("001");
        testEnrichedTrx.setSrcName("Tamado Sitohang");
    }

    @Test
    public void testProcessElement() throws Exception {
        enrichTransactionsWithCustomersJoinFunction = new EnrichTransactionsWithCustomersJoinFunction();

        testHarness = new KeyedTwoInputStreamOperatorTestHarness<>(
                new KeyedCoProcessOperator<>(enrichTransactionsWithCustomersJoinFunction),
                TransactionsBean::getSrcAcct,
                CustomersBean::getAcctNumber,
                Types.STRING);

        testHarness.open();
        testHarness.processElement2(testCustomer, 10);
        testHarness.processElement1(testTrx, 10);

        for (StreamRecord<? extends EnrichedTransactionsBean> etx : testHarness.extractOutputStreamRecords()) {
            Assert.assertEquals(testEnrichedTrx.getCif(), etx.getValue().getCif());
            Assert.assertEquals(testEnrichedTrx.getDestAcct(), etx.getValue().getDestAcct());
            Assert.assertEquals(testEnrichedTrx.getSrcAcct(), etx.getValue().getSrcAcct());
            Assert.assertEquals(testEnrichedTrx.getSrcName(), etx.getValue().getSrcName());
            Assert.assertEquals(testEnrichedTrx.getTrxType(), etx.getValue().getTrxType());
            Assert.assertEquals(testEnrichedTrx.getAmount(), etx.getValue().getAmount());
        }
    }

    @Test
    public void testOnTimer() throws Exception {
        enrichTransactionsWithCustomersJoinFunction = new EnrichTransactionsWithCustomersJoinFunction();

        testHarness = new KeyedTwoInputStreamOperatorTestHarness<>(
                new KeyedCoProcessOperator<>(enrichTransactionsWithCustomersJoinFunction),
                TransactionsBean::getSrcAcct,
                CustomersBean::getAcctNumber,
                Types.STRING);

        testHarness.open();
        testHarness.processElement1(testTrx, 10);
        testHarness.processElement2(testCustomer, 10);
        testHarness.setProcessingTime(5011);

        for (StreamRecord<? extends EnrichedTransactionsBean> etx : testHarness.extractOutputStreamRecords()) {
            Assert.assertEquals(testEnrichedTrx.getCif(), etx.getValue().getCif());
            Assert.assertEquals(testEnrichedTrx.getDestAcct(), etx.getValue().getDestAcct());
            Assert.assertEquals(testEnrichedTrx.getSrcAcct(), etx.getValue().getSrcAcct());
            Assert.assertEquals(testEnrichedTrx.getSrcName(), etx.getValue().getSrcName());
            Assert.assertEquals(testEnrichedTrx.getTrxType(), etx.getValue().getTrxType());
            Assert.assertEquals(testEnrichedTrx.getAmount(), etx.getValue().getAmount());
        }
    }

    @Test
    public void testOnTimerWithNullCustomer() throws Exception {
        enrichTransactionsWithCustomersJoinFunction = new EnrichTransactionsWithCustomersJoinFunction();

        testHarness = new KeyedTwoInputStreamOperatorTestHarness<>(
                new KeyedCoProcessOperator<>(enrichTransactionsWithCustomersJoinFunction),
                TransactionsBean::getSrcAcct,
                CustomersBean::getAcctNumber,
                Types.STRING);

        testHarness.open();
        testHarness.processElement1(testTrx, 10);
        testHarness.setProcessingTime(5011);

        for (StreamRecord<? extends EnrichedTransactionsBean> etx : testHarness.extractOutputStreamRecords()) {
            Assert.assertEquals(null, etx.getValue().getCif());
            Assert.assertEquals(testEnrichedTrx.getDestAcct(), etx.getValue().getDestAcct());
            Assert.assertEquals(testEnrichedTrx.getSrcAcct(), etx.getValue().getSrcAcct());
            Assert.assertEquals(null, etx.getValue().getSrcName());
            Assert.assertEquals(testEnrichedTrx.getTrxType(), etx.getValue().getTrxType());
            Assert.assertEquals(testEnrichedTrx.getAmount(), etx.getValue().getAmount());
        }
    }
}

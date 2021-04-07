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

package dev.ramottamado.java.flink.functions.helper;

import org.apache.flink.streaming.api.functions.AssignerWithPunctuatedWatermarks;
import org.apache.flink.streaming.api.watermark.Watermark;

public class TestTimestampAssigner<T> implements AssignerWithPunctuatedWatermarks<T> {
    private static final long serialVersionUID = 5737593418503255204L;

    @Override
    public long extractTimestamp(T element, long previousElementTimestamp) {
        return System.currentTimeMillis();
    }

    @Override
    public Watermark checkAndGetNextWatermark(T lastElement, long extractedTimestamp) {
        return new Watermark(extractedTimestamp);
    }
}

/*
 * Copyright 2010-2016 OrientDB LTD (http://orientdb.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orientechnologies.orient.test.database.auto;

import com.orientechnologies.orient.core.record.impl.ODocument;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Measurement(iterations = 5, batchSize = 1)
@Warmup(iterations = 5, batchSize = 1)
@Fork(1)
public class JSONTestBenchmark extends DocumentDBBaseTest {
  public static void main(String[] args) throws RunnerException, IOException {
    final Options opt =
        new OptionsBuilder()
            .include("JSONTestBenchmark.*")
            // .addProfiler(StackProfiler.class, "detailLine=true;excludePackages=true;period=1")
            // .addProfiler(GCProfiler.class)
            .jvmArgs("-server", "-XX:+UseConcMarkSweepGC", "-Xmx4G", "-Xms1G")
            // .result("tests" + "/" + "target" + "/" + "results.csv")
            // .param("offHeapMessages", "true""
            // .resultFormat(ResultFormatType.CSV)
            .build();
    return;
  }

  @Benchmark
  public void testAlmostLink() {
    final ODocument doc = new ODocument();
    doc.fromJSON("{'title': '#330: Dollar Coins Are Done'}");
  }

  @Benchmark
  public void testAlmostLinkStream() throws IOException {
    final ODocument doc = new ODocument();
    doc.fromJSON(
        new ByteArrayInputStream(
            "{'title': '#330: Dollar Coins Are Done'}".getBytes(StandardCharsets.UTF_8)));
  }

  @Benchmark
  public void testNullList() {
    final ODocument documentSource = new ODocument();
    documentSource.fromJSON("{\"list\" : [\"string\", null]}");
  }

  @Benchmark
  public void testNullListStream() throws Exception {
    final ODocument documentSource = new ODocument();
    documentSource.fromJSON(
        new ByteArrayInputStream(
            "{\"list\" : [\"string\", null]}".getBytes(StandardCharsets.UTF_8)));
  }

  @Benchmark
  public void testBooleanList() {
    final ODocument documentSource = new ODocument();
    documentSource.fromJSON("{\"list\" : [true, false]}");
  }

  @Benchmark
  public void testBooleanListStream() throws IOException {
    final ODocument documentSource = new ODocument();
    documentSource.fromJSON(
        new ByteArrayInputStream("{\"list\" : [true, false]}".getBytes(StandardCharsets.UTF_8)));
  }

  @Benchmark
  public void testNumericIntegerList() {
    final ODocument documentSource = new ODocument();
    documentSource.fromJSON("{\"list\" : [17,42]}");
  }

  @Benchmark
  public void testNumericIntegerListStream() throws IOException {
    final ODocument documentSource = new ODocument();
    documentSource.fromJSON(
        new ByteArrayInputStream("{\"list\" : [17,42]}".getBytes(StandardCharsets.UTF_8)));
  }

  @Benchmark
  public void testNumericLongList() {
    final ODocument documentSource = new ODocument();
    documentSource.fromJSON("{\"list\" : [100000000000,100000000001]}");
  }

  @Benchmark
  public void testNumericLongListStream() throws IOException {
    final ODocument documentSource = new ODocument();
    documentSource.fromJSON(
        new ByteArrayInputStream(
            "{\"list\" : [100000000000,100000000001]}".getBytes(StandardCharsets.UTF_8)));
  }

  @Benchmark
  public void testNumericFloatList() {
    final ODocument documentSource = new ODocument();
    documentSource.fromJSON("{\"list\" : [17.3,42.7]}");
  }

  @Benchmark
  public void testNumericFloatListStream() throws IOException {
    final ODocument documentSource = new ODocument();
    documentSource.fromJSON(
        new ByteArrayInputStream("{\"list\" : [17.3,42.7]}".getBytes(StandardCharsets.UTF_8)));
  }

  @Benchmark
  public void testNullity() {
    final ODocument doc = new ODocument();
    doc.fromJSON(
        "{\"gender\":{\"name\":\"Male\"},\"firstName\":\"Jack\",\"lastName\":\"Williams\",\"phone\":\"561-401-3348\",\"email\":\"0586548571@example.com\",\"address\":{\"street1\":\"Smith"
            + " Ave\","
            + "\"street2\":null,\"city\":\"GORDONSVILLE\",\"state\":\"VA\",\"code\":\"22942\"},\"dob\":\"2011-11-17"
            + " 03:17:04\"}");
  }

  @Benchmark
  public void testNullityStream() throws IOException {
    final ODocument doc = new ODocument();
    final String json =
        "{\"gender\":{\"name\":\"Male\"},\"firstName\":\"Jack\",\"lastName\":\"Williams\",\"phone\":\"561-401-3348\",\"email\":\"0586548571@example.com\",\"address\":{\"street1\":\"Smith"
            + " Ave\","
            + "\"street2\":null,\"city\":\"GORDONSVILLE\",\"state\":\"VA\",\"code\":\"22942\"},\"dob\":\"2011-11-17"
            + " 03:17:04\"}";
    doc.fromJSON(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
  }
}

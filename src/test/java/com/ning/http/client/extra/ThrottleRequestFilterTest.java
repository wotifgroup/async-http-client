/*
 * Copyright 2010 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.ning.http.client.extra;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Response;
import com.ning.http.client.async.AbstractBasicTest;
import com.ning.http.client.async.ProviderUtil;

/**
 */
public class ThrottleRequestFilterTest extends AbstractBasicTest {

    @Test
    public void testMaxThrottledConnections() throws InterruptedException {
        final AsyncHttpClient client = getAsyncHttpClient(
                new AsyncHttpClientConfig.Builder()
                        .setConnectionTimeoutInMs(100)
                        .setRequestTimeoutInMs(100)
                        .setAllowPoolingConnection(false)
                        .setMaximumConnectionsTotal(1)
                        .setMaximumConnectionsPerHost(1)
                        .addRequestFilter(new ThrottleRequestFilter(1))
                        .build()
                );

        ExecutorService pool = Executors.newFixedThreadPool(2);

        List<Future> results = new ArrayList<Future>();

        for (int i = 0; i < 10; i++) {
            results.add(pool.submit(new Callable<Response>() {
                @Override
                public Response call() throws Exception {
                    return client.prepareGet("http://127.0.0.1:" + port1 + "/json").addHeader("LockThread", "100").execute()
                            .get();
                }
            }));
        }

        pool.shutdown();
        pool.awaitTermination(1000, TimeUnit.SECONDS);

        for (Future<Response> result : results) {
            try {
                result.get();
            } catch (ExecutionException e) {
                if (e.getCause() instanceof IOException) {
                    Assert.fail("Should not hit max connection limit", e.getCause());
                }
            }
        }

        client.close();
    }

    @Override
    public AsyncHttpClient getAsyncHttpClient(AsyncHttpClientConfig config) {
        return ProviderUtil.nettyProvider(config);
    }

}

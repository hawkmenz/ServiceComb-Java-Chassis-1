/*
 * Copyright 2017 Huawei Technologies Co., Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.servicecomb.core.provider.consumer;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import io.servicecomb.core.CseContext;
import io.servicecomb.core.Invocation;
import io.servicecomb.core.definition.OperationMeta;
import io.servicecomb.core.definition.SchemaMeta;
import io.servicecomb.core.invocation.InvocationFactory;
import io.servicecomb.swagger.invocation.AsyncResponse;
import io.servicecomb.swagger.invocation.Response;
import io.servicecomb.swagger.invocation.exception.InvocationException;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mock;
import mockit.MockUp;

public class TestInvokerUtils {

  @Test
  public void testSyncInvokeInvocationWithException() throws InterruptedException {
    Invocation invocation = Mockito.mock(Invocation.class);

    Response response = Mockito.mock(Response.class);
    new MockUp<SyncResponseExecutor>() {
      @Mock
      public Response waitResponse() throws InterruptedException {
        return Mockito.mock(Response.class);
      }
    };
    Mockito.when(response.isSuccessed()).thenReturn(true);
    OperationMeta operationMeta = Mockito.mock(OperationMeta.class);
    Mockito.when(invocation.getOperationMeta()).thenReturn(operationMeta);
    Mockito.when(operationMeta.getMicroserviceQualifiedName()).thenReturn("test");

    try {
      InvokerUtils.syncInvoke(invocation);
    } catch (InvocationException e) {
      Assert.assertEquals(490, e.getStatusCode());
    }
  }

  @Test
  public void testReactiveInvoke() {
    Invocation invocation = Mockito.mock(Invocation.class);
    AsyncResponse asyncResp = Mockito.mock(AsyncResponse.class);
    boolean validAssert;
    try {
      InvokerUtils.reactiveInvoke(invocation, asyncResp);
      validAssert = true;
    } catch (Exception e) {
      validAssert = false;
    }
    Assert.assertTrue(validAssert);
  }

  @Test
  public void testInvokeWithException() {
    new MockUp<SyncResponseExecutor>() {
      @Mock
      public Response waitResponse() throws InterruptedException {
        return Mockito.mock(Response.class);
      }
    };
    Invocation invocation = Mockito.mock(Invocation.class);
    OperationMeta operationMeta = Mockito.mock(OperationMeta.class);
    Mockito.when(invocation.getOperationMeta()).thenReturn(operationMeta);
    Mockito.when(operationMeta.isSync()).thenReturn(true);
    try {
      InvokerUtils.invoke(invocation);
    } catch (InvocationException e) {
      Assert.assertEquals(490, e.getStatusCode());
    }
  }

  @Test
  public void testInvoke() {
    Object[] objectArray = new Object[2];
    Invocation invocation = Mockito.mock(Invocation.class);
    OperationMeta operationMeta = Mockito.mock(OperationMeta.class);
    Mockito.when(invocation.getOperationMeta()).thenReturn(operationMeta);
    Mockito.when(operationMeta.isSync()).thenReturn(false);
    Mockito.when(invocation.getArgs()).thenReturn(objectArray);
    Object obj = InvokerUtils.invoke(invocation);
    Assert.assertNull(obj);
  }

  @Test
  public void tetSyncInvokeNotReady() {
    ReferenceConfigUtils.setReady(false);

    try {
      InvokerUtils.syncInvoke("ms", "schemaId", "opName", null);
      Assert.fail("must throw exception");
    } catch (IllegalStateException e) {
      Assert.assertEquals("System is not ready for remote calls. "
          + "When beans are making remote calls in initialization, it's better to "
          + "implement io.servicecomb.core.BootListener and do it after EventType.AFTER_REGISTRY.",
          e.getMessage());
    }

    try {
      InvokerUtils.syncInvoke("ms", "latest", "rest", "schemaId", "opName", null);
      Assert.fail("must throw exception");
    } catch (IllegalStateException e) {
      Assert.assertEquals("System is not ready for remote calls. "
          + "When beans are making remote calls in initialization, it's better to "
          + "implement io.servicecomb.core.BootListener and do it after EventType.AFTER_REGISTRY.",
          e.getMessage());
    }
  }

  @Test
  public void tetSyncInvokeReady(@Injectable ConsumerProviderManager consumerProviderManager,
      @Injectable Invocation invocation) {
    ReferenceConfigUtils.setReady(true);
    CseContext.getInstance().setConsumerProviderManager(consumerProviderManager);

    new Expectations(InvocationFactory.class) {
      {
        InvocationFactory.forConsumer((ReferenceConfig) any, (SchemaMeta) any, (String) any, (Object[]) any);
        result = invocation;
      }
    };
    new Expectations(InvokerUtils.class) {
      {
        InvokerUtils.syncInvoke(invocation);
        result = "ok";
      }
    };
    Object result1 = InvokerUtils.syncInvoke("ms", "schemaId", "opName", null);
    Assert.assertEquals("ok", result1);

    Object result2 = InvokerUtils.syncInvoke("ms", "latest", "rest", "schemaId", "opName", null);
    Assert.assertEquals("ok", result2);

    CseContext.getInstance().setConsumerProviderManager(null);
  }
}

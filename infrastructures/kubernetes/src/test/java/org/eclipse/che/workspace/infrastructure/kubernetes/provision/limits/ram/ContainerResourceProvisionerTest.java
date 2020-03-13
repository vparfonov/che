/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.workspace.infrastructure.kubernetes.provision.limits.ram;

import static com.google.common.collect.ImmutableMap.of;
import static org.eclipse.che.api.core.model.workspace.config.MachineConfig.CPU_LIMIT_ATTRIBUTE;
import static org.eclipse.che.api.core.model.workspace.config.MachineConfig.CPU_REQUEST_ATTRIBUTE;
import static org.eclipse.che.api.core.model.workspace.config.MachineConfig.MEMORY_LIMIT_ATTRIBUTE;
import static org.eclipse.che.api.core.model.workspace.config.MachineConfig.MEMORY_REQUEST_ATTRIBUTE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import java.util.Collections;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.workspace.server.spi.environment.InternalMachineConfig;
import org.eclipse.che.api.workspace.server.spi.environment.ResourceLimitAttributesProvisioner;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment;
import org.eclipse.che.workspace.infrastructure.kubernetes.environment.KubernetesEnvironment.PodData;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * Tests {@link ContainerResourceProvisioner}.
 *
 * @author Anton Korneta
 */
@Listeners(MockitoTestNGListener.class)
public class ContainerResourceProvisionerTest {

  private static final String POD_NAME = "web";
  private static final String CONTAINER_NAME = "app";
  private static final String MACHINE_NAME = POD_NAME + '/' + CONTAINER_NAME;
  private static final String RAM_LIMIT_VALUE = "2147483648";
  private static final String RAM_REQUEST_VALUE = "1234567890";
  private static final String CPU_LIMIT_VALUE = "0.4";
  private static final String CPU_REQUEST_VALUE = "0.15";

  @Mock private KubernetesEnvironment k8sEnv;
  @Mock private RuntimeIdentity identity;
  @Mock private InternalMachineConfig internalMachineConfig;
  @Mock private ResourceLimitAttributesProvisioner resourceLimitAttributesProvisioner;

  private Container container;
  private ContainerResourceProvisioner resourceProvisioner;

  @BeforeMethod
  public void setup() {
    resourceProvisioner =
        new ContainerResourceProvisioner(
            1024, 512, "500m", "100m", resourceLimitAttributesProvisioner);
    container = new Container();
    container.setName(CONTAINER_NAME);
    when(k8sEnv.getMachines()).thenReturn(of(MACHINE_NAME, internalMachineConfig));
    when(internalMachineConfig.getAttributes())
        .thenReturn(
            of(
                MEMORY_LIMIT_ATTRIBUTE,
                RAM_LIMIT_VALUE,
                MEMORY_REQUEST_ATTRIBUTE,
                RAM_REQUEST_VALUE,
                CPU_LIMIT_ATTRIBUTE,
                CPU_LIMIT_VALUE,
                CPU_REQUEST_ATTRIBUTE,
                CPU_REQUEST_VALUE));
    final ObjectMeta podMetadata = mock(ObjectMeta.class);
    when(podMetadata.getName()).thenReturn(POD_NAME);
    final PodSpec podSpec = mock(PodSpec.class);
    when(podSpec.getContainers()).thenReturn(Collections.singletonList(container));
    when(k8sEnv.getPodsData()).thenReturn(of(POD_NAME, new PodData(podSpec, podMetadata)));
  }

  @Test
  public void testProvisionResourcesLimitAndRequestAttributeToContainer() throws Exception {
    resourceProvisioner.provision(k8sEnv, identity);
    assertEquals(container.getResources().getLimits().get("memory").getAmount(), RAM_LIMIT_VALUE);
    assertEquals(container.getResources().getLimits().get("cpu").getAmount(), CPU_LIMIT_VALUE);
    assertEquals(
        container.getResources().getRequests().get("memory").getAmount(), RAM_REQUEST_VALUE);
    assertEquals(container.getResources().getRequests().get("cpu").getAmount(), CPU_REQUEST_VALUE);
  }

  @Test
  public void testOverridesContainerRamLimitAndRequestFromMachineAttribute() throws Exception {
    ResourceRequirements resourceRequirements =
        new ResourceRequirementsBuilder()
            .addToLimits(of("memory", new Quantity("3221225472"), "cpu", new Quantity("0.678")))
            .addToRequests(of("memory", new Quantity("1231231423"), "cpu", new Quantity("0.333")))
            .build();
    container.setResources(resourceRequirements);

    resourceProvisioner.provision(k8sEnv, identity);

    assertEquals(container.getResources().getLimits().get("memory").getAmount(), RAM_LIMIT_VALUE);
    assertEquals(container.getResources().getLimits().get("cpu").getAmount(), CPU_LIMIT_VALUE);
    assertEquals(
        container.getResources().getRequests().get("memory").getAmount(), RAM_REQUEST_VALUE);
    assertEquals(container.getResources().getRequests().get("cpu").getAmount(), CPU_REQUEST_VALUE);
  }
}

/*
 * Copyright (c) 2012-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.multiuser.machine.authentication.server;

import com.google.inject.Inject;
import java.util.Base64;
import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.server.spi.provision.env.EnvVarProvider;
import org.eclipse.che.commons.lang.Pair;

/** @author Anton Korneta */
public class SignaturePublicKeyEnvProvider implements EnvVarProvider {

  @Inject SignatureKeyManager keyManager;

  @Override
  public Pair<String, String> get(RuntimeIdentity runtimeIdentity) throws InfrastructureException {
    return Pair.of(
        "SIGNATURE_PUBLIC_KEY",
        new String(Base64.getEncoder().encode(keyManager.getKeyPair().getPublic().getEncoded())));
  }
}

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

import java.security.PrivateKey;
import java.security.PublicKey;

/** @author Anton Korneta */
public class SignatureKeyImpl implements SignatureKey {

  private String algorithm;

  private String format;

  private boolean isPublic;

  private byte[] encoded;

  public SignatureKeyImpl() {}

  public SignatureKeyImpl(PublicKey delegate) {
    this(true, delegate.getEncoded(), delegate.getAlgorithm(), delegate.getFormat());
  }

  public SignatureKeyImpl(PrivateKey delegate) {
    this(false, delegate.getEncoded(), delegate.getAlgorithm(), delegate.getFormat());
  }

  public SignatureKeyImpl(boolean isPublic, byte[] encoded, String algorithm, String format) {
    this.isPublic = isPublic;
    this.encoded = encoded;
    this.algorithm = algorithm;
    this.format = format;
  }

  @Override
  public boolean isPublic() {
    return isPublic;
  }

  @Override
  public String getAlgorithm() {
    return algorithm;
  }

  @Override
  public String getFormat() {
    return format;
  }

  @Override
  public byte[] getEncoded() {
    return encoded;
  }
}

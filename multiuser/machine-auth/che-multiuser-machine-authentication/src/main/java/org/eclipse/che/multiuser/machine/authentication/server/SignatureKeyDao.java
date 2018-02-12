/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.multiuser.machine.authentication.server;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;

/**
 * Defines data access object for {@link SignatureKeyPair}.
 *
 * @author Anton Korneta
 */
public interface SignatureKeyDao {

  void createKey(SignatureKey key) throws ConflictException, ServerException;

  SignatureKey getKey() throws NotFoundException, ServerException;

  void removeKey() throws ServerException;
}

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

import javax.inject.Singleton;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;

/**
 * JPA based implementation of {@link SignatureKeyDao}.
 *
 * @author Anton Korneta
 */
@Singleton
public class JpaSignatureKeyDao implements SignatureKeyDao {

  @Override
  public void createKey(SignatureKey key) throws ConflictException, ServerException {}

  @Override
  public SignatureKey getKey() throws NotFoundException, ServerException {
    return null;
  }

  @Override
  public void removeKey() throws ServerException {}

  //  private final Provider<EntityManager> managerProvider;
  //
  //  @Inject
  //  public JpaSignatureKeyDao(Provider<EntityManager> managerProvider) {
  //    this.managerProvider = managerProvider;
  //  }
  //
  //  @Override
  //  public void createKey(SignatureKey key) throws ConflictException, ServerException {
  //    requireNonNull(key, "Required non-null key");
  //    try {
  //      doCreate(key);
  //    } catch (RuntimeException ex) {
  //      throw new ServerException(ex.getLocalizedMessage(), ex);
  //    }
  //  }
  //
  //  @Override
  //  public SignatureKey getKey() throws NotFoundException, ServerException {
  //    final EntityManager manager = managerProvider.get();
  //    try {
  //      SignatureKey key =
  //          manager
  //              .createNamedQuery("SELECT * FROM SignatureKeyPair", SignatureKey.class)
  //              .getSingleResult();
  //      if (key == null) {
  //        throw new NotFoundException("No stored Keys found");
  //      }
  //      return key;
  //    } catch (RuntimeException ex) {
  //      throw new ServerException(ex.getLocalizedMessage(), ex);
  //    }
  //  }
  //
  //  @Override
  //  public void removeKey(SignatureKey key) throws ServerException {
  //    try {
  //      doRemove(key);
  //    } catch (RuntimeException ex) {
  //      throw new ServerException(ex.getLocalizedMessage(), ex);
  //    }
  //  }
  //
  //  private void doRemove(SignatureKey key) {
  //    final EntityManager manager = managerProvider.get();
  //    manager.remove(key);
  //    manager.flush();
  //  }
  //
  //  @Transactional
  //  protected void doCreate(SignatureKey key) {
  //    final EntityManager manager = managerProvider.get();
  //    manager.persist(key);
  //    manager.flush();
  //  }
}

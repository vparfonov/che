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

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Base64.Decoder;
import javax.inject.Singleton;

/** @author Anton Korneta */
@Singleton
public class SignatureKeyManager {

  private static final String RSA_KEY_FILENAME = "/data/rsa_key";

  private final int keySize = 2048;
  private final String algorithm = "RSA";

  //  @Singleton
  //  public SignatureKeyManager(
  //      @Named("che.signature_key.size") int keySize,
  //      @Named("che.signature_key.algorithm") String algorithm) {
  //    this.keySize = keySize;
  //    this.algorithm = algorithm;
  //  }

  public SignatureKeyPair getKeyPair() {
    final SignatureKeyPair signatureKeyPair = loadKeys();
    return signatureKeyPair != null ? signatureKeyPair : generateKeyPair();
  }

  private SignatureKeyPair generateKeyPair() {
    try {
      final KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithm);
      kpg.initialize(keySize);
      final KeyPair pair = kpg.generateKeyPair();
      final Base64.Encoder encoder = Base64.getEncoder();
      final File privateFile = new File(RSA_KEY_FILENAME + ".private");
      privateFile.getParentFile().mkdirs();
      FileWriter out = new FileWriter(privateFile);
      out.write(encoder.encodeToString(pair.getPrivate().getEncoded()));
      out.close();
      final File pubFile = new File(RSA_KEY_FILENAME + ".public");
      pubFile.createNewFile();
      out = new FileWriter(pubFile);
      out.write(encoder.encodeToString(pair.getPublic().getEncoded()));
      out.close();
      return new SignatureKeyPair(pair.getPrivate(), pair.getPublic());
    } catch (Exception ex) {
      // TODO
      throw new RuntimeException("Ohh no algorithm for keys generation found");
    }
  }

  private SignatureKeyPair loadKeys() {
    final Decoder decoder = Base64.getDecoder();
    PrivateKey privateKey;
    try {
      final Path path = Paths.get(RSA_KEY_FILENAME + ".private");
      final byte[] bytes = Files.readAllBytes(path);
      byte[] decoded = decoder.decode(bytes);
      final PKCS8EncodedKeySpec ks = new PKCS8EncodedKeySpec(decoded);
      final KeyFactory kf = KeyFactory.getInstance(algorithm);
      privateKey = kf.generatePrivate(ks);
    } catch (Exception ex) {
      return null;
    }
    PublicKey publicKey;
    try {
      final Path path = Paths.get(RSA_KEY_FILENAME + ".public");
      final byte[] bytes = Files.readAllBytes(path);
      byte[] decoded = decoder.decode(bytes);
      final X509EncodedKeySpec ks = new X509EncodedKeySpec(decoded);
      final KeyFactory kf = KeyFactory.getInstance(algorithm);
      publicKey = kf.generatePublic(ks);
    } catch (Exception ex) {
      return null;
    }
    return new SignatureKeyPair(privateKey, publicKey);
  }
}

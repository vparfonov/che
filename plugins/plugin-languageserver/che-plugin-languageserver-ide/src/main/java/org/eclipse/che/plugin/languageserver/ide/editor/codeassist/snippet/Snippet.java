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
package org.eclipse.che.plugin.languageserver.ide.editor.codeassist.snippet;

import java.util.List;

/**
 * The root expression of a snippet.
 *
 * @author Thomas Mäder
 */
public class Snippet extends Expression {

  private List<Expression> expressions;

  public Snippet(int startChar, int endChar, List<Expression> expressions) {
    super(startChar, endChar);
    this.expressions = expressions;
  }

  public List<Expression> getExpressions() {
    return expressions;
  }

  @Override
  public void accept(ExpressionVisitor v) {
    v.visit(this);
  }
}

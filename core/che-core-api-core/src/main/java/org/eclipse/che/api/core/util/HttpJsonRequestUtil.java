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
package org.eclipse.che.api.core.util;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.regex.Pattern.compile;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.che.commons.annotation.Nullable;

/**
 * Provides useful methods to simplify work with {@link
 * org.eclipse.che.api.core.rest.HttpJsonRequest} instances.
 *
 * @author Roman Nikitenko
 */
public final class HttpJsonRequestUtil {
  private static final String REQUEST_URL_FIELD = "Failed access:";
  private static final String REQUEST_METHOD_FIELD = "method:";
  private static final String RESPONSE_CODE_FIELD = "response code:";
  private static final String RESPONSE_MESSAGE_FIELD = "message:";
  private static final Pattern ERROR_PATTERN =
      compile(format(".+ %s ([0-9]*).+ %s (.*)", RESPONSE_CODE_FIELD, RESPONSE_MESSAGE_FIELD));

  /**
   * Helps to create formatted error message with corresponding info when request is failed.
   *
   * @param url request url
   * @param method request method
   * @param responseCode status code of response
   * @param message error message of response
   * @return error message with corresponding info about failed request
   */
  public static String createErrorMessage(
      String url, String method, int responseCode, String message) {
    return format(
        REQUEST_URL_FIELD
            + " %s, "
            + REQUEST_METHOD_FIELD
            + " %s, "
            + RESPONSE_CODE_FIELD
            + " %s, "
            + RESPONSE_MESSAGE_FIELD
            + " %s",
        url,
        method,
        responseCode,
        message);
  }

  /**
   * Helps to extract response code from formatted message.
   *
   * @param exceptionMessage formatted message with corresponding info about failed request. Note:
   *     message should match error pattern (should be created by {@link
   *     HttpJsonRequestUtil#createErrorMessage(String, String, int, String)})
   * @return status code of response or -1 when {@code exceptionMessage} does not match error
   *     pattern
   */
  public static int getResponseCode(String exceptionMessage) {
    Matcher matcher = ERROR_PATTERN.matcher(exceptionMessage);
    if (!matcher.find()) {
      return -1;
    }

    try {
      return parseInt(matcher.group(1));

    } catch (NumberFormatException e) {
      return -1;
    }
  }

  /**
   * Helps to extract error message which server sent.
   *
   * @param exceptionMessage formatted message with corresponding info about failed request. Note:
   *     message should match error pattern (should be created by {@link
   *     HttpJsonRequestUtil#createErrorMessage(String, String, int, String)})
   * @return error message which server sent or empty string when {@code exceptionMessage} does not
   *     match error pattern
   */
  public static String getErrorMessage(String exceptionMessage) {
    Matcher matcher = ERROR_PATTERN.matcher(exceptionMessage);
    if (!matcher.find()) {
      return "";
    }

    return matcher.group(2);
  }

  /**
   * Helps to parse given JSON string and get as a {@link JsonObject}.
   *
   * @param json JSON string for creating {@link JsonObject}
   * @return given JSON string as a {@link JsonObject} or {@code null} when malformed string is
   *     given.
   */
  @Nullable
  public static JsonObject getAsJsonObject(String json) {
    try {
      JsonParser jsonParser = new JsonParser();
      JsonElement jsonElement = jsonParser.parse(json);
      return jsonElement.getAsJsonObject();
    } catch (JsonSyntaxException e) {
      return null;
    }
  }
}

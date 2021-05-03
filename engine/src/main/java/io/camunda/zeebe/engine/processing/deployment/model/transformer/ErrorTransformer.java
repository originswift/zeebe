/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.engine.processing.deployment.model.transformer;

import io.camunda.zeebe.engine.processing.deployment.model.element.ExecutableError;
import io.camunda.zeebe.engine.processing.deployment.model.transformation.ModelElementTransformer;
import io.camunda.zeebe.engine.processing.deployment.model.transformation.TransformContext;
import io.camunda.zeebe.model.bpmn.instance.Error;
import io.camunda.zeebe.util.buffer.BufferUtil;
import java.util.Optional;

public class ErrorTransformer implements ModelElementTransformer<Error> {

  @Override
  public Class<Error> getType() {
    return Error.class;
  }

  @Override
  public void transform(final Error element, final TransformContext context) {

    final var error = new ExecutableError(element.getId());

    // ignore error events that are not references by the process
    Optional.ofNullable(element.getErrorCode())
        .map(BufferUtil::wrapString)
        .ifPresent(
            errorCode -> {
              error.setErrorCode(errorCode);
              context.addError(error);
            });
  }
}

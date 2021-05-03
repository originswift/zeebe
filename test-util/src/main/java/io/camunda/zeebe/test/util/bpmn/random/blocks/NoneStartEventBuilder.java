/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.test.util.bpmn.random.blocks;

import io.camunda.zeebe.model.bpmn.builder.ProcessBuilder;
import io.camunda.zeebe.test.util.bpmn.random.ConstructionContext;
import io.camunda.zeebe.test.util.bpmn.random.ExecutionPathSegment;
import io.camunda.zeebe.test.util.bpmn.random.StartEventBlockBuilder;
import io.camunda.zeebe.test.util.bpmn.random.steps.StepStartProcessInstance;
import java.util.Map;

public final class NoneStartEventBuilder implements StartEventBlockBuilder {

  private final String startEventId;

  public NoneStartEventBuilder(final ConstructionContext context) {
    final var idGenerator = context.getIdGenerator();
    startEventId = idGenerator.nextId();
  }

  @Override
  public io.camunda.zeebe.model.bpmn.builder.StartEventBuilder buildStartEvent(
      final ProcessBuilder processBuilder) {
    return processBuilder.startEvent(startEventId);
  }

  @Override
  public ExecutionPathSegment findRandomExecutionPath(
      final String processId, final Map<String, Object> variables) {
    final var pathSegment = new ExecutionPathSegment();
    pathSegment.appendDirectSuccessor(new StepStartProcessInstance(processId, variables));
    return pathSegment;
  }
}

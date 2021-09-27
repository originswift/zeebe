/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.engine.processing.bpmn.task;

import io.camunda.zeebe.engine.processing.bpmn.BpmnElementContext;
import io.camunda.zeebe.engine.processing.bpmn.BpmnElementProcessor;
import io.camunda.zeebe.engine.processing.bpmn.behavior.BpmnBehaviors;
import io.camunda.zeebe.engine.processing.bpmn.behavior.BpmnEventSubscriptionBehavior;
import io.camunda.zeebe.engine.processing.bpmn.behavior.BpmnIncidentBehavior;
import io.camunda.zeebe.engine.processing.bpmn.behavior.BpmnJobBehavior;
import io.camunda.zeebe.engine.processing.bpmn.behavior.BpmnStateTransitionBehavior;
import io.camunda.zeebe.engine.processing.bpmn.behavior.BpmnVariableMappingBehavior;
import io.camunda.zeebe.engine.processing.deployment.model.element.ExecutableJobWorkerTask;

/**
 * A BPMN processor for tasks that are based on jobs and should be processed by job workers. For
 * example, service tasks.
 */
public final class JobWorkerTaskProcessor implements BpmnElementProcessor<ExecutableJobWorkerTask> {

  private final BpmnIncidentBehavior incidentBehavior;
  private final BpmnStateTransitionBehavior stateTransitionBehavior;
  private final BpmnVariableMappingBehavior variableMappingBehavior;
  private final BpmnEventSubscriptionBehavior eventSubscriptionBehavior;
  private final BpmnJobBehavior jobBehavior;

  public JobWorkerTaskProcessor(final BpmnBehaviors behaviors) {
    eventSubscriptionBehavior = behaviors.eventSubscriptionBehavior();
    incidentBehavior = behaviors.incidentBehavior();
    stateTransitionBehavior = behaviors.stateTransitionBehavior();
    variableMappingBehavior = behaviors.variableMappingBehavior();
    jobBehavior = behaviors.jobBehavior();
  }

  @Override
  public Class<ExecutableJobWorkerTask> getType() {
    return ExecutableJobWorkerTask.class;
  }

  @Override
  public void onActivate(final ExecutableJobWorkerTask element, final BpmnElementContext context) {
    variableMappingBehavior
        .applyInputMappings(context, element)
        .flatMap(ok -> eventSubscriptionBehavior.subscribeToEvents(element, context))
        .flatMap(ok -> jobBehavior.createNewJob(context, element))
        .ifRightOrLeft(
            ok -> stateTransitionBehavior.transitionToActivated(context),
            failure -> incidentBehavior.createIncident(failure, context));
  }

  @Override
  public void onComplete(final ExecutableJobWorkerTask element, final BpmnElementContext context) {
    variableMappingBehavior
        .applyOutputMappings(context, element)
        .flatMap(
            ok -> {
              eventSubscriptionBehavior.unsubscribeFromEvents(context);
              return stateTransitionBehavior.transitionToCompleted(element, context);
            })
        .ifRightOrLeft(
            completed -> stateTransitionBehavior.takeOutgoingSequenceFlows(element, completed),
            failure -> incidentBehavior.createIncident(failure, context));
  }

  @Override
  public void onTerminate(final ExecutableJobWorkerTask element, final BpmnElementContext context) {

    jobBehavior.cancelJob(context);
    eventSubscriptionBehavior.unsubscribeFromEvents(context);
    incidentBehavior.resolveIncidents(context);

    eventSubscriptionBehavior
        .findEventTrigger(context)
        .ifPresentOrElse(
            eventTrigger -> {
              final var terminated = stateTransitionBehavior.transitionToTerminated(context);
              // Event triggers could be used to store variables. If the event trigger belongs to
              // the job worker task itself, we should not activate it, Otherwise the job worker
              // task will be activated a second time.
              if (!element.getId().equals(eventTrigger.getElementId())) {
                eventSubscriptionBehavior.activateTriggeredEvent(
                    context.getElementInstanceKey(),
                    terminated.getFlowScopeKey(),
                    eventTrigger,
                    terminated);
              } else {
                stateTransitionBehavior.onElementTerminated(element, terminated);
              }
            },
            () -> {
              final var terminated = stateTransitionBehavior.transitionToTerminated(context);
              stateTransitionBehavior.onElementTerminated(element, terminated);
            });
  }
}

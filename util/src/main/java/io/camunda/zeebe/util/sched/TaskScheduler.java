/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.util.sched;

import io.camunda.zeebe.util.sched.clock.ActorClock;

public interface TaskScheduler {
  ActorTask getNextTask(ActorClock now);

  default void onTaskReleased(final ActorTask task) {
    // no-op
  }
}

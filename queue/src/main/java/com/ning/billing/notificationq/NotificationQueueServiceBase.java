/*
 * Copyright 2010-2011 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.billing.notificationq;

import java.util.ArrayList;
import java.util.List;

import org.skife.jdbi.v2.IDBI;

import com.ning.billing.util.clock.Clock;


public abstract class NotificationQueueServiceBase extends NotificationQueueDispatcher implements NotificationQueueService {


    public NotificationQueueServiceBase(final Clock clock, final NotificationQueueConfig config, final IDBI dbi) {
        super(clock, config, dbi);
    }

    @Override
    public NotificationQueue createNotificationQueue(final String svcName,
                                                     final String queueName,
                                                     final NotificationQueueHandler handler) throws NotificationQueueAlreadyExists {
        if (svcName == null || queueName == null || handler == null) {
            throw new RuntimeException("Need to specify all parameters");
        }

        final String compositeName = getCompositeName(svcName, queueName);
        NotificationQueue result = null;
        synchronized (queues) {
            result = queues.get(compositeName);
            if (result != null) {
                throw new NotificationQueueAlreadyExists(String.format("Queue for svc %s and name %s already exist",
                                                                       svcName, queueName));
            }
            result = createNotificationQueueInternal(svcName, queueName, handler);
            queues.put(compositeName, result);
        }
        return result;
    }

    @Override
    public NotificationQueue getNotificationQueue(final String svcName,
                                                  final String queueName) throws NoSuchNotificationQueue {

        NotificationQueue result;
        final String compositeName = getCompositeName(svcName, queueName);
        synchronized (queues) {
            result = queues.get(compositeName);
            if (result == null) {
                throw new NoSuchNotificationQueue(String.format("Queue for svc %s and name %s does not exist",
                                                                svcName, queueName));
            }
        }
        return result;
    }

    public void deleteNotificationQueue(final String svcName, final String queueName)
            throws NoSuchNotificationQueue {
        final String compositeName = getCompositeName(svcName, queueName);
        synchronized (queues) {
            final NotificationQueue result = queues.get(compositeName);
            if (result == null) {
                throw new NoSuchNotificationQueue(String.format("Queue for svc %s and name %s does not exist",
                                                                svcName, queueName));
            }
            queues.remove(compositeName);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("NotificationQueueServiceBase");
        sb.append("{queues=").append(queues);
        sb.append('}');
        return sb.toString();
    }


    //
    // Test ONLY
    //
    @Override
    public int triggerManualQueueProcessing(final Boolean keepRunning) {

        int result = 0;

        List<NotificationQueue> manualQueues = new ArrayList<NotificationQueue>(queues.values());
        for (final NotificationQueue cur : manualQueues) {
            int processedNotifications = 0;
            do {
                doProcessEventsWithLimit(1);
                log.info("Got {} results from queue {}", processedNotifications, cur.getFullQName());
                result += processedNotifications;
            } while (keepRunning && processedNotifications > 0);
        }
        return result;
    }

    protected abstract NotificationQueue createNotificationQueueInternal(String svcName,
                                                                         String queueName, NotificationQueueHandler handler);
}

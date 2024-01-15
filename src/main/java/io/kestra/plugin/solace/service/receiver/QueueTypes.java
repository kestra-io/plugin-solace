package io.kestra.plugin.solace.service.receiver;

import com.solace.messaging.resources.Queue;

/**
 * Supported Solace queue types.
 */
public enum QueueTypes {

    DURABLE_EXCLUSIVE {
        @Override
        public Queue get(String queueName) {
            return Queue.durableExclusiveQueue(queueName);
        }
    },
    DURABLE_NON_EXCLUSIVE {
        @Override
        public Queue get(String queueName) {
            return Queue.durableNonExclusiveQueue(queueName);
        }
    },
    NON_DURABLE_EXCLUSIVE {
        @Override
        public Queue get(String queueName) {
            return Queue.nonDurableExclusiveQueue(queueName);
        }
    };


    public abstract Queue get(String queueName);
}

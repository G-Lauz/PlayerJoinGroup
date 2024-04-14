package fr.freebuild.playerjoingroup.spigot;

public class ConstantRetryPolicy implements RetryPolicy {
        private final int maxAttempts;
        private final long delay;

        public ConstantRetryPolicy(int maxAttempts, long delay) {
            this.maxAttempts = maxAttempts;
            this.delay = delay;
        }

        @Override
        public boolean shouldRetry(int attempt) {
            return attempt < maxAttempts;
        }

        @Override
        public long getDelay(int attempt) {
            return delay;
        }
}

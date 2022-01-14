package fr.freebuild.playerjoingroup.bungee.query;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class QueryHasPlayedBefore implements Callable<Boolean> {

    private Collection<QuerySpigotServer<Boolean>> queries;

    public QueryHasPlayedBefore(Collection<QuerySpigotServer<Boolean>> queries) {
        this.queries = queries;
    }

    @Override
    public Boolean call() throws InterruptedException {
        int poolSize = this.queries.size();
        if (poolSize < 1)
            return false;

        ExecutorService services = Executors.newFixedThreadPool(poolSize);
        List<Future<Boolean>> responses = services.invokeAll(this.queries);

        return responses.stream().anyMatch(future -> {
            try {
                return future.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            return false;
        });
    }
}

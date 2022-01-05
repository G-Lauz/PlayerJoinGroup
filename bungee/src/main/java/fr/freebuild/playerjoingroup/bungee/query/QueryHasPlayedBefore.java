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
        ExecutorService services = Executors.newFixedThreadPool(this.queries.size());
        List<Future<Boolean>> responses = services.invokeAll(this.queries);

        return responses.stream().allMatch(future -> {
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

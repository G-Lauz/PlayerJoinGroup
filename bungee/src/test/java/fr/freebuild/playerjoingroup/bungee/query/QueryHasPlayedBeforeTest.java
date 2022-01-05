package fr.freebuild.playerjoingroup.bungee.query;

import fr.freebuild.playerjoingroup.core.protocol.ConstructPacketErrorException;
import fr.freebuild.playerjoingroup.core.protocol.InvalidPacketException;
import fr.freebuild.playerjoingroup.core.protocol.Packet;

import net.md_5.bungee.api.config.ServerInfo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryHasPlayedBeforeTest {
    @Mock
    private Packet packet;

    @Mock
    private ServerInfo serverInfo;

    @Test
    void testQueryMultipleDelayedAnswer() throws ExecutionException, InterruptedException, InvalidPacketException, ConstructPacketErrorException {
        ScheduledExecutorService observable = Executors.newScheduledThreadPool(3);
        ExecutorService service = Executors.newSingleThreadExecutor();

        ArrayList<QuerySpigotServer<Boolean>> queries = new ArrayList<>();

        for(int i = 0; i < 3; i++) {
            QuerySpigotServer<Boolean> query = spy(new QuerySpigotServer<>(serverInfo, packet));
            doNothing().when(query).sendQuery();
            queries.add(query);

            int finalI = i;
            observable.schedule(() -> {
                try {
                    Thread.sleep(2000 - (finalI * 500));
                    query.notify(true);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    assert false;
                }
            }, 1, TimeUnit.SECONDS);
        }

        Future<Boolean> response = service.submit(new QueryHasPlayedBefore(queries));
        assertTrue(response.get());
    }

    @Test
    void testOneServerReturnFalse() throws InvalidPacketException, ConstructPacketErrorException, ExecutionException, InterruptedException {
        ScheduledExecutorService observable = Executors.newScheduledThreadPool(3);
        ExecutorService service = Executors.newSingleThreadExecutor();

        ArrayList<QuerySpigotServer<Boolean>> queries = new ArrayList<>();

        for(int i = 0; i < 3; i++) {
            QuerySpigotServer<Boolean> query = spy(new QuerySpigotServer<>(serverInfo, packet));
            doNothing().when(query).sendQuery();
            queries.add(query);

            int finalI = i;
            observable.schedule(() -> {
                try {
                    Thread.sleep(2000 - (finalI * 500));

                    if (finalI == 1)
                        query.notify(false);
                    else
                        query.notify(true);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    assert false;
                }
            }, 1, TimeUnit.SECONDS);
        }

        Future<Boolean> response = service.submit(new QueryHasPlayedBefore(queries));
        assertFalse(response.get());
    }
}
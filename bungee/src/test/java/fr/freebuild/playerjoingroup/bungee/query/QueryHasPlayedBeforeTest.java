package fr.freebuild.playerjoingroup.bungee.query;

import fr.freebuild.playerjoingroup.bungee.MessagesManager;
import fr.freebuild.playerjoingroup.core.protocol.ConstructPacketErrorException;
import fr.freebuild.playerjoingroup.core.protocol.InvalidPacketException;
import fr.freebuild.playerjoingroup.core.protocol.Packet;

import net.md_5.bungee.api.config.ServerInfo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryHasPlayedBeforeTest {
    @Mock
    private Packet packet;

    @Mock
    private MessagesManager messagesManager;

    @Test
    void testQueryMultipleDelayedAnswer() throws ExecutionException, InterruptedException, InvalidPacketException, ConstructPacketErrorException, IOException {
        ScheduledExecutorService observable = Executors.newScheduledThreadPool(3);
        ExecutorService service = Executors.newSingleThreadExecutor();

        ArrayList<QuerySpigotServer<Boolean>> queries = new ArrayList<>();

        for(int i = 0; i < 3; i++) {
            QuerySpigotServer<Boolean> query = spy(new QuerySpigotServer<>("server1", packet, messagesManager));
            doNothing().when(query).sendQuery();
            queries.add(query);

            int finalI = i;
            observable.schedule(() -> {
                try {
                    Thread.sleep(2000 - (finalI * 500));
                    query.update(true);
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
    void testOneServerReturnFalse() throws InvalidPacketException, ConstructPacketErrorException, ExecutionException, InterruptedException, IOException {
        ScheduledExecutorService observable = Executors.newScheduledThreadPool(3);
        ExecutorService service = Executors.newSingleThreadExecutor();

        ArrayList<QuerySpigotServer<Boolean>> queries = new ArrayList<>();

        for(int i = 0; i < 3; i++) {
            QuerySpigotServer<Boolean> query = spy(new QuerySpigotServer<>("server1", packet, messagesManager));
            doNothing().when(query).sendQuery();
            queries.add(query);

            int finalI = i;
            observable.schedule(() -> {
                try {
                    Thread.sleep(2000 - (finalI * 500));

                    if (finalI == 1)
                        query.update(false);
                    else
                        query.update(false);
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
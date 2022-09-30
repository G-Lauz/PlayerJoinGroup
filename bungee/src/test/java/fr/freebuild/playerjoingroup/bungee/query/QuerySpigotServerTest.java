package fr.freebuild.playerjoingroup.bungee.query;

import fr.freebuild.playerjoingroup.bungee.MessagesManager;
import fr.freebuild.playerjoingroup.core.protocol.*;

import net.md_5.bungee.api.config.ServerInfo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuerySpigotServerTest {

    @Mock
    private Packet packet;

    @Mock
    private MessagesManager messagesManager;

    @Test
    void testQueryDelayedAnswer() throws ExecutionException, InterruptedException, InvalidPacketException, ConstructPacketErrorException, IOException {
        ExecutorService service = Executors.newFixedThreadPool(2);

        QuerySpigotServer<Boolean> query = spy(new QuerySpigotServer<>("server1", this.packet, this.messagesManager));
        doNothing().when(query).sendQuery();

        Future<Boolean> answer = service.submit(query);

        service.submit(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                assert false;
            }
            query.update(true);
        });

        assertTrue(answer.get());
    }
}
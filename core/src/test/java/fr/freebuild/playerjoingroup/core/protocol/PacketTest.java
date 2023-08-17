package fr.freebuild.playerjoingroup.core.protocol;

import fr.freebuild.playerjoingroup.core.event.EventType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PacketTest {
    @Test
    void testPacketBuilder() {
        Subchannel subchannel = Subchannel.BROADCAST;
        String data = "test data";
        EventType event = EventType.SERVER_CONNECT;
        String serverGroup = "test-server";
        String key = "testKey";
        String value = "testValue";

        Packet packet = new Packet.Builder(subchannel)
                .setData(data)
                .setEventType(event)
                .setServerGroup(serverGroup)
                .appendParam(key, value)
                .build();

        assertEquals(packet.getSubchannel(), subchannel.getValue());
        assertEquals(packet.getData(), data);

        Map<String, String> params = packet.getFields();
        assertEquals(EventType.typeof(packet.getField(ParamsKey.EVENT)), event);
        assertEquals(packet.getField(ParamsKey.SERVER_GROUP.getValue()), serverGroup);
        assertTrue(params.containsKey(key));
        assertEquals(params.get(key), value);
    }
}
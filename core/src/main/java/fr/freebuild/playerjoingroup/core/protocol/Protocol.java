package fr.freebuild.playerjoingroup.core.protocol;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.io.*;
import java.util.Map;

public class Protocol {

    /**
     * Construct a Packet with the following format:
     * <ol type="1">
     *   <li>Subchannel : String</li>
     *   <li>Has Parameter : Boolean</li>
     *   <li>Number of parameters : Short</li>
     *   <li>List of Parameters :
     *      <li>Parameter key: byte[]</li>
     *      <li>Parameter value: byte[]</li>
     *   </li>
     *   <li>Has data : Boolean</li>
     *   <li>Data length : Short</li>
     *   <li>The actual data : byte[]</li>
     * </ol>
     *
     * @param packet
     * @return
     * @throws InvalidPacketException
     * @throws ConstructPacketErrorException
     */
    public static byte[] constructPacket(Packet packet) throws InvalidPacketException, ConstructPacketErrorException {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();

        String subchannel = packet.getSubchannel();
        if (subchannel == null)
            throw new InvalidPacketException("packet.actions is null");

        output.writeUTF(subchannel);

        Map<String, String> params = packet.getFields();
        if (params != null) {
            output.writeBoolean(true);
            output.writeShort(params.size());
            params.forEach((key, value) -> {
                output.writeUTF(key);
                output.writeUTF(value);
            });
        } else {
            output.writeBoolean(false);
        }

        String data = packet.getData();
        if (data != null) {
            output.writeBoolean(true);

            ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
            DataOutputStream msgout = new DataOutputStream(msgbytes);

            try {
                msgout.writeUTF(data);

                output.writeShort(msgbytes.toByteArray().length);
                output.write(msgbytes.toByteArray());

            } catch (IOException exception) {
                throw new ConstructPacketErrorException("Unable to write packet.data:\n" + exception.getMessage());
            }
        } else {
            output.writeBoolean(false);
        }

        return output.toByteArray();
    }

    /**
     *
     * @param bytes
     * @return
     * @throws DeconstructPacketErrorException
     */
    public static Packet deconstructPacket(byte[] bytes) throws DeconstructPacketErrorException {
        ByteArrayDataInput input = ByteStreams.newDataInput(bytes);

        String subchannel = input.readUTF();
        Packet.Builder packetBuilder = new Packet.Builder(subchannel);

        boolean hasParameters = input.readBoolean();
        if (hasParameters) {
            short nbParameters = input.readShort();
            for(int i = 0; i < nbParameters; i++) {
                packetBuilder.appendParam(input.readUTF(), input.readUTF());
            }
        }

        boolean hasDatum = input.readBoolean();
        if (hasDatum) {
            short len = input.readShort();
            byte[] msgbytes = new byte[len];
            input.readFully(msgbytes);

            DataInputStream msgin = new DataInputStream(new ByteArrayInputStream(msgbytes));

            try {
                packetBuilder.setData(msgin.readUTF());
            } catch (IOException exception) {
                throw new DeconstructPacketErrorException("Unable to read the data:\n" + exception.getMessage());
            }
        }

        return packetBuilder.build();
    }
}

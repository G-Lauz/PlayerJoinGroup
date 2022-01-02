package fr.freebuild.playerjoingroup.core.protocol;

import fr.freebuild.playerjoingroup.core.event.EventType;

import java.util.HashMap;
import java.util.Map;

public class Packet {
    private String subchannel;
    private Map<String, String> params;
    private String data;

    public static class Builder {
        private final String subchannel;
        private Map<String, String> params;
        private String data;

        public Builder(Subchannel subchannel) {
            this.subchannel = subchannel.getValue();
            this.params = new HashMap<String, String>();
            this.data = null;
        }

        public Builder(String subchannel) {
            this.subchannel = subchannel;
            this.params = new HashMap<String, String>();
            this.data = null;
        }

        public Packet build() {
            return new Packet(this);
        }

        public Builder setData(String data) {
            this.data = data;
            return this;
        }

        public Builder setServerGroup(String serverGroup) {
            this.params.put(ParamsKey.SERVER_GROUP.getValue(), serverGroup);
            return this;
        }

        public Builder setEventType(EventType eventType) {
            this.params.put(ParamsKey.EVENT.getValue(), eventType.getValue());
            return this;
        }

        public Builder appendParam(String key, String value) {
            this.params.put(key, value);
            return this;
        }
    }

    public Packet(Builder builder) {
        this.subchannel = builder.subchannel;
        this.params = builder.params;
        this.data = builder.data;
    }

    @Override
    public String toString(){
        String obj = "Subchannel: " + this.getSubchannel() + "\n" + "Parameters:\n";
        for (Map.Entry<String, String> map : this.getParams().entrySet()) {
            obj = obj.concat("\t\"" + map.getKey() + "\" : \"" + map.getValue() + "\"\n");
        }
        obj = obj.concat("Data: " + this.getData());
        return obj;
    }

    public String getSubchannel() {
        return subchannel;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public String getData() {
        return data;
    }
}

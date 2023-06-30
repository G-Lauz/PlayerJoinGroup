package fr.freebuild.playerjoingroup.core.protocol;

import fr.freebuild.playerjoingroup.core.event.EventType;

import java.util.Map;

public class Packet {
    private String subchannel;
    private FieldManager fieldManager;
    private String data;

    public static class Builder {
        private final String subchannel;
        private FieldManager fieldManager;
        private String data;

        public Builder(Subchannel subchannel) {
            this.subchannel = subchannel.getValue();
            this.fieldManager = new FieldManager();
            this.data = null;
        }

        public Builder(String subchannel) {
            this.subchannel = subchannel;
            this.fieldManager = new FieldManager();
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
            this.fieldManager.setField(ParamsKey.SERVER_GROUP, serverGroup);
            return this;
        }

        public Builder setEventType(EventType eventType) {
            this.fieldManager.setField(ParamsKey.EVENT, eventType.getValue());
            return this;
        }

        public Builder setQuery(QueryType query) {
            this.fieldManager.setField(ParamsKey.QUERY, query.getValue());
            return this;
        }

        public Builder setHashCode(Integer hashCode) {
            this.fieldManager.setField(ParamsKey.HASH_CODE, hashCode.toString());
            return this;
        }

        public Builder appendParam(String key, String value) {
            this.fieldManager.setField(key, value);
            return this;
        }
    }

    public Packet(Builder builder) {
        this.subchannel = builder.subchannel;
        this.fieldManager = builder.fieldManager;
        this.data = builder.data;
    }

    @Override
    public String toString(){
        String obj = "Subchannel: " + this.getSubchannel() + "\n" + "Parameters:\n";
        for (Map.Entry<String, String> map : this.fieldManager.getFields().entrySet()) {
            obj = obj.concat("\t\"" + map.getKey() + "\" : \"" + map.getValue() + "\"\n");
        }
        obj = obj.concat("Data: " + this.getData());
        return obj;
    }

    public String getSubchannel() {
        return subchannel;
    }

    public String getField(Object key) {
        return this.fieldManager.get(key);
    }

    public Map<String, String> getFields() {
        return this.fieldManager.getFields();
    }

    public String getData() {
        return data;
    }
}

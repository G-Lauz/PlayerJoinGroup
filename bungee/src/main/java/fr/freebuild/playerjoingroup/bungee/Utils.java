package fr.freebuild.playerjoingroup.bungee;

import java.util.ArrayList;
import java.util.Map;

public class Utils {

    public static String getServerGroupName(String serverName, Config config) {

        for (Object entry : config.getGroup().entrySet()) {
            if (((Map.Entry<String, ArrayList<String>>)entry).getValue().contains(serverName))
                return ((Map.Entry<String, ArrayList<String>>)entry).getKey();
        }

        return null;
    }
}

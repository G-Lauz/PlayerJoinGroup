package fr.freebuild.playerjoingroup.bungee;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UtilsTest {

    @Mock
    private Config config;
    private Hashtable mockGroupTable;

    @BeforeEach
    void setUp() {
        mockGroupTable = new Hashtable();
        ArrayList<String> survival = new ArrayList<>();
        survival.add("survival1");
        survival.add("survival2");
        mockGroupTable.put("survival", survival);
        ArrayList<String> creative = new ArrayList<>();
        survival.add("creative1");
        survival.add("creative2");
        mockGroupTable.put("creative", creative);

        when(config.getGroup()).thenReturn(mockGroupTable);
    }

    @Test
    void getServerGroupName() {
        String group = Utils.getServerGroupName("survival1", config);

        assertEquals("survival", group);
    }
}
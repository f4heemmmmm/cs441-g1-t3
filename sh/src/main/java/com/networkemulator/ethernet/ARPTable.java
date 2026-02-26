package com.networkemulator.ethernet;

import java.util.HashMap;
import java.util.Map;

public class ARPTable {
    private final Map<Integer , String> table = new HashMap<>();

    public void addEntry(int ip , String mac){
        table.put(ip , mac);
    }

    public String getMac(int ip){
        return table.get(ip);
    }
}

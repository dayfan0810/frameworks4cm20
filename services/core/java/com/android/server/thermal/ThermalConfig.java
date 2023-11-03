package com.android.server.thermal;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import android.util.Slog;


public class ThermalConfig {
    public static class CpuConfig {
        public int sampling;
        public int[] thresholds;
        public int[] thresholdsClr;
        public String[] actions;
        public int[] actionInfo;
    }

    private Map<String, CpuConfig> cpuConfigs;

    public ThermalConfig(String configFile) throws IOException {
        cpuConfigs = new HashMap<>();
        readConfigFile(configFile);
    }

    private void readConfigFile(String configFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(configFile));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("#") || line.isEmpty()) {
                continue;
            }
            /*
            [CPU0_MONITOR]
            sampling         1000
            thresholds       70000    85000    90000 
            thresholds_clr   67000    80000    85000 
            actions          cpu0 cpu0 cpu0
            action_info      1890000 1674000 1350000

            [CPU4_MONITOR]
            sampling         1000
            thresholds       70000    85000    90000 
            thresholds_clr   67000    80000    85000 
            actions          cpu4 cpu4 cpu4
            action_info      1890000 1674000 1350000
            */
            if (line.startsWith("[CPU")) {
                String cpu = line.substring(line.indexOf("[") + 1, line.indexOf("]"));
                CpuConfig cpuConfig = new CpuConfig();
                String[] parts = reader.readLine().split("\\s+");
                cpuConfig.sampling = Integer.parseInt(parts[1]);
                cpuConfig.thresholds = new int[]{Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4])};
                cpuConfig.thresholdsClr = new int[]{Integer.parseInt(parts[5]), Integer.parseInt(parts[6]), Integer.parseInt(parts[7])};
                cpuConfig.actions = new String[]{parts[8], parts[9], parts[10]};
                cpuConfig.actionInfo = new int[]{Integer.parseInt(parts[11]), Integer.parseInt(parts[12]), Integer.parseInt(parts[13])};
                cpuConfigs.put(cpu, cpuConfig);
            }
        }
        reader.close();
    }

    public CpuConfig getCpuConfig(String cpu) {
        return cpuConfigs.get(cpu);
    }

    public Map<String, CpuConfig> getAllCpuConfigs() {
        return cpuConfigs;
    }
}

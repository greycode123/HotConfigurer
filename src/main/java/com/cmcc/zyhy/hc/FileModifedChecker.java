package com.cmcc.zyhy.hc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class FileModifedChecker {
    
    private Logger logger = LoggerFactory.getLogger(FileModifedChecker.class);
    
    private List<String> locations;
    private Map<String, Long> fileData = new HashMap<>();
    
    public FileModifedChecker(List<String> locations) {
        this.locations = locations;
        
    }
    
    private void initFileData() {
        for (String location : locations) {
            fileData.put(location, new File(location).lastModified());
        }
    }
    
    public void startCheck() {
        initFileData();
    
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    logger.info("startCheck...");
                    for (String location : locations) {
                        long oldTime = fileData.get(location);
                        long newTime = new File(location).lastModified();
                        logger.info("{} oldTime:{} newTime:{}", location, oldTime, newTime);
                        if (oldTime != newTime) {
                            logger.info("{} location modified, trigger change");
                            onChange();
                            fileData.put(location, newTime);
                        }
                    }
                    try {
                        Thread.sleep(10 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        t.setName("File-Modifed-Checker");
        t.setDaemon(true);
        t.start();
    }
    
    public abstract void onChange();
}

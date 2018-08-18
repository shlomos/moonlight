package org.moonlightcontroller.aggregator;

import org.moonlightcontroller.bal.BoxApplication;
import org.moonlightcontroller.processing.IProcessingBlock;

public class Origin {
    private BoxApplication app;
    private IProcessingBlock block;
    private String id;
    
    public Origin(BoxApplication app, IProcessingBlock block, String id) {
        this.app = app;
        this.block = block;
        this.id = id;
    }

    public BoxApplication getApp() {
        return this.app;
    }

    public IProcessingBlock getBlock() {
        return this.block;
    }

    public String getId() {
        return this.id;
    }
}
package org.moonlightcontroller.mtd;

import org.moonlightcontroller.aggregator.ApplicationAggregator;
import org.moonlightcontroller.aggregator.IApplicationAggregator;
import org.moonlightcontroller.managers.ConnectionManager;
import org.moonlightcontroller.topology.ITopologyManager;
import org.moonlightcontroller.topology.InstanceLocationSpecifier;
import org.moonlightcontroller.topology.TopologyManager;

import java.util.TimerTask;
import java.util.Timer;
import java.util.Date;

public class ProactiveCasteling extends TimerTask {

    public static final int DELTA = 30000;

    Date now;
    IApplicationAggregator aggregator;
    ConnectionManager connMgr;
    Timer time;
    
    public ProactiveCasteling() {
        super();
        this.aggregator = ApplicationAggregator.getInstance();
        this.connMgr = ConnectionManager.getInstance();
        this.time = new Timer();
    }

    public void start() {
        time.schedule(this, DELTA, DELTA);
    }

	public void run() {
        ITopologyManager topology = TopologyManager.getInstance();
        now = new Date();

        System.out.println("[" + now + "] " + "Proactively casteling applications...");
        for (InstanceLocationSpecifier loc : topology.getAllEndpoints()) {
            this.aggregator.invalidateProcessingGraph(loc);
            connMgr.sendSetProcessingGraphRequest(loc);
        }
        System.out.println("sending re-aggregated graph");
	}
}
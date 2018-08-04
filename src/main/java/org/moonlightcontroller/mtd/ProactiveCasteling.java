package org.moonlightcontroller.mtd;

import org.moonlightcontroller.aggregator.ApplicationAggregator;
import org.moonlightcontroller.aggregator.IApplicationAggregator;
import org.moonlightcontroller.topology.InstanceLocationSpecifier;
 
import java.util.TimerTask;
import java.util.Timer;
import java.util.Date;


// Create a class extends with TimerTask
public class ProactiveCasteling extends TimerTask {

    public static final int DELTA = 30000;

    Date now;
    IApplicationAggregator aggregator;
    Timer time;
    
    public ProactiveCasteling() {
        super();
        this.aggregator = ApplicationAggregator.getInstance();
        this.time = new Timer();
    }

    public void start() {
        time.schedule(this, DELTA, DELTA);
    }

	public void run() {
		now = new Date();
        System.out.println("[" + now + "] " + "Proactively casteling applications...");
        InstanceLocationSpecifier randomLocation = new InstanceLocationSpecifier(22);
        this.aggregator.invalidateProcessingGraph(randomLocation);
        this.aggregator.aggregateLocation(randomLocation);
        System.out.println("sending re-aggregated graph");
	}
}
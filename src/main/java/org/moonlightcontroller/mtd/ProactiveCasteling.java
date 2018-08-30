package org.moonlightcontroller.mtd;

import org.moonlightcontroller.aggregator.ApplicationAggregator;
import org.moonlightcontroller.aggregator.IApplicationAggregator;
import org.moonlightcontroller.managers.ConnectionManager;
import org.moonlightcontroller.topology.ITopologyManager;
import org.moonlightcontroller.topology.InstanceLocationSpecifier;
import org.moonlightcontroller.topology.TopologyManager;
import java.util.Random;
import java.util.logging.Logger;

import java.util.TimerTask;
import java.util.Timer;
import java.util.Date;

public class ProactiveCasteling extends TimerTask {

    public static final int DELTA = 30000;

    protected Date now;
    protected Random random;
    protected IApplicationAggregator aggregator;
    protected ConnectionManager connMgr;
    protected Timer time;

    private final static Logger LOG = Logger.getLogger(ProactiveCasteling.class.getName());
    
    public ProactiveCasteling() {
        super();
        this.aggregator = ApplicationAggregator.getInstance();
        this.connMgr = ConnectionManager.getInstance();
        this.time = new Timer();
        this.random = new Random();
    }

    public void start() {
        time.schedule(this, DELTA, DELTA);
    }

	public void run() {
        ITopologyManager topology = TopologyManager.getInstance();
        now = new Date();

        LOG.info("Proactively casteling applications...");
        for (InstanceLocationSpecifier loc : topology.getAllEndpoints()) {
            if (this.random.nextInt(2) == 1) {
                this.aggregator.invalidateProcessingGraph(loc);
                if (connMgr.sendSetProcessingGraphRequest(loc) != null) {
                    LOG.info("Sending re-aggregated graph");
                }
            }
        }
	}
}
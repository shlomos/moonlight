package org.moonlightcontroller.blocks;

import org.moonlightcontroller.processing.ProcessingBlock;
import org.openboxprotocol.protocol.Priority;

import java.util.List;
import java.util.Map;

import org.moonlightcontroller.aggregator.Tupple.Pair;
import org.moonlightcontroller.exceptions.MergeException;
import org.moonlightcontroller.processing.BlockClass;
import org.moonlightcontroller.processing.IProcessingGraph;

public class StringMatcher extends ProcessingBlock implements IClassifierProcessingBlock {
	private List<String> pattern;
    private boolean payload_only;
    private String matcher_type;
	private boolean match_all;
	private Priority priority;

	public StringMatcher(String id, List<String> pattern, String matcher_type) {
        super(id);
        this.matcher_type = matcher_type;
		this.pattern = pattern;
	}
	
	public StringMatcher(String id, List<String> pattern, String matcher_type, boolean payload_only, boolean match_all, Priority priority) {
		super(id);
        this.pattern = pattern;
        this.matcher_type = matcher_type;
	}

	public List<String> getPattern() {
		return pattern;
	}

	public boolean getPayload_only() {
		return payload_only;
	}

	public boolean getMatch_all() {
		return match_all;
    }
    
    public String getMatcherType() {
		return matcher_type;
	}

	@Override
	public BlockClass getBlockClass() {
		return BlockClass.BLOCK_CLASS_CLASSIFIER;
	}

	@Override
	protected void putConfiguration(Map<String, Object> config) {
		config.put("matcher", this.matcher_type);
		config.put("pattern", this.pattern.toArray(new String[0]));
	}

	@Override
	protected ProcessingBlock spawn(String id) {
		return new StringMatcher(id, pattern, matcher_type, payload_only, match_all, priority);
	}

	@Override
	public Priority getPriority() {
		return priority;
	}

	@Override
	public boolean canMergeWith(IClassifierProcessingBlock other) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public IClassifierProcessingBlock mergeWith(IClassifierProcessingBlock other, IProcessingGraph containingGraph,
			List<Pair<Integer>> outPortSources) throws MergeException {
		// TODO Auto-generated method stub
		return null;
	}
}

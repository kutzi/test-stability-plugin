package de.esailors.jenkins.teststability;

import hudson.tasks.junit.TestAction;
import hudson.tasks.junit.TestObject;
import hudson.tasks.junit.TestResultAction.Data;
import hudson.tasks.junit.CaseResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jenkins.model.Jenkins;



class StabilityTestData extends Data {
	
	static {
		// TODO: this doesn't seem to work
		Jenkins.XSTREAM2.aliasType("circularStabilityHistory", CircularStabilityHistory.class);
	}
	
	private final Map<String,CircularStabilityHistory> stability;
	
	public StabilityTestData(Map<String, CircularStabilityHistory> stabilityHistory) {
		this.stability = stabilityHistory;
	}

	@SuppressWarnings("deprecation")
	@Override
	public List<? extends TestAction> getTestAction(TestObject testObject) {
		
		if (testObject instanceof CaseResult) {
			CaseResult cr = (CaseResult) testObject;
			CircularStabilityHistory ringBuffer = stability.get(cr.getId());
			return Collections.singletonList(new StabilityTestAction(ringBuffer));
		}
		
		return Collections.emptyList();
	}
	
	
	
	public static class Result {
		int buildNumber;
		boolean passed;
		
		public Result(int buildNumber, boolean passed) {
			super();
			this.buildNumber = buildNumber;
			this.passed = passed;
		}
	}
	
	
}

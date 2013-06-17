package de.esailors.jenkins.teststability;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.tasks.junit.PackageResult;
import hudson.tasks.junit.TestDataPublisher;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction.Data;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.ClassResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import de.esailors.jenkins.teststability.StabilityTestData.CircularStabilityHistory;
import de.esailors.jenkins.teststability.StabilityTestData.Result;

public class StabilityTestDataPublisher extends TestDataPublisher {
	
	@DataBoundConstructor
	public StabilityTestDataPublisher() {
	}
	
	@Override
	public Data getTestData(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener, TestResult testResult) throws IOException,
			InterruptedException {
		
		Map<String,CircularStabilityHistory> stabilityHistory = new HashMap<String,CircularStabilityHistory>();
		
		for (CaseResult result: getCaseResults(testResult)) {
			
			CircularStabilityHistory previousRingBuffer = getPreviousHistory(result);
			
			if (previousRingBuffer != null) {
				if (result.isPassed()) {
					previousRingBuffer.add(build.getNumber(), true);
				} else if (result.getFailCount() > 0) {
					previousRingBuffer.add(build.getNumber(), false);
				}
				// else test is skipped and we leave history unchanged
				
				stabilityHistory.put(result.getId(), previousRingBuffer);
			} else if (isFirstTestFailure(result, previousRingBuffer)) {
				int maxHistoryLength = getDescriptor().getMaxHistoryLength();
				CircularStabilityHistory ringBuffer = new CircularStabilityHistory(maxHistoryLength);
				
				// add previous results (if there are any):
				buildUpInitialHistory(ringBuffer, result, maxHistoryLength - 1);
				
				ringBuffer.add(build.getNumber(), false);
				stabilityHistory.put(result.getId(), ringBuffer);
			}
		}
		
		return new StabilityTestData(stabilityHistory);
	}

	private CircularStabilityHistory getPreviousHistory(CaseResult result) {
		CaseResult previous = result.getPreviousResult();
		if (previous != null) {
			StabilityTestAction previousAction = previous.getTestAction(StabilityTestAction.class);
			if (previousAction != null) {
				CircularStabilityHistory ringBuffer = previousAction.getRingBuffer();
				
				if (ringBuffer == null) {
					return null;
				}
				
				if (ringBuffer.getMaxSize() != getDescriptor().getMaxHistoryLength()) {
					Result[] data = ringBuffer.getData();
					
					ringBuffer = new CircularStabilityHistory(getDescriptor().getMaxHistoryLength());
					ringBuffer.addAll(data);
				}
				
				return ringBuffer;
			}
		}
		return null;
	}

	private boolean isFirstTestFailure(CaseResult result,
			CircularStabilityHistory previousRingBuffer) {
		return previousRingBuffer == null && result.getFailCount() > 0;
	}
	
	private void buildUpInitialHistory(CircularStabilityHistory ringBuffer, CaseResult result, int number) {
		List<Result> testResultsFromNewestToOldest = new ArrayList<Result>(number);
		CaseResult previousResult = result.getPreviousResult();
		while (previousResult != null) {
			testResultsFromNewestToOldest.add(
					new Result(previousResult.getOwner().getNumber(), previousResult.isPassed()));
			previousResult = previousResult.getPreviousResult();
		}

		for (int i = testResultsFromNewestToOldest.size() - 1; i >= 0; i--) {
			ringBuffer.add(testResultsFromNewestToOldest.get(i));
		}
	}

	private Collection<CaseResult> getCaseResults(TestResult testResult) {
		List<CaseResult> results = new ArrayList<CaseResult>();
		
		Collection<PackageResult> packageResults = testResult.getChildren();
		for (PackageResult pkgResult : packageResults) {
			Collection<ClassResult> classResults = pkgResult.getChildren();
			for (ClassResult cr : classResults) {
				results.addAll(cr.getChildren());
			}
		}

		return results;
	}

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }
	

	@Extension
	public static class DescriptorImpl extends Descriptor<TestDataPublisher> {
		
		private int maxHistoryLength = 30;

		@Override
		public boolean configure(StaplerRequest req, JSONObject json)
				throws FormException {
			this.maxHistoryLength = json.getInt("maxHistoryLength");
			
			save();
            return super.configure(req,json);
		}
		
		public int getMaxHistoryLength() {
			return this.maxHistoryLength;
		}

		@Override
		public String getDisplayName() {
			return "Test stability history";
		}
	}
}

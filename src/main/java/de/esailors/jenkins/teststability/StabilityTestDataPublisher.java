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

import org.kohsuke.stapler.DataBoundConstructor;

import de.esailors.jenkins.teststability.StabilityTestData.CircularBuffer;

public class StabilityTestDataPublisher extends TestDataPublisher {
	
	private static final int MAX_HISTORY_SIZE = 10;

	@DataBoundConstructor
	public StabilityTestDataPublisher() {
	}
	
	@Override
	public Data getTestData(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener, TestResult testResult) throws IOException,
			InterruptedException {
		
		Map<String,CircularBuffer> stabilityHistory = new HashMap<String,CircularBuffer>();
		
		for (CaseResult result: getCaseResults(testResult)) {
			
			CircularBuffer previousRingBuffer = null;
			
			CaseResult previous = result.getPreviousResult();
			if (previous != null) {
				StabilityTestAction previousAction = previous.getTestAction(StabilityTestAction.class);
				if (previousAction != null) {
					previousRingBuffer = previousAction.getRingBuffer();

					if (previousRingBuffer != null) {
						if (result.isPassed()) {
							previousRingBuffer.add(true);
						} else if (result.getFailCount() > 0) {
							previousRingBuffer.add(false);
						}
						
						stabilityHistory.put(result.getId(), previousRingBuffer);
					}
				}
			}
			
			if (previousRingBuffer == null && result.getFailCount() > 0) {
				CircularBuffer ringBuffer = new CircularBuffer(MAX_HISTORY_SIZE);
				
				// add previous 9 results (if there are any):
				buildUpInitialHistory(ringBuffer, result, MAX_HISTORY_SIZE - 1);
				
				ringBuffer.add(false);
				stabilityHistory.put(result.getId(), ringBuffer);
			}
		}
		
		return new StabilityTestData(stabilityHistory);
	}

	
	private void buildUpInitialHistory(CircularBuffer ringBuffer, CaseResult result, int number) {
		List<Boolean> passesFromNewestToOldest = new ArrayList<Boolean>(number);
		CaseResult previousResult = result.getPreviousResult();
		while (previousResult != null) {
			passesFromNewestToOldest.add(previousResult.isPassed());
			previousResult = previousResult.getPreviousResult();
		}

		for (int i = passesFromNewestToOldest.size() - 1; i >= 0; i--) {
			ringBuffer.add(passesFromNewestToOldest.get(i));
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


	@Extension
	public static class DescriptorImpl extends Descriptor<TestDataPublisher> {
		
		@Override
		public String getDisplayName() {
			return "Test stabbi";
		}
	}
}

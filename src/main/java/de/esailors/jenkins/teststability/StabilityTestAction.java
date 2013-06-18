package de.esailors.jenkins.teststability;

import org.jvnet.localizer.Localizable;

import hudson.model.HealthReport;
import hudson.tasks.junit.TestAction;
import de.esailors.jenkins.teststability.StabilityTestData.Result;

class StabilityTestAction extends TestAction {

	private CircularStabilityHistory ringBuffer;
	private String description;

	public StabilityTestAction(CircularStabilityHistory ringBuffer) {
		this.ringBuffer = ringBuffer;
		
		if (this.ringBuffer == null || this.ringBuffer.isEmpty()) {
			this.description = "No known failures. Stability 100 %";
		} else {
			int total = 0, failed = 0;
			for (Result r : this.ringBuffer.getData()) {
				total++;
				if (!r.passed) {
					failed++;
				}
			}
			
			double stability = 100 * (total - failed) / total; 
			
			this.description =
					String.format("Failed %d times in the last %d runs. Stability: %.0f %%", failed, total, stability);
		}
	}
	
	private int getStability() {
		
		if (ringBuffer == null) {
			return 100;
		}
		
		int total = 0, failed = 0;
		for (Result r : this.ringBuffer.getData()) {
			total++;
			if (!r.passed) {
				failed++;
			}
		}
		
		int stability = 100 * (total - failed) / total;
		return stability;
	}
	
	public String getBigImagePath() {
		HealthReport healthReport = new HealthReport(getStability(), (Localizable)null);
		return healthReport.getIconUrl("32x32");
	}
	
	public String getSmallImagePath() {
		HealthReport healthReport = new HealthReport(getStability(), (Localizable)null);
		return healthReport.getIconUrl("16x16");
	}

	public CircularStabilityHistory getRingBuffer() {
		// TODO: only publish an immutable view of the buffer!
		return this.ringBuffer;
	}

	public String getDescription() {
		return this.description;
	}
	
	public String getIconFileName() {
		return null;
	}
	
	public String getDisplayName() {
		return null;
	}

	public String getUrlName() {
		return null;
	}
	
}
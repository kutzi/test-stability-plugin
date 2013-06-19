/*
 * The MIT License
 * 
 * Copyright (c) 2013, eSailors IT Solutions GmbH
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
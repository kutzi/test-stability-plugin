package de.esailors.jenkins.teststability;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.Launcher;
import hudson.maven.MavenBuild;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.maven.MavenTestDataPublisher;
import hudson.maven.reporters.SurefireReport;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.model.Saveable;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.tasks.junit.TestDataPublisher;
import hudson.tasks.junit.TestResultAction.Data;
import hudson.util.DescribableList;

public class FixedMavenTestDataPublisher extends MavenTestDataPublisher {

	public FixedMavenTestDataPublisher(
			DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>> testDataPublishers) {
		super(testDataPublishers);
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {
		
		MavenModuleSetBuild msb = (MavenModuleSetBuild) build;
		
		Map<MavenModule, MavenBuild> moduleLastBuilds = msb.getModuleLastBuilds();
		
		for (MavenBuild moduleBuild : moduleLastBuilds.values()) {
		
			SurefireReport report = moduleBuild.getAction(SurefireReport.class);
			if (report == null) {
				return true;
			}
			
			List<Data> data = new ArrayList<Data>();
			if (getTestDataPublishers() != null) {
				for (TestDataPublisher tdp : getTestDataPublishers()) {
					Data d = tdp.getTestData(build, launcher, listener, report.getResult());
					if (d != null) {
						data.add(d);
					}
				}
			}
			
			report.setData(data);
			moduleBuild.save();
		}

		return true;
	}
	
	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		@Override
		public String getDisplayName() {
			return "Additional test report features (fixed)";
		}

		@SuppressWarnings("rawtypes")
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return MavenModuleSet.class.isAssignableFrom(jobType) && !TestDataPublisher.all().isEmpty();
		}
		
		@Override
		public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
			DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>> testDataPublishers
                    = new DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>>(Saveable.NOOP);
            try {
                testDataPublishers.rebuild(req, formData, TestDataPublisher.all());
            } catch (IOException e) {
                throw new FormException(e,null);
            }

            return new FixedMavenTestDataPublisher(testDataPublishers);
		}
		
	}
	

}

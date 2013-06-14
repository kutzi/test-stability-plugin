package de.esailors.jenkins.teststability;

import hudson.model.AbstractBuild;
import hudson.tasks.junit.TestAction;
import hudson.tasks.junit.TestObject;
import hudson.tasks.junit.TestResultAction.Data;
import hudson.tasks.junit.CaseResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;

class StabilityTestData extends Data {
	
	private final Map<String,CircularBuffer> stability;
	
	private transient AbstractBuild<?,?> build;

	public StabilityTestData(Map<String, CircularBuffer> stabilityHistory) {
		this.stability = stabilityHistory;
	}

	@SuppressWarnings("deprecation")
	@Override
	public List<? extends TestAction> getTestAction(TestObject testObject) {
		
		if (testObject instanceof CaseResult) {
			CaseResult cr = (CaseResult) testObject;
			CircularBuffer ringBuffer = stability.get(cr.getId());
			return Collections.singletonList(new StabilityTestAction(ringBuffer));
		}
		
		return Collections.emptyList();
	}
	
	
	
	public static class CircularBuffer {
		  private boolean data[];
		  private int head; 
		  private int tail;
		  // number of elements in queue
	      private int size = 0; 

		  public CircularBuffer(int size) {
		    data = new boolean[size];
		    head = 0;
		    tail = 0;
		  }

		  public boolean add(boolean value) {
		      data[tail] = value;
		      tail++;
		      if (tail == data.length) {
		        tail = 0;
		      }
		      
		      if (size == data.length) {  
	                head = (head + 1) % data.length;  
	           } else {  
	                size++;  
	           }  
		      return true;
		  }
		  
		  public boolean[] getData() {
			  boolean[] copy = new boolean[size];
			  
			  for (int i = 0; i < size; i++) {
				  copy[i] = data[(head + i) % data.length];
			  }
			  return copy;
		  }

		public boolean isEmpty() {
			return data.length == 0;
		}
		
		// TODO: minimal serialization format!
	}
	
}

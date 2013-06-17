package de.esailors.jenkins.teststability;

import hudson.tasks.junit.TestAction;
import hudson.tasks.junit.TestObject;
import hudson.tasks.junit.TestResultAction.Data;
import hudson.tasks.junit.CaseResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import jenkins.model.Jenkins;

class StabilityTestData extends Data {
	
	private final Map<String,CircularBuffer> stability;
	
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
		
		static {
			Jenkins.XSTREAM2.registerConverter(new ConverterImpl());
		}
		
		public static class ConverterImpl implements Converter {

			public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
				return CircularBuffer.class.isAssignableFrom(type);
			}

			public void marshal(Object source, HierarchicalStreamWriter writer,
					MarshallingContext context) {
				CircularBuffer b = (CircularBuffer) source;
				
				writer.startNode("head");
				writer.setValue(Integer.toString(b.head));
				writer.endNode();
				
				writer.startNode("tail");
				writer.setValue(Integer.toString(b.tail));
				writer.endNode();

				writer.startNode("size");
				writer.setValue(Integer.toString(b.size));
				writer.endNode();
				
				writer.startNode("data");
				writer.setValue(dataToString(b.data));
				writer.endNode();
			}
			
			
			private String dataToString(boolean[] data) {
				StringBuilder buf = new StringBuilder();
				for (boolean b : data) {
					if (b) {
						buf.append("1,");
					} else {
						buf.append("0,");
					}
				}
				
				if (buf.length() > 0) {
					buf.deleteCharAt(buf.length() - 1);
				}
				
				return buf.toString();
			}

			public CircularBuffer unmarshal(HierarchicalStreamReader r,
					UnmarshallingContext context) {
				
				r.moveDown();
				int head = Integer.parseInt(r.getValue());
				r.moveUp();
				
				r.moveDown();
				int tail = Integer.parseInt(r.getValue());
				r.moveUp();
				
				r.moveDown();
				int size = Integer.parseInt(r.getValue());
				r.moveUp();
				
				r.moveDown();
				String data = r.getValue();
				r.moveUp();
				
				CircularBuffer buf = new CircularBuffer();
				boolean[] b = stringToData(data);
				
				buf.data = b;
				buf.head = head;
				buf.size = size;
				buf.tail = tail;
				
				return buf;
			}
			
			private  boolean[] stringToData(String s) {
				String[] split = s.split(",");
				boolean d[] = new boolean[split.length];
				
				int i = 0;
				for(String b : split) {
					if ("1".equals(b)) {
						d[i] = true;
					} else if ("0".equals(b)) {
						d[i] = false;
					} else {
						System.err.println("Invalid value: " + b); 
					}
					i++;
				}
				
				return d;
			}

		}
		
		  private transient boolean data[];
		  private int head; 
		  private int tail;
		  // number of elements in queue
	      private int size = 0; 

	      private CircularBuffer() {}
	      
		  public CircularBuffer(int maxSize) {
		    data = new boolean[maxSize];
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
	}
	
	
}

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.junit.Test;

import com.google.common.base.Function;

import cpm.Activity;
import cpm.Event;
import plainjavaubt.gantt2cpm.Gantt2Cpm;

public class Gantt2CpmTest {
	@Test
	public void testTransform() {
		File source = new File("test/GanttDiagram.xmi");
		File expected = new File("test/CPMNetwork.xmi");
		Function<EObject, String> idFunction = new Function<EObject, String>() {
			public String apply(EObject input) {
				if (input instanceof Event) {
					List<String> outgoingNames = new ArrayList<>();
					for (Activity outgoing : ((Event) input).getOutgoingActivities()) {
						outgoingNames.add(outgoing.getName());
					}
					Collections.sort(outgoingNames);
					
					List<String> incomingNames = new ArrayList<>();
					for (Activity incoming : ((Event) input).getIncomingActivities()) {
						incomingNames.add(incoming.getName());
					}
					Collections.sort(incomingNames);
					
					String result = new String();
					for (String name : outgoingNames) {
						result += name;
					}
					for (String name : incomingNames) {
						result += name;
					}
					
					return result;
				}
				
				return null;
			}
		};
		Gantt2Cpm trafo = new Gantt2Cpm();	
		new plainjavaubt.util.test.TestableTransformationTest().testTransform(source, expected, idFunction, trafo);
	}
}

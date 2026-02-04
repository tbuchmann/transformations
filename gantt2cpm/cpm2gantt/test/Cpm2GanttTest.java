import java.io.File;

import org.junit.Test;

import plainjavaubt.cpm2gantt.Cpm2Gantt;

public class Cpm2GanttTest {
	@Test
	public void testTransform() {
		File source = new File("test/CPMNetwork.xmi");
		File expected = new File("test/GanttDiagram.xmi");
		Cpm2Gantt trafo = new Cpm2Gantt();	
		new plainjavaubt.util.test.TestableTransformationTest().testTransform(source, expected, trafo);
	}
}

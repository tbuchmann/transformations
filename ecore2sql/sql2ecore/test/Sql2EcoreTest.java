import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.eclipse.emf.ecore.EPackage;
import org.junit.Test;

import sql2ecore.Sql2Ecore;
import plainjavaubt.util.io.Xmi;

public class Sql2EcoreTest {
	@Test
	public void testTransform() {
		File source = new File("test/CampusManagement.xmi");
		File expected = new File("test/CampusManagement.ecore");
		Sql2Ecore trafo = new Sql2Ecore();	
		
		File target;
		try {
			long before = System.nanoTime();
			target = trafo.transform(source);
			long duration = (System.nanoTime() - before) / 1000000;
			System.out.println(trafo.getClass().getSimpleName() + " duration: "  + (duration));
		} catch (IOException e) {
			fail("Saving xmi-file failed.");
			return;
		}
		
		EPackage root = (EPackage) Xmi.load(target);
		root.setNsPrefix("cm");
		root.setNsURI("http://de.ubt.ai1.bw.qvt.examples.cm.ecore");
		try {
			Xmi.save(target, root);
		} catch (IOException e) {
			fail("Saving xmi-file failed.");
			return;
		}
		
		assertFalse(plainjavaubt.util.io.Xmi.differ(target, expected, null));
	}
}

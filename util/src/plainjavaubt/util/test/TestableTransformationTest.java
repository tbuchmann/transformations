package plainjavaubt.util.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.eclipse.emf.ecore.EObject;
import org.junit.Test;

import com.google.common.base.Function;

/**
 * Provides a generic test for TestableTransformations.
 */
public class TestableTransformationTest {
	@Test
	public void testTransform(File source, File expected, TestableTransformation trafo) {
		testTransform(source, expected, null, trafo);
	}
	
	@Test
	public void testTransform(File source, File expected, Function<EObject, String> idFunction,
			TestableTransformation trafo) {
		File target;
		try {
			long before = System.nanoTime();
			target = trafo.transform(source);
			long duration = (System.nanoTime() - before) / 1000000;
			System.out.println(trafo.getClass().getSimpleName() + " duration: " + (duration));
		} catch (IOException e) {
			fail("Saving xmi-file failed.");
			return;
		}
		
		assertFalse(plainjavaubt.util.io.Xmi.differ(target, expected, idFunction));
	}
}

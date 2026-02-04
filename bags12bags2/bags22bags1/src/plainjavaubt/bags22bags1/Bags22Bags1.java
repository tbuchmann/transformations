package plainjavaubt.bags22bags1;

import java.io.File;
import java.io.IOException;

import bags1.Bags1Factory;
import bags1.Bags1Package;
import bags2.Bags2Package;
import plainjavaubt.util.test.BXToolTransformation;
import plainjavaubt.util.test.TestableTransformation;
import plainjavaubt.util.trafo.Transformation;

/**
 * Converts bags2 into bags1 xmi-files.
 */
public class Bags22Bags1 implements TestableTransformation, BXToolTransformation {
	/**
	 * Converts the given bags2 xmi-file to the corresponding bags1 xmi-file. The new bags1 xmi-file is saved on the
	 * same location as the bags2 xmi-file with the Postfix Transformed.
	 * 
	 * @param bags2 xmi-file to transform.
	 * @return the location of the created bags1 xmi-file.
	 * @throws IOException if saving fails.
	 */
	public File transform(File bags2) throws IOException {
		bags2.MyBag source = null;
		source = Transformation.prepareTransformation(bags2, Bags2Package.eINSTANCE, Bags1Package.eINSTANCE, source);
		bags1.MyBag target = performTransformation(source);
		return Transformation.finishTransformation(bags2, target);
	}
	
	/**
	 * Works like the one parameter version, but you can also specify the target file.
	 */
	public File transform(File bags2, File bags1) throws IOException {
		bags2.MyBag source = null;
		source = Transformation.prepareTransformation(bags2, Bags2Package.eINSTANCE, Bags1Package.eINSTANCE, source);
		bags1.MyBag target = performTransformation(source);
		plainjavaubt.util.io.Xmi.save(bags1, target);
		return bags1;
	}

	private bags1.MyBag performTransformation(bags2.MyBag source) {
		Bags1Factory factory = Bags1Factory.eINSTANCE;
		bags1.MyBag target = factory.createMyBag();
		
		for (bags2.Element elementSource : source.getElements()) {
			for (int i = 0; i < elementSource.getMultiplicity(); i++) {
				bags1.Element elementTarget = factory.createElement();
				elementTarget.setValue(elementSource.getValue());
				elementTarget.setBag(target);
				target.getElements().add(elementTarget);
			}		
		}
		
		return target;
	}
}

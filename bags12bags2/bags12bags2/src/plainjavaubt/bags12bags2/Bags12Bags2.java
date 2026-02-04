package plainjavaubt.bags12bags2;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import bags1.Bags1Package;
import bags2.Bags2Factory;
import bags2.Bags2Package;
import plainjavaubt.util.test.BXToolTransformation;
import plainjavaubt.util.test.TestableTransformation;
import plainjavaubt.util.trafo.Transformation;

/**
 * Converts bags1 into bags2 xmi-files.
 */
public class Bags12Bags2 implements TestableTransformation, BXToolTransformation {
	/**
	 * Converts the given bags1 xmi-file to the corresponding bags2 xmi-file. The new bags2 xmi-file is saved on the
	 * same location as the bags1 xmi-file with the Postfix Transformed.
	 * 
	 * @param bags1 xmi-file to transform.
	 * @return the location of the created bags2 xmi-file.
	 * @throws IOException if saving fails.
	 */
	public File transform(File bags1) throws IOException {
		bags1.MyBag source = null;
		source = Transformation.prepareTransformation(bags1, Bags1Package.eINSTANCE, Bags2Package.eINSTANCE, source);
		bags2.MyBag target = performTransformation(source);
		return Transformation.finishTransformation(bags1, target);
	}
	
	/**
	 * Works like the one parameter version, but you can also specify the target file.
	 */
	public File transform(File bags1, File bags2) throws IOException {
		bags1.MyBag source = null;
		source = Transformation.prepareTransformation(bags1, Bags1Package.eINSTANCE, Bags2Package.eINSTANCE, source);
		bags2.MyBag target = performTransformation(source);
		plainjavaubt.util.io.Xmi.save(bags2, target);
		return bags2;
	}
	
	private bags2.MyBag performTransformation(bags1.MyBag source) {
		Bags2Factory factory = Bags2Factory.eINSTANCE;
		bags2.MyBag target = factory.createMyBag();
		
		Map<String, bags2.Element> containedElements = new HashMap<>();
		for (bags1.Element elementSource : source.getElements()) {
			if (containedElements.containsKey(elementSource.getValue())) {
				bags2.Element elementTarget = containedElements.get(elementSource.getValue());
				int multiplicity = elementTarget.getMultiplicity();
				multiplicity++;
				elementTarget.setMultiplicity(multiplicity);
			} else {	
				bags2.Element elementTarget = factory.createElement();
				elementTarget.setValue(elementSource.getValue());
				elementTarget.setMultiplicity(1);
				elementTarget.setBag(target);
				target.getElements().add(elementTarget);
				containedElements.put(elementTarget.getValue(), elementTarget);
			}
		}
		
		return target;
	}
}

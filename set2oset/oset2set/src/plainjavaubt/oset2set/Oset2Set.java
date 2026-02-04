package plainjavaubt.oset2set;

import java.io.File;
import java.io.IOException;

import osets.OsetsPackage;
import plainjavaubt.util.test.BXToolTransformation;
import plainjavaubt.util.test.TestableTransformation;
import plainjavaubt.util.trafo.Transformation;
import sets.SetsFactory;
import sets.SetsPackage;

/**
 * Converts osets into sets xmi-files.
 */
public class Oset2Set implements TestableTransformation, BXToolTransformation {
	/**
	 * Converts the given osets xmi-file to the corresponding sets xmi-file. The new sets xmi-file is saved on the
	 * same location as the osets xmi-file with the Postfix Transformed.
	 * 
	 * @param osets xmi-file to transform.
	 * @return the location of the created sets xmi-file.
	 * @throws IOException if saving fails.
	 */
	public File transform(File osets) throws IOException {
		osets.MyOrderedSet source = null;
		source = Transformation.prepareTransformation(osets, OsetsPackage.eINSTANCE, SetsPackage.eINSTANCE, source);
		sets.MySet target = performTransformation(source);
		return Transformation.finishTransformation(osets, target);
	}
	
	/**
	 * Works like the one parameter version, but you can also specify the target file.
	 */
	public File transform(File osets, File sets) throws IOException {
		osets.MyOrderedSet source = null;
		source = Transformation.prepareTransformation(osets, OsetsPackage.eINSTANCE, SetsPackage.eINSTANCE, source);
		sets.MySet target = performTransformation(source);
		plainjavaubt.util.io.Xmi.save(sets, target);
		return sets;
	}
	
	private sets.MySet performTransformation(osets.MyOrderedSet source) {
		SetsFactory factory = SetsFactory.eINSTANCE;
		sets.MySet target = factory.createMySet();
		
		target.setName(source.getName());
		for (osets.Element elementSource : source.getElements()) {
			sets.Element elementTarget = factory.createElement();
			elementTarget.setValue(elementSource.getValue());
			elementTarget.setSet(target);
			target.getElements().add(elementTarget);
		}
		
		return target;
	}
}

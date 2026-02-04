package plainjavaubt.set2oset;

import java.io.File;
import java.io.IOException;

import osets.OsetsFactory;
import osets.OsetsPackage;
import plainjavaubt.util.test.BXToolTransformation;
import plainjavaubt.util.test.TestableTransformation;
import plainjavaubt.util.trafo.Transformation;
import sets.SetsPackage;

/**
 * Converts sets into osets xmi-files.
 */
public class Set2Oset implements TestableTransformation, BXToolTransformation {
	/**
	 * Converts the given sets xmi-file to the corresponding osets xmi-file. The new osets xmi-file is saved on the
	 * same location as the sets xmi-file with the Postfix Transformed.
	 * 
	 * @param sets xmi-file to transform.
	 * @return the location of the created osets xmi-file.
	 * @throws IOException if saving fails.
	 */
	public File transform(File sets) throws IOException {
		sets.MySet source = null;
		source = Transformation.prepareTransformation(sets, SetsPackage.eINSTANCE, OsetsPackage.eINSTANCE, source);
		osets.MyOrderedSet target = performTransformation(source);
		return Transformation.finishTransformation(sets, target);
	}
	
	/**
	 * Works like the one parameter version, but you can also specify the target file.
	 */
	public File transform(File sets, File ostes) throws IOException {
		sets.MySet source = null;
		source = Transformation.prepareTransformation(sets, SetsPackage.eINSTANCE, OsetsPackage.eINSTANCE, source);
		osets.MyOrderedSet target = performTransformation(source);
		plainjavaubt.util.io.Xmi.save(ostes, target);
		return ostes;
	}
	
	private osets.MyOrderedSet performTransformation(sets.MySet source) {
		OsetsFactory factory = OsetsFactory.eINSTANCE;
		osets.MyOrderedSet target = factory.createMyOrderedSet();
		
		target.setName(source.getName());
		osets.Element lastAdded = null;
		for (sets.Element elementSource : source.getElements()) {
			osets.Element elementTarget = factory.createElement();
			elementTarget.setValue(elementSource.getValue());
			if (lastAdded != null) {
				lastAdded.setNext(elementTarget);
				elementTarget.setPrevious(lastAdded);
			}		
			lastAdded = elementTarget;
			elementTarget.setOrderedSet(target);
			target.getElements().add(elementTarget);
		}
		
		return target;
	}
}

package plainjavaubt.pdb12pdb2;

import java.io.File;
import java.io.IOException;

import pdb1.Pdb1Package;
import pdb2.Pdb2Factory;
import pdb2.Pdb2Package;
import plainjavaubt.util.test.BXToolTransformation;
import plainjavaubt.util.test.TestableTransformation;
import plainjavaubt.util.trafo.Transformation;

/**
 * Converts pdb1 into pdb2 xmi-files.
 */
public class Pdb12Pdb2 implements TestableTransformation, BXToolTransformation {
	/**
	 * Converts the given pdb1 xmi-file to the corresponding pdb2 xmi-file. The new pdb2 xmi-file is saved on the same
	 * location as the pdb1 xmi-file with the Postfix Transformed.
	 * 
	 * @param pdb1 xmi-file to transform.
	 * @return the location of the created pdb2 xmi-file.
	 * @throws IOException if saving fails.
	 */
	public File transform(File pdb1) throws IOException {
		pdb1.Database source = null;
		source = Transformation.prepareTransformation(pdb1, Pdb1Package.eINSTANCE, Pdb2Package.eINSTANCE, source);
		pdb2.Database target = performTransformation(source);
		return Transformation.finishTransformation(pdb1, target);
	}
	
	/**
	 * Works like the one parameter version, but you can also specify the target file.
	 */
	public File transform(File pdb1, File pdb2) throws IOException {
		pdb1.Database source = null;
		source = Transformation.prepareTransformation(pdb1, Pdb1Package.eINSTANCE, Pdb2Package.eINSTANCE, source);
		pdb2.Database target = performTransformation(source);
		plainjavaubt.util.io.Xmi.save(pdb2, target);
		return pdb2;
	}
	
	private pdb2.Database performTransformation(pdb1.Database source) {
		Pdb2Factory factory = Pdb2Factory.eINSTANCE;
		pdb2.Database target = factory.createDatabase();
		
		target.setName(source.getName());
		for (pdb1.Person personSource : source.getPersons()) {
			pdb2.Person personTarget = factory.createPerson();
			personTarget.setName(personSource.getFirstName() + " " + personSource.getLastName());
			personTarget.setBirthday(personSource.getBirthday());
			personTarget.setPlaceOfBirth(personSource.getPlaceOfBirth());
			personTarget.setId(personSource.getId());
			personTarget.setDatabase(target);
			target.getPersons().add(personTarget);
		}
		
		return target;
	}
}

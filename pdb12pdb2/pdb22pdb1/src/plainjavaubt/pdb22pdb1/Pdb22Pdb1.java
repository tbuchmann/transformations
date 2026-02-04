package plainjavaubt.pdb22pdb1;

import java.io.File;
import java.io.IOException;

import pdb1.Pdb1Factory;
import pdb1.Pdb1Package;
import pdb2.Pdb2Package;
import plainjavaubt.util.test.BXToolTransformation;
import plainjavaubt.util.test.TestableTransformation;
import plainjavaubt.util.trafo.Transformation;

/**
 * Converts pdb2 into pdb1 xmi-files.
 */
public class Pdb22Pdb1 implements TestableTransformation, BXToolTransformation  {
	/**
	 * Converts the given pdb2 xmi-file to the corresponding pdb1 xmi-file. The new pdb1 xmi-file is saved on the same
	 * location as the pdb2 xmi-file with the Postfix Transformed.
	 * 
	 * @param pdb2 xmi-file to transform.
	 * @return the location of the created pdb1 xmi-file.
	 * @throws IOException if saving fails.
	 */
	public File transform(File pdb2) throws IOException {
		pdb2.Database source = null;
		source = Transformation.prepareTransformation(pdb2, Pdb2Package.eINSTANCE, Pdb1Package.eINSTANCE, source);
		pdb1.Database target = performTransformation(source);
		return Transformation.finishTransformation(pdb2, target);
	}
	
	/**
	 * Works like the one parameter version, but you can also specify the target file.
	 */
	public File transform(File pdb2, File pdb1) throws IOException {
		pdb2.Database source = null;
		source = Transformation.prepareTransformation(pdb2, Pdb2Package.eINSTANCE, Pdb1Package.eINSTANCE, source);
		pdb1.Database target = performTransformation(source);
		plainjavaubt.util.io.Xmi.save(pdb1, target);
		return pdb1;
	}
	
	private pdb1.Database performTransformation(pdb2.Database source) {
		Pdb1Factory factory = Pdb1Factory.eINSTANCE;
		pdb1.Database target = factory.createDatabase();
		
		target.setName(source.getName());
		for (pdb2.Person personSource : source.getPersons()) {
			pdb1.Person personTarget = factory.createPerson();
			String name = personSource.getName();
			personTarget.setFirstName(name.substring(0, name.indexOf(' ')));
			personTarget.setLastName(name.substring(name.indexOf(' ') + 1));
			personTarget.setBirthday(personSource.getBirthday());
			personTarget.setPlaceOfBirth(personSource.getPlaceOfBirth());
			personTarget.setId(personSource.getId());
			personTarget.setDatabase(target);
			target.getPersons().add(personTarget);
		}
		
		return target;
	}
}

package plainjavaubt.util.trafo;

import java.io.File;
import java.io.IOException;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.util.Diagnostician;

/**
 * Provides methods to prepare and finish transformations.
 */
public final class Transformation {	
	public static boolean validateInput = true;
	
	@SuppressWarnings("unchecked")
	public static <T extends EObject> T prepareTransformation(File in, EPackage srcPackage, EPackage tarPackage, T source) {
		if (!in.getName().endsWith(".xmi") && !in.getName().endsWith(".ecore")) {
			throw new IllegalArgumentException("Only xmi- and ecore-files can be transformed.");
		}
		
		srcPackage.eClass();
		source = (T) plainjavaubt.util.io.Xmi.load(in);		
		if (validateInput && Diagnostician.INSTANCE.validate(source).getSeverity() != Diagnostic.OK) {
			throw new IllegalArgumentException("The loaded xmi is no valid instance of its ecore model.");
		}
		
		tarPackage.eClass();
		
		return source;
	}
	
	public static <T extends EObject> File finishTransformation(File in, T target) throws IOException {
		String sourcePath = in.getAbsolutePath();
		String extension = target instanceof EPackage ? "ecore" : "xmi";
		File transformed = new File(sourcePath.substring(0, sourcePath.lastIndexOf('.')) + "Transformed." + extension);
		plainjavaubt.util.io.Xmi.save(transformed, target);
		return transformed;
	}
	
	private Transformation() {	
	}
}
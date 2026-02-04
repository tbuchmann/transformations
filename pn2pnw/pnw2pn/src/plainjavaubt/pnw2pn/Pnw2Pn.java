package plainjavaubt.pnw2pn;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import plainjavaubt.util.test.BXToolTransformation;
import plainjavaubt.util.test.TestableTransformation;
import plainjavaubt.util.trafo.Transformation;
import pn.PnFactory;
import pn.PnPackage;
import pnw.PnwPackage;

/**
 * Converts pnw into pn xmi-files.
 */
public class Pnw2Pn implements TestableTransformation, BXToolTransformation {
	/**
	 * Converts the given pnw xmi-file to the corresponding pn xmi-file. The new pn xmi-file is saved on the same
	 * location as the pnw xmi-file with the Postfix Transformed.
	 * 
	 * @param pnw xmi-file to transform.
	 * @return the location of the created pn xmi-file.
	 * @throws IOException if saving fails.
	 */
	public File transform(File pnw) throws IOException {
		pnw.Net source = null;
		source = Transformation.prepareTransformation(pnw, PnwPackage.eINSTANCE, PnPackage.eINSTANCE, source);
		pn.Net target = performTransformation(source);
		return Transformation.finishTransformation(pnw, target);
	}
	
	/**
	 * Works like the one parameter version, but you can also specify the target file.
	 */
	public File transform(File pnw, File pn) throws IOException {
		pnw.Net source = null;
		source = Transformation.prepareTransformation(pnw, PnwPackage.eINSTANCE, PnPackage.eINSTANCE, source);
		pn.Net target = performTransformation(source);
		plainjavaubt.util.io.Xmi.save(pn, target);
		return pn;
	}
	
	private pn.Net performTransformation(pnw.Net source) {
		PnFactory factory = PnFactory.eINSTANCE;
		pn.Net target = factory.createNet();
		
		target.setName(source.getName());
		HashMap<String, pn.Transition> transitions = new HashMap<>();
		HashMap<String, pn.Place> places = new HashMap<>();
		for (pnw.NetElement elementSource : source.getElements()) {
			if (elementSource instanceof pnw.Transition) {
				pn.Transition transitionTarget = factory.createTransition();
				transitionTarget.setName(((pnw.Transition) elementSource).getName());
				transitionTarget.setNet(target);
				transitions.put(((pnw.Transition) elementSource).getName(), transitionTarget);
			}
			
			if (elementSource instanceof pnw.Place) {
				pn.Place placeTarget = factory.createPlace();
				placeTarget.setName(((pnw.Place) elementSource).getName());
				placeTarget.setNoOfTokens(((pnw.Place) elementSource).getNoOfTokens());
				placeTarget.setNet(target);
				places.put(((pnw.Place) elementSource).getName(), placeTarget);
			}
		}
		
		for (pnw.NetElement elementSource : source.getElements()) {
			if (elementSource instanceof pnw.TPEdge) {
				pn.Place place = places.get(((pnw.TPEdge) elementSource).getToPlace().getName());
				pn.Transition transiton = transitions.get(((pnw.TPEdge) elementSource).getFromTransition().getName());
				place.getSrcT2P().add(transiton);
			}
			
			if (elementSource instanceof pnw.PTEdge) {
				pn.Place place = places.get(((pnw.PTEdge) elementSource).getFromPlace().getName());
				pn.Transition transiton = transitions.get(((pnw.PTEdge) elementSource).getToTransition().getName());
				place.getTrgP2T().add(transiton);
			}
		}
		
		return target;
	}
}

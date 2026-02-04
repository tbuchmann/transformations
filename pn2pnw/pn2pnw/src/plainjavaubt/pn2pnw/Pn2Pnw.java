package plainjavaubt.pn2pnw;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import plainjavaubt.util.test.BXToolTransformation;
import plainjavaubt.util.test.TestableTransformation;
import plainjavaubt.util.trafo.Transformation;
import pn.PnPackage;
import pnw.PnwFactory;
import pnw.PnwPackage;

/**
 * Converts pn into pnw xmi-files.
 */
public class Pn2Pnw implements TestableTransformation, BXToolTransformation {
	/**
	 * Converts the given pn xmi-file to the corresponding pnw xmi-file. The new pnw xmi-file is saved on the same
	 * location as the pn xmi-file with the Postfix Transformed.
	 * 
	 * @param pn xmi-file to transform.
	 * @return the location of the created pnw xmi-file.
	 * @throws IOException if saving fails.
	 */
	public File transform(File pn) throws IOException {
		pn.Net source = null;
		source = Transformation.prepareTransformation(pn, PnPackage.eINSTANCE, PnwPackage.eINSTANCE, source);
		pnw.Net target = performTransformation(source);
		return Transformation.finishTransformation(pn, target);
	}
	
	/**
	 * Works like the one parameter version, but you can also specify the target file.
	 */
	public File transform(File pn, File pnw) throws IOException {
		pn.Net source = null;
		source = Transformation.prepareTransformation(pn, PnPackage.eINSTANCE, PnwPackage.eINSTANCE, source);
		pnw.Net target = performTransformation(source);
		plainjavaubt.util.io.Xmi.save(pnw, target);
		return pnw;
	}
	
	private pnw.Net performTransformation(pn.Net source) {
		PnwFactory factory = PnwFactory.eINSTANCE;
		pnw.Net target = factory.createNet();
		
		target.setName(source.getName());
		HashMap<String, pnw.Transition> transitions = new HashMap<>();
		for (pn.NetElement transitionSource : source.getElements()) {
			if (transitionSource instanceof pn.Transition) {
				pnw.Transition transitionTarget = factory.createTransition();
				transitionTarget.setName(transitionSource.getName());
				transitionTarget.setNet(target);
				transitions.put(transitionSource.getName(), transitionTarget);
			}
		}
		
		for (pn.NetElement placeSource : source.getElements()) {
			if (placeSource instanceof pn.Place) {
				pnw.Place placeTarget = factory.createPlace();
				placeTarget.setName(placeSource.getName());
				placeTarget.setNoOfTokens(((pn.Place) placeSource).getNoOfTokens());
				placeTarget.setNet(target);
				
				for (pn.Transition transitionSource : ((pn.Place) placeSource).getSrcT2P()) {
					pnw.TPEdge edge = factory.createTPEdge();	
					edge.setFromTransition(transitions.get(transitionSource.getName()));
					edge.setToPlace(placeTarget); 
					edge.setNet(target);
				}
				
				for (pn.Transition transitionSource : ((pn.Place) placeSource).getTrgP2T()) {
					pnw.PTEdge edge = factory.createPTEdge();
					edge.setFromPlace(placeTarget);
					edge.setToTransition(transitions.get(transitionSource.getName()));
					edge.setNet(target);
				}
			}
		}
		
		return target;
	}
}

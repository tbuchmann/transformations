package plainjavaubt.util.io;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.diff.DefaultDiffEngine;
import org.eclipse.emf.compare.diff.DiffBuilder;
import org.eclipse.emf.compare.diff.FeatureFilter;
import org.eclipse.emf.compare.diff.IDiffEngine;
import org.eclipse.emf.compare.diff.IDiffProcessor;
import org.eclipse.emf.compare.match.DefaultComparisonFactory;
import org.eclipse.emf.compare.match.DefaultEqualityHelperFactory;
import org.eclipse.emf.compare.match.DefaultMatchEngine;
import org.eclipse.emf.compare.match.IComparisonFactory;
import org.eclipse.emf.compare.match.IMatchEngine;
import org.eclipse.emf.compare.match.eobject.IEObjectMatcher;
import org.eclipse.emf.compare.match.eobject.IdentifierEObjectMatcher;
import org.eclipse.emf.compare.match.impl.MatchEngineFactoryImpl;
import org.eclipse.emf.compare.match.impl.MatchEngineFactoryRegistryImpl;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.compare.utils.UseIdentifiers;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import com.google.common.base.Function;

/**
 * Provides methods to work with xmi-files, e.g. loading and saving instances of ecore models into xmi-files.
 */
public final class Xmi {
	static {
		// discards the complete log4j output, which is used by org.eclipse.emf.compare
		org.apache.log4j.BasicConfigurator.configure(new org.apache.log4j.varia.NullAppender());
	}
	
	private Xmi() {	
	}
	
	/**
	 * Loads an ecore model instance from the given path.
	 * 
	 * @param path of the xmi-file to load.
	 * @return the EObject representing the root of the loaded xmi.
	 */
	public static EObject load(File path) {
		ResourceSet resSet = new ResourceSetImpl();
		resSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
		resSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore", new XMIResourceFactoryImpl());
		URI fileURI = URI.createFileURI(path.getAbsolutePath());
		Resource resource = resSet.getResource(fileURI, true);
		
		return resource.getContents().get(0);
	}
	
	/**
	 * Saves an ecore model instance at the given path.
	 * 
	 * @param <T> class of the root element.
	 * @param path where to save the xmi-file.
	 * @param root of the ecore model instance to save.
	 * @throws IOException if saving fails.
	 */
	public static <T extends EObject> void save(File path, T root) throws IOException {
		ResourceSet resSet = new ResourceSetImpl();
		resSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
		resSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore", new XMIResourceFactoryImpl());
		URI fileURI = URI.createFileURI(path.getAbsolutePath());
		Resource resource = resSet.createResource(fileURI);
				
		HashMap<String, Object> xmiOptions = new HashMap<String, Object>();
		xmiOptions.put(XMIResource.OPTION_SCHEMA_LOCATION, true);
		xmiOptions.put(XMIResource.OPTION_ENCODING, "UTF-8");
		resource.getContents().add(root);
		resource.save(xmiOptions);
	}
	
	/**
	 * Indicates, if there is at least one difference between the instances represented by the given xmi-files. The
	 * idFunction can be used to assign some objects custom identifiers. If idFunction == null, no object has a custom
	 * identifier.
	 * 
	 * @param lhs xmi-file to compare.
	 * @param rhs xmi-file to compare.
	 * @param idFunction to assign some objects custom identifiers.
	 * @return true, if there is at least one difference.
	 */
	public static boolean differ(File lhs, File rhs, Function<EObject, String> idFunction) {
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new XMIResourceFactoryImpl());
		
		ResourceSet resSetLhs = new ResourceSetImpl();
		ResourceSet resSetRhs = new ResourceSetImpl();
		resSetLhs.getResource(URI.createFileURI(lhs.getAbsolutePath()), true);
		resSetRhs.getResource(URI.createFileURI(rhs.getAbsolutePath()), true);
		
		IComparisonScope scope = new DefaultComparisonScope(resSetLhs, resSetRhs, null);
		
		// code using the id function to match objects
		if (idFunction == null) {
			idFunction = new Function<EObject, String>() {
				public String apply(EObject input) {
					return null;
				}
			};
		}
		
		IEObjectMatcher fallBackMatcher = DefaultMatchEngine.createDefaultEObjectMatcher(UseIdentifiers.WHEN_AVAILABLE);
		IEObjectMatcher customIDMatcher = new IdentifierEObjectMatcher(fallBackMatcher, idFunction);
		 
		IComparisonFactory comparisonFactory = new DefaultComparisonFactory(new DefaultEqualityHelperFactory());
		 
		IMatchEngine.Factory.Registry registry = MatchEngineFactoryRegistryImpl.createStandaloneInstance();
		final MatchEngineFactoryImpl matchEngineFactory = new MatchEngineFactoryImpl(
				customIDMatcher, comparisonFactory);
		matchEngineFactory.setRanking(20);
		registry.add(matchEngineFactory);
		
//		Comparison comparison = EMFCompare.builder().setMatchEngineFactoryRegistry(registry).build().compare(scope);
		
		// code to disable checkForOrderingChanges
		IDiffProcessor diffProcessor = new DiffBuilder();
		IDiffEngine diffEngine = new DefaultDiffEngine(diffProcessor) {
			@Override
			protected FeatureFilter createFeatureFilter() {
				return new FeatureFilter() {
					@Override
					public boolean checkForOrderingChanges(EStructuralFeature feature) {
						return false;
					}
				};
			}
		};
//		Comparison comparison = EMFCompare.builder().setDiffEngine(diffEngine).build().compare(scope);
		Comparison comparison = EMFCompare.builder()
				.setMatchEngineFactoryRegistry(registry).setDiffEngine(diffEngine).build().compare(scope);
		
		// default code not respecting any order or custom id
//		Comparison comparison = EMFCompare.builder().build().compare(scope);
		
		
		// prints differences, comment out if not needed
//		for (Diff diff: comparison.getDifferences()) {
//			System.out.println(diff);
//		}
		
		return !comparison.getDifferences().isEmpty();
	}
}

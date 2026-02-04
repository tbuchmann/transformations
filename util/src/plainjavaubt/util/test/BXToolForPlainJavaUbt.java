package plainjavaubt.util.test;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import org.benchmarx.Configurator;
import org.benchmarx.emf.BXToolForEMF;
import org.benchmarx.emf.Comparator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.util.EcoreUtil;

import plainjavaubt.util.io.Xmi;

public abstract class BXToolForPlainJavaUbt<S extends EObject, T extends EObject, D> extends BXToolForEMF<S, T, D> {
	public BXToolForPlainJavaUbt(BXToolTransformation fwd, BXToolTransformation bwd, String modelDirectory,
			EObject rootSrc, EObject rootTrg, Comparator<S> comparatorSrc, Comparator<T> comparatorTrg) {
		super(comparatorSrc, comparatorTrg);
		
		this.fwd = fwd;
		this.bwd = bwd;
		src = new File(modelDirectory + "/source." + (rootSrc.getClass() == EPackage.class ? "ecore" : "xmi"));
		trg = new File(modelDirectory + "/target." + (rootTrg.getClass() == EPackage.class ? "ecore" : "xmi"));
		this.rootSrc = rootSrc; 
	}
	
	@Override
	public String getName() {
		return "PlainJava";
	}
	
	@Override
	public String toString() {
		return this.getName();
	}

	@Override
	public void initiateSynchronisationDialogue() {
		src.delete();
		trg.delete();
		try {
			Xmi.save(src, EcoreUtil.copy(rootSrc));
			fwd.transform(src, trg);
		} catch (IOException e) {
			throw new AssertionError("Creating source/target file failed.", e);
		}
	}

	@Override
	public void performAndPropagateSourceEdit(Consumer<S> edit) {
		S root = getSourceModel();
		edit.accept(root);
		try {
			Xmi.save(src, root);
			fwd.transform(src, trg);	
		} catch (IOException e) {
			throw new AssertionError("Saving .xmi failed.", e);
		}
	}

	@Override
	public void performAndPropagateTargetEdit(Consumer<T> edit) {
		T root = getTargetModel();
		edit.accept(root);
		try {
			Xmi.save(trg, root);
			bwd.transform(trg, src);
		} catch (IOException e) {
			throw new AssertionError("Saving .xmi failed.", e);
		}	
	}

	@Override
	public void performIdleSourceEdit(Consumer<S> edit) {
		S root = getSourceModel();
		edit.accept(root);
		try {
			Xmi.save(src, root);
		} catch (IOException e) {
			throw new AssertionError("Saving .xmi failed.", e);
		}
	}

	@Override
	public void performIdleTargetEdit(Consumer<T> edit) {
		T root = getTargetModel();
		edit.accept(root);
		try {
			Xmi.save(trg, root);
		} catch (IOException e) {
			throw new AssertionError("Saving .xmi failed.", e);
		}	
	}

	@Override
	public void setConfigurator(Configurator<D> configurator) {
	}

	@SuppressWarnings("unchecked")
	@Override
	public S getSourceModel() {
		return (S) Xmi.load(src);
	}

	@SuppressWarnings("unchecked")
	@Override
	public T getTargetModel() {
		return (T) Xmi.load(trg);
	}

	@Override
	public void saveModels(String name) {
		throw new UnsupportedOperationException();
	}
	
	private BXToolTransformation fwd;
	private BXToolTransformation bwd;
	private File src;
	private File trg;
	private EObject rootSrc;
}

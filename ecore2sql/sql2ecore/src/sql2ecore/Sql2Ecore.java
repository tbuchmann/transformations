package sql2ecore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;

import sql.SqlPackage;
import sql.Table;
import plainjavaubt.util.test.BXToolTransformation;
import plainjavaubt.util.test.TestableTransformation;
import plainjavaubt.util.trafo.Transformation;

/**
 * Converts sql into ecore xmi-files.
 */
public class Sql2Ecore implements TestableTransformation, BXToolTransformation {
	/**
	 * Converts the given sql xmi-file to the corresponding ecore xmi-file. The new ecore xmi-file is saved on the
	 * same location as the sql xmi-file with the Postfix Transformed.
	 * 
	 * @param sql xmi-file to transform.
	 * @return the location of the created ecore xmi-file.
	 * @throws IOException if saving fails.
	 */
	public File transform(File sql) throws IOException {
		sql.Schema source = null;
		source = Transformation.prepareTransformation(sql, SqlPackage.eINSTANCE, EcorePackage.eINSTANCE, source);
		EPackage target = performTransformation(source);
		return Transformation.finishTransformation(sql, target);
	}
	
	/**
	 * Works like the one parameter version, but you can also specify the target file.
	 */
	public File transform(File sql, File ecore) throws IOException {
		sql.Schema source = null;
		source = Transformation.prepareTransformation(sql, SqlPackage.eINSTANCE, EcorePackage.eINSTANCE, source);
		EPackage target = performTransformation(source);
		plainjavaubt.util.io.Xmi.save(ecore, target);
		return ecore;
	}
	
	private EPackage performTransformation(sql.Schema source) {
		EcoreFactory factory = EcoreFactory.eINSTANCE;
		
		// creates package
		EPackage target = factory.createEPackage();
		target.setName(source.getName());
		
		// transforms classes
		LinkedHashMap<sql.Table, EClass> eclasses = new LinkedHashMap<>();
		for (sql.Table eclassSource : source.getOwnedTables()) {
			if (!toStringList(eclassSource.getOwnedAnnotations()).contains("class")) {
				continue;
			}
			
			EClass eclassTarget = factory.createEClass();
			eclassTarget.setName(eclassSource.getName());
			eclassTarget.setAbstract(toStringList(eclassSource.getOwnedAnnotations()).contains("abstract"));
			target.getEClassifiers().add(eclassTarget);
			eclasses.put(eclassSource, eclassTarget);
		}
		
		// handles inheritance
		for (Map.Entry<Table, EClass> eclass : eclasses.entrySet()) {
			for (sql.ForeignKey foreignKey : eclass.getKey().getOwnedForeignKeys()) {
				if (toStringList(foreignKey.getOwnedAnnotations()).contains("superType")) {
					eclass.getValue().getESuperTypes().add(eclasses.get(foreignKey.getReferencedTable()));
				}
			}
		}
		
		// transforms single valued attributes
		for (Map.Entry<Table, EClass> eclass : eclasses.entrySet()) {
			for (sql.Column eattributeSource : eclass.getKey().getOwnedColumns()) {
				if (toStringList(eattributeSource.getOwnedAnnotations()).contains("attribute")) {
					createEattribute(eattributeSource.getName(), eattributeSource.getType(), eclass.getValue());
				}
			}
		}
		
		// transforms multi valued attributes
		for (sql.Table eattributeSource : source.getOwnedTables()) {
			if (!toStringList(eattributeSource.getOwnedAnnotations()).contains("attribute")) {
				continue;
			}
			
			String name = eattributeSource.getName().substring(eattributeSource.getName().indexOf('_') + 1);
			String type = null;
			for (sql.Column column : eattributeSource.getOwnedColumns()) {
				if (column.getName().equals("value")) {
					type = column.getType();
				}
			}
			EClass owning = eclasses.get(getForeignKey(eattributeSource, "id").getReferencedTable());
			EAttribute eattributeTarget = createEattribute(name, type, owning);
			eattributeTarget.setUpperBound(-1);
		}
		
		// transforms containment references
		for (Map.Entry<Table, EClass> eclass : eclasses.entrySet()) {
			for (sql.Column ereferenceSource : eclass.getKey().getOwnedColumns()) {
				List<String> annotations = toStringList(ereferenceSource.getOwnedAnnotations());
				if (!annotations.contains("containment")) {
					continue;
				}
				
				String name = annotations.contains("bidirectional") ? ereferenceSource.getName().split("_inverse_")[1]
						: ereferenceSource.getName().split("_inverse")[0];	
				EClass owning = eclasses.get(
						getForeignKey(eclass.getKey(), ereferenceSource.getName()).getReferencedTable());
				EReference ereferenceTarget = createEreference(name, eclass.getValue(), owning);
				ereferenceTarget.setContainment(true);
				ereferenceTarget.setUpperBound(annotations.contains("single") ? 1 : -1);
				
				if (!annotations.contains("bidirectional")) {
					continue;
				}
				
				String oppositeName = ereferenceSource.getName().split("_inverse_")[0];
				EReference opposite = createEreference(oppositeName, owning, eclass.getValue());
				opposite.setEOpposite(ereferenceTarget);
				ereferenceTarget.setEOpposite(opposite);
			}
		}
		
		// transforms single unidirectional cross references
		for (Map.Entry<Table, EClass> eclass : eclasses.entrySet()) {
			for (sql.Column ereferenceSource : eclass.getKey().getOwnedColumns()) {
				if (!toStringList(ereferenceSource.getOwnedAnnotations()).contains("cross")) {
					continue;
				}
				
				EClass type = eclasses.get(
						getForeignKey(eclass.getKey(), ereferenceSource.getName()).getReferencedTable());
				createEreference(ereferenceSource.getName(), type, eclass.getValue());
			}
		}
		
		// transforms multi unidirectional cross references
		for (sql.Table ereferenceSource : source.getOwnedTables()) {
			if (!toStringList(ereferenceSource.getOwnedAnnotations()).contains("unidirectional")) {
				continue;
			}
			
			String name = ereferenceSource.getName().substring(ereferenceSource.getName().lastIndexOf('_') + 1);
			EClass type = eclasses.get(getForeignKey(ereferenceSource, "reference").getReferencedTable());
			EClass owning = eclasses.get(getForeignKey(ereferenceSource, "id").getReferencedTable());
			EReference ereferenceTarget = createEreference(name, type, owning);
			ereferenceTarget.setUpperBound(-1);
		}
		
		// transforms bidirectional cross references
		for (sql.Table ereferenceSource : source.getOwnedTables()) {
			List<String> annotations = toStringList(ereferenceSource.getOwnedAnnotations());
			if (!annotations.contains("bidirectional")) {
				continue;
			}
			
			String name = ereferenceSource.getName().split("_")[1];
			EClass type = eclasses.get(getForeignKey(ereferenceSource, "target").getReferencedTable());
			EClass owning = eclasses.get(getForeignKey(ereferenceSource, "source").getReferencedTable());
			EReference ereferenceTarget = createEreference(name, type, owning);
			ereferenceTarget.setUpperBound(annotations.contains("forwardSingle") ? 1 : -1);
			
			String oppositeName = ereferenceSource.getName().split("_")[4];
			EReference opposite = createEreference(oppositeName, owning, type);
			opposite.setUpperBound(annotations.contains("backwardSingle") ? 1 : -1);
			opposite.setEOpposite(ereferenceTarget);
			ereferenceTarget.setEOpposite(opposite);
		}
		
		return target;
	}
	
	private List<String> toStringList(Collection<sql.Annotation> annotations) {
		ArrayList<String> result = new ArrayList<>();
		for (sql.Annotation annotation : annotations) {
			result.add(annotation.getAnnotation());
		}
		return result;
	}
	
	private sql.ForeignKey getForeignKey(sql.Table table, String column) {
		for (sql.ForeignKey key : table.getOwnedForeignKeys()) {
			if (key.getColumn().getName().equals(column)) {
				return key;
			}
		}
		
		return null;
	}
	
	private EAttribute createEattribute(String name, String type, EClass owning) {
		EDataType eattributeType;
		switch (type) {
		case ("int"):
			eattributeType = EcorePackage.Literals.EINT;
			break;
		case ("date"):
			eattributeType = EcorePackage.Literals.EDATE;
			break;
		case ("boolean"):
			eattributeType = EcorePackage.Literals.EBOOLEAN;
			break;
		default:
			eattributeType = EcorePackage.Literals.ESTRING;
			break;
		}
		EAttribute eattribute = EcoreFactory.eINSTANCE.createEAttribute();
		eattribute.setName(name);
		eattribute.setEType(eattributeType);
		owning.getEStructuralFeatures().add(eattribute);
		return eattribute;
	}
	
	private EReference createEreference(String name, EClass type, EClass owning) {
		EReference ereference = EcoreFactory.eINSTANCE.createEReference();
		ereference.setName(name);
		ereference.setEType(type);
		owning.getEStructuralFeatures().add(ereference);
		return ereference;
	}
}

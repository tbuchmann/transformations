package ecore2sql;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;

import sql.SqlFactory;
import sql.SqlPackage;
import plainjavaubt.util.test.BXToolTransformation;
import plainjavaubt.util.test.TestableTransformation;
import plainjavaubt.util.trafo.Transformation;

/**
 * Converts ecore into sql xmi-files.
 */
public class Ecore2Sql implements TestableTransformation, BXToolTransformation {
	/**
	 * Converts the given ecore xmi-file to the corresponding sql xmi-file. The new sql xmi-file is saved on the
	 * same location as the ecore xmi-file with the Postfix Transformed.
	 * 
	 * @param ecore xmi-file to transform.
	 * @return the location of the created sql xmi-file.
	 * @throws IOException if saving fails.
	 */
	public File transform(File ecore) throws IOException {
		EPackage source = null;
		source = Transformation.prepareTransformation(ecore, EcorePackage.eINSTANCE, SqlPackage.eINSTANCE, source);
		sql.Schema target = performTransformation(source);
		return Transformation.finishTransformation(ecore, target);
	}
	
	/**
	 * Works like the one parameter version, but you can also specify the target file.
	 */
	public File transform(File ecore, File sql) throws IOException {
		EPackage source = null;
		source = Transformation.prepareTransformation(ecore, EcorePackage.eINSTANCE, SqlPackage.eINSTANCE, source);
		sql.Schema target = performTransformation(source);
		plainjavaubt.util.io.Xmi.save(sql, target);
		return sql;
	}
	
	private sql.Schema performTransformation(EPackage source) {
		SqlFactory factory = SqlFactory.eINSTANCE;
		
		// creates schema
		sql.Schema target = factory.createSchema();
		target.setName(source.getName());
		addAnnotations(target, Arrays.asList("package"));
		
		//  creates object table
		sql.Table objectTable = createTable("EObject", target);
		sql.Column objectTableId = createPrimaryKeyAttribute(objectTable);
		objectTableId.getProperties().add(sql.Property.AUTO_INCREMENT);
		
		// transforms classes
		HashMap<EClass, sql.Table> eclasses = new HashMap<>();
		for (EClassifier eclassSource : source.getEClassifiers()) {
			if (!(eclassSource instanceof EClass)) {
				continue;
			}
			
			sql.Table eclassTarget = createTable(eclassSource.getName(), target);
			createPrimaryKeyAttribute(eclassTarget);
			addAnnotations(eclassTarget, Arrays.asList(
					"class", ((EClass) eclassSource).isAbstract() ? "abstract" : "concrete"));
			eclasses.put((EClass) eclassSource, eclassTarget);
			
			sql.Column objectTableColumn = createForeignKeyAttribute(eclassTarget.getName(), objectTable, eclassTarget);
			objectTableColumn.getProperties().clear();
			objectTableColumn.getProperties().add(sql.Property.UNIQUE);
		}
		
		// handles inheritance
		for (EClassifier eclassSource : source.getEClassifiers()) {
			if (!(eclassSource instanceof EClass)) {
				continue;
			}
			
			sql.Table superType;
			String annotation;
			EList<EClass> esuperTypes = ((EClass) eclassSource).getESuperTypes();
			if (!esuperTypes.isEmpty()) {
				superType = eclasses.get(esuperTypes.get(0));
				annotation = "superType";
			} else {
				superType = objectTable;
				annotation = "root";
			}
			
			sql.Table eclassTarget = eclasses.get((EClass) eclassSource);
			sql.Key key = createForeignKey(eclassTarget.getOwnedPrimaryKey().getColumn(), eclassTarget, superType);
			addAnnotations(key, Arrays.asList(annotation));
		}
		
		// transforms attributes
		for (EClassifier eclassSource : source.getEClassifiers()) {
			if (!(eclassSource instanceof EClass)) {
				continue;
			}
			
			for (EStructuralFeature eattributeSource : ((EClass) eclassSource).getEStructuralFeatures()) {
				if (!(eattributeSource instanceof EAttribute)) {
					continue;
				}
				
				String eattributeTargetType;
				EDataType eattributeSourceType = ((EAttribute) eattributeSource).getEAttributeType();
				if (eattributeSourceType.equals(EcorePackage.Literals.EINT)) {
					eattributeTargetType = "int";
				} else if (eattributeSourceType.equals(EcorePackage.Literals.EDATE)) {
					eattributeTargetType = "date";
				} else if (eattributeSourceType.equals(EcorePackage.Literals.EBOOLEAN)) {
					eattributeTargetType = "boolean";
				} else {
					eattributeTargetType = "varchar(30)";
				}
				
				if (((EAttribute) eattributeSource).getUpperBound() == 1) {
					sql.Column eattributeTarget = factory.createColumn();
					eattributeTarget.setName(eattributeSource.getName());
					eattributeTarget.setType(eattributeTargetType);
					eattributeTarget.setOwningTable(eclasses.get((EClass) eclassSource));
					addAnnotations(eattributeTarget, Arrays.asList("attribute", "single"));
					
				} else {
					String eattributeTargetName = eclassSource.getName() + "_" + eattributeSource.getName();
					sql.Table eattributeTarget = createTable(eattributeTargetName, target);
					createForeignKeyAttribute("id", eattributeTarget, eclasses.get((EClass) eclassSource));
					sql.Column value = factory.createColumn();
					value.setName("value");
					value.setType(eattributeTargetType);
					value.getProperties().add(sql.Property.NOT_NULL);
					value.setOwningTable(eattributeTarget);
					addAnnotations(eattributeTarget, Arrays.asList("attribute", "multi"));
				}
			}
		}
		
		// transforms references
		for (EClassifier eclassSource : source.getEClassifiers()) {
			if (!(eclassSource instanceof EClass)) {
				continue;
			}
			
			for (EStructuralFeature Source : ((EClass) eclassSource).getEStructuralFeatures()) {
				if (!(Source instanceof EReference)) {
					continue;
				}
				
				EReference ereferenceSource = (EReference) Source;
				
				if (ereferenceSource.isContainment()) {
					String ereferenceTargetName;
					ArrayList<String> annotations = new ArrayList<>(Arrays.asList("containment"));
					if (ereferenceSource.getEOpposite() == null) {
						ereferenceTargetName = ereferenceSource.getName() + "_inverse";
						annotations.add("unidirectional");
						
					} else {
						ereferenceTargetName =
								ereferenceSource.getEOpposite().getName() + "_inverse_" + ereferenceSource.getName();
						annotations.add("bidirectional");
					}
					
					sql.Column ereferenceTarget = createForeignKeyAttribute(ereferenceTargetName,
							eclasses.get(ereferenceSource.getEReferenceType()), eclasses.get((EClass) eclassSource));
					ereferenceTarget.getProperties().clear();
					if (ereferenceSource.getUpperBound() == 1) {
						annotations.add("single");
					} else {
						annotations.add("multi");
					}
					addAnnotations(ereferenceTarget, annotations);
					addAnnotations(ereferenceTarget.getKeys().get(0), annotations);
					
				} else if (ereferenceSource.isContainer()) {
					continue;
					
				} else if (ereferenceSource.getUpperBound() == 1 && ereferenceSource.getEOpposite() == null) {
					sql.Column ereferenceTarget = createForeignKeyAttribute(ereferenceSource.getName(),
							eclasses.get((EClass) eclassSource), eclasses.get(ereferenceSource.getEReferenceType()));
					ereferenceTarget.getProperties().clear();
					((sql.ForeignKey) ereferenceTarget.getKeys().get(0)).getOwnedEvents().get(0).setAction(
							sql.Action.SET_NULL);
					List<String> annotations = Arrays.asList("single", "unidirectional", "cross");
					addAnnotations(ereferenceTarget, annotations);
					addAnnotations(ereferenceTarget.getKeys().get(0), annotations);
					
				} else if (ereferenceSource.getEOpposite() == null) {
					String ereferenceTargetName = eclassSource.getName() + "_" + ereferenceSource.getName();
					sql.Table ereferenceTarget = createTable(ereferenceTargetName, target);
					createForeignKeyAttribute("id", ereferenceTarget, eclasses.get((EClass) eclassSource));
					createForeignKeyAttribute(
							"reference", ereferenceTarget, eclasses.get(ereferenceSource.getEReferenceType()));
					addAnnotations(ereferenceTarget, Arrays.asList("cross", "multi", "unidirectional"));
					
				} else {
					String sourceNameFragment = eclassSource.getName() + "_" + ereferenceSource.getName();
					String oppositeNameFragment = ereferenceSource.getEType().getName() + "_" 
							+ ereferenceSource.getEOpposite().getName();
					if (sourceNameFragment.compareTo(oppositeNameFragment) > 0) {
						continue;
						
					} else {
						String ereferenceTargetName = sourceNameFragment + "_inverse_" + oppositeNameFragment;
						sql.Table ereferenceTarget = createTable(ereferenceTargetName, target);
						createForeignKeyAttribute("source", ereferenceTarget, eclasses.get((EClass) eclassSource));
						createForeignKeyAttribute(
								"target", ereferenceTarget, eclasses.get(ereferenceSource.getEReferenceType()));
						ArrayList<String> annotations = new ArrayList<>(Arrays.asList("cross", "bidirectional"));
						if (ereferenceSource.getUpperBound() == 1) {
							annotations.add("forwardSingle");
						} else {
							annotations.add("forwardMulti");
						}
						if (ereferenceSource.getEOpposite().getUpperBound() == 1) {
							annotations.add("backwardSingle");
						} else {
							annotations.add("backwardMulti");
						}
						addAnnotations(ereferenceTarget, annotations);
					}
				}
			}
		}
		
		return target;
	}
	
	private sql.Table createTable(String name, sql.Schema owningSchema) {
		sql.Table table = SqlFactory.eINSTANCE.createTable();
		table.setName(name);
		table.setOwningSchema(owningSchema);	
		return table;
	}
	
	private sql.Column createPrimaryKeyAttribute(sql.Table owning) {
		sql.Column attribute = SqlFactory.eINSTANCE.createColumn();
		attribute.setName("id");
		attribute.setType("int");
		attribute.getProperties().add(sql.Property.NOT_NULL);
		attribute.setOwningTable(owning);
		sql.PrimaryKey key = SqlFactory.eINSTANCE.createPrimaryKey();
		key.setColumn(attribute);
		key.setOwningTable(owning);
		return attribute;
	}
	
	private sql.ForeignKey createForeignKey(sql.Column column, sql.Table owning, sql.Table referenced) {
		sql.ForeignKey key = SqlFactory.eINSTANCE.createForeignKey();
		key.setColumn(column);
		key.setReferencedTable(referenced);
		key.setOwningTable(owning);
		sql.Event deletion = SqlFactory.eINSTANCE.createEvent();
		deletion.setCondition(sql.Condition.DELETE);
		deletion.setAction(sql.Action.CASCADE);
		deletion.setOwningForeignKey(key);
		return key;
	}
	
	private sql.Column createForeignKeyAttribute(String name, sql.Table owning, sql.Table referenced) {
		sql.Column attribute = SqlFactory.eINSTANCE.createColumn();
		attribute.setName(name);
		attribute.setType("int");
		attribute.getProperties().add(sql.Property.NOT_NULL);
		attribute.setOwningTable(owning);
		createForeignKey(attribute, owning, referenced);
		return attribute;
	}
	
	private void addAnnotations(sql.ModelElement element, Collection<String> annotations) {
		for (String annotationName : annotations) {
			sql.Annotation annotation = SqlFactory.eINSTANCE.createAnnotation();
			annotation.setAnnotation(annotationName);
			annotation.setOwningModelElement(element);
		}
	}
}

package de.tbuchmann.m2m.f2p.java;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import Families.FamiliesFactory;
import Families.FamiliesPackage;
import Families.Family;
import Families.FamilyMember;
import Families.FamilyRegister;
import Persons.Female;
import Persons.Male;
import Persons.Person;
import Persons.PersonRegister;
import Persons.PersonsFactory;
import Persons.PersonsPackage;

/**
 * A simple Java-based model-to-model transformation from Families to Persons.
 * It performs the following transformations:
 * - batch forward
 * - batch backward with options to add to existing families and prefer parent roles.
 * - incremental forward and backward transformations that check for deletions in the source and target models and update the target model accordingly.
 * (some cases are still failing)
 * - concurrent edits by executing the forward and backward transformation sequentially and checking for deletions in both models to reflect changes in the respective other model.
 * (note: this is the simplest way to handle concurrent edits, but it is not ideal, as it can lead to conflicts and lost updates, if the same element is edited in both models at the same time. A more sophisticated approach would be to use a conflict detection and resolution mechanism, e.g. by using EMF Compare to detect conflicts and then applying a resolution strategy, e.g. by asking the user or by using a predefined strategy.)
 * 
 * @author tbuchmann
 * @version 0.1.0
 *  
 */
public class Families2PersonsJava {
	private Resource familiesResource;
	private Resource personsResource;
	
	// support for incremental ones
	private BiMap<EObject, EObject> correspondences = HashBiMap.create();
	
	public Families2PersonsJava(Resource source, Resource target) {
		familiesResource = source;
		personsResource = target;
	}

    public static void main(String[] args) {
        // Load Families and Persons models
    	// Standard EMF resource set and resource loading
    	// required to load and save EMF models
        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore", new XMIResourceFactoryImpl());
        resourceSet.getPackageRegistry().put(FamiliesPackage.eNS_URI, FamiliesPackage.eINSTANCE);
        resourceSet.getPackageRegistry().put(PersonsPackage.eNS_URI, PersonsPackage.eINSTANCE);

        // hier werden die Modellinstanzen geladen
        // Bitte passen Sie die Pfade zu Ihren Modellen an
        Resource familiesResource = resourceSet.getResource(URI.createFileURI("path/to/Families.xmi"), true);
        Resource personsResource = resourceSet.getResource(URI.createFileURI("path/to/Persons.xmi"), true);
        
        Families2PersonsJava t = new Families2PersonsJava(familiesResource, personsResource);

        // das jeweilige Wurzelelement extrahieren
        // EMF-Modelle haben immer genau ein Wurzelelement
        // d.h. damit hat man den Einstiegspunkt in das Modell, um von dort aus alle anderen Elemente zu erreichen
        FamilyRegister familyRegister = (FamilyRegister) familiesResource.getContents().get(0);
        PersonRegister personRegister = (PersonRegister) personsResource.getContents().get(0);

        // Transform Families to Persons
        t.transformFamiliesToPersons(familyRegister, personRegister);

        // Save the transformed model
        t.saveModel(personsResource, "path/to/TransformedPersons.xmi");
    }
    
    public void forward(FamilyRegister familyRegister, PersonRegister personRegister) {
		List<FamilyMember> currentFamilyMembers = transformFamiliesToPersons(familyRegister, personRegister);
		// check for deletions in the source model (e.g. if a son has been removed from a family, it should also be removed from the target model)	
        checkSourceDeletions(currentFamilyMembers);
	}
    
    public void backward(PersonRegister personRegister, FamilyRegister familyRegister, boolean addToExistingFamilies, boolean preferParentRole) {
		List<EObject> currentPersons = transformPersonsToFamilies(personRegister, familyRegister, addToExistingFamilies, preferParentRole);
		checkTargetDeletions(currentPersons);
    }
    
    public void synch(FamilyRegister familyRegister, PersonRegister personRegister, boolean addToExistingFamilies, boolean preferParentRole) {
    	List<FamilyMember> currentFamilyMembers = transformFamiliesToPersons(familyRegister, personRegister);
    	checkSourceDeletions(currentFamilyMembers);
		List<EObject> currentPersons = transformPersonsToFamilies(personRegister, familyRegister, addToExistingFamilies, preferParentRole);
		checkTargetDeletions(currentPersons);
	}

    public List<FamilyMember> transformFamiliesToPersons(FamilyRegister familyRegister, PersonRegister personRegister) {
    	List<FamilyMember> currentFamilyMembers = new ArrayList<>();
    	
        for (Family family : familyRegister.getFamilies()) {
            // Transform father
            if (family.getFather() != null) {
            	// check if father exists in the map,
            	// if not, create a new one and add it to the map
            	Male father = (Male) correspondences.get(family.getFather());
            	if (father == null) {          	              		 
            		father = PersonsFactory.eINSTANCE.createMale();
            		personRegister.getPersons().add(father);
            		// add both elements to the map to support incremental transformations in the future
            		addCorrespondence(family.getFather(), father);
            	}
            	// update the name of the father (even if it already exists, to reflect changes in the source model)
                father.setName(family.getName() + ", " + family.getFather().getName());
                // add the father to the list of current family members to check for deletions in the source model later	
                currentFamilyMembers.add(family.getFather());
            }
            // Transform mother
            if (family.getMother() != null) {
            	// check if father exists in the map,
            	// if not, create a new one and add it to the map
                Female mother = (Female) correspondences.get(family.getMother());
                if (mother == null) {
                	mother = PersonsFactory.eINSTANCE.createFemale();
                	personRegister.getPersons().add(mother);
                	addCorrespondence(family.getMother(), mother);
                }
                // update the name of the father (even if it already exists, to reflect changes in the source model)
                mother.setName(family.getName() + ", " + family.getMother().getName());  
                // add the mother to the list of current family members to check for deletions in the source model later
                currentFamilyMembers.add(family.getMother());
            }
            // Transform sons
            for (FamilyMember son : family.getSons()) {
            	// check if son exists in the map,
            	Male maleSon = null;
            	Date birthday;
            	if (correspondences.get(son) != null && correspondences.get(son) instanceof Male) {
            		maleSon = (Male) correspondences.get(son);
            	} else if (correspondences.get(son) != null && correspondences.get(son) instanceof Female) {
            		// the role of the person had changed (e.g. a female has been added as a son...
            		Female femaleSon = (Female) correspondences.get(son);
            		birthday = femaleSon.getBirthday();
            		personRegister.getPersons().remove(femaleSon);
            		removeCorrespondenceByTarget(femaleSon);
            		maleSon = PersonsFactory.eINSTANCE.createMale();
            		maleSon.setBirthday(birthday);
					personRegister.getPersons().add(maleSon);
					addCorrespondence(son, maleSon);
            	}
				if (maleSon == null) {                 
					maleSon = PersonsFactory.eINSTANCE.createMale();
					personRegister.getPersons().add(maleSon);
					addCorrespondence(son, maleSon);
				}
                maleSon.setName(family.getName() + ", " + son.getName());
                // add the son to the list of current family members to check for deletions in the source model later
                currentFamilyMembers.add(son);
                
            }
            // Transform daughters
            for (FamilyMember daughter : family.getDaughters()) {
                Female femaleDaughter = null;
                Date birthday;
                if (correspondences.get(daughter) != null && correspondences.get(daughter) instanceof Female) {
                	femaleDaughter = (Female) correspondences.get(daughter);                
                } else if (correspondences.get(daughter) != null && correspondences.get(daughter) instanceof Male) {
                	// the role of the person had changed (e.g. a female has been added as a son...
                	Male maleDaughter = (Male) correspondences.get(daughter);
                	birthday = maleDaughter.getBirthday();
                	personRegister.getPersons().remove(maleDaughter);
                	removeCorrespondenceByTarget(maleDaughter);
                	femaleDaughter = PersonsFactory.eINSTANCE.createFemale();
                	femaleDaughter.setBirthday(birthday);
                	personRegister.getPersons().add(femaleDaughter);
                	addCorrespondence(daughter, femaleDaughter);
                }
                if (femaleDaughter == null) {
                	femaleDaughter = PersonsFactory.eINSTANCE.createFemale();
                	personRegister.getPersons().add(femaleDaughter);
                	addCorrespondence(daughter, femaleDaughter);
                }
                femaleDaughter.setName(family.getName() + ", " + daughter.getName());
                // add the daughter to the list of current family members to check for deletions in the source model later
                currentFamilyMembers.add(daughter);
            }
        }
        return currentFamilyMembers;
    }

    public List<EObject> transformPersonsToFamilies(PersonRegister personRegister, FamilyRegister familyRegister, boolean addToExistingFamilies, boolean preferParentRole) {
    	List<EObject> currentPersons = new ArrayList<>();
        for (Person person : personRegister.getPersons()) {
        	// check if person exists in the map,
        	// if not, create a new one and add it to the map
        	FamilyMember familyMember = (FamilyMember) correspondences.inverse().get(person);
            String[] nameParts = person.getName().split(", ");
            if (nameParts.length != 2) {
                continue; // Skip malformed names
            }
            String familyName = nameParts[0];
            String personName = nameParts[1];
            
            // check if person and familymember matches
            if (familyMember != null && 
            		personName.equals(familyMember.getName()) && 
            		familyName.equals( ((Family)familyMember.eContainer()).getName())) {
				continue; // no update needed, skip to the next person
			}

            Family family = findOrCreateFamily(familyRegister, familyName, addToExistingFamilies, preferParentRole);

            if (familyMember == null) {
            	familyMember = FamiliesFactory.eINSTANCE.createFamilyMember();
            	addCorrespondence(familyMember, person);
            }
            familyMember.setName(personName);

            if (preferParentRole) {
                if (person instanceof Male) {
                    if (family.getFather() == null) {
                        family.setFather(familyMember);
                    } else {
                        family.getSons().add(familyMember);
                    }
                } else if (person instanceof Female) {
                    if (family.getMother() == null) {
                        family.setMother(familyMember);
                    } else {
                        family.getDaughters().add(familyMember);
                    }
                }
            } else {
                if (person instanceof Male) {
                    family.getSons().add(familyMember);
                } else if (person instanceof Female) {
                    family.getDaughters().add(familyMember);
                }
            }
            currentPersons.add(person);
        }
        return currentPersons;
    }

    private Family findOrCreateFamily(FamilyRegister familyRegister, String familyName, boolean addToExistingFamilies, boolean preferParentRole) {
        //Optional<Family> existing = Optional.empty();
    	if (addToExistingFamilies) {
        	//List<Family> matchingFamilies = new ArrayList<>();
            for (Family existingFamily : familyRegister.getFamilies()) {
                if (existingFamily.getName().equals(familyName)) {
                    return existingFamily;
                	//matchingFamilies.add(existingFamily);
                }
            }
//            existing = matchingFamilies.stream().filter(f -> preferParentRole ? (f.getFather() == null || f.getMother() == null) : true).findFirst();
//            if (existing.isPresent()) {
//				return existing.get();
//			}
        }
        Family newFamily = FamiliesFactory.eINSTANCE.createFamily();
        newFamily.setName(familyName);
        familyRegister.getFamilies().add(newFamily);
        return newFamily;
    }

    /**
     * Elegantly collects all family members from all families in a register.
     * This is an efficient and performant way to gather all family members in one pass.
     * 
     * @param familyRegister the family register to collect members from
     * @return a list containing all family members (fathers, mothers, sons, daughters)
     */
    private List<FamilyMember> getAllFamilyMembers(FamilyRegister familyRegister) {
        List<FamilyMember> allFamilyMembers = new ArrayList<>();
        for (Family family : familyRegister.getFamilies()) {
            if (family.getFather() != null) {
                allFamilyMembers.add(family.getFather());
            }
            if (family.getMother() != null) {
                allFamilyMembers.add(family.getMother());
            }
            allFamilyMembers.addAll(family.getSons());
            allFamilyMembers.addAll(family.getDaughters());
        }
        return allFamilyMembers;
    }


    public void saveModel(Resource resource, String filePath) {
        try {
            resource.setURI(URI.createFileURI(filePath));
            resource.save(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private boolean sourceElementExistsInTarget(EObject sourceElement) {
		return correspondences.containsKey(sourceElement);
	}
    
    private EObject getCorrespondingTargetElement(EObject sourceElement) {
		return correspondences.get(sourceElement);
	}
    
    private EObject getCorrespondingSourceElement(EObject targetElement) {
		return correspondences.inverse().get(targetElement);
	}
	
	private void addCorrespondence(EObject sourceElement, EObject targetElement) {
		correspondences.put(sourceElement, targetElement);
	}
	
	private void removeCorrespondenceBySource(EObject sourceElement) {
		correspondences.remove(sourceElement);
	}
	
	private void removeCorrespondenceByTarget(EObject targetElement) {
		correspondences.inverse().remove(targetElement);
	}
	
	private void checkSourceDeletions(List<FamilyMember> currentFamilyMembers) {
		// Detect and remove orphaned correspondences where the source family member was deleted
		List<EObject> toRemove = new ArrayList<>();
		for (EObject member : new ArrayList<>(correspondences.keySet())) {
			if (!currentFamilyMembers.contains(member)) {
				toRemove.add(member);
			}
		}
		
		for (EObject member : toRemove) {
			EObject trgt = getCorrespondingTargetElement(member);
			if (trgt != null) {
				// delete corresponding target element
				PersonRegister register = (PersonRegister) personsResource.getContents().get(0);
				register.getPersons().remove(trgt);
			}
			// remove correspondence
			removeCorrespondenceBySource(member);
		}
		
		// Detect and remove orphaned correspondences where the target person was deleted
		toRemove.clear();
		PersonRegister register = (PersonRegister) personsResource.getContents().get(0);
		for (Person person : new ArrayList<>(register.getPersons())) {
			if (!correspondences.values().contains(person)) {
				toRemove.add(person);
			}
		}
		
		for (EObject person : toRemove) {
			EObject src = getCorrespondingSourceElement(person);
			((PersonRegister)personsResource.getContents().get(0)).getPersons().remove(person);
			if (src != null) {
				removeCorrespondenceByTarget(person);
			}
		}
	}
	
	private void checkTargetDeletions(List<EObject> currentPersons) {
		// Iterate over correspondences and check, if the target person still exists in the target model, if not, delete the source family member and remove the correspondence
		PersonRegister personRegister = (PersonRegister) personsResource.getContents().get(0);
		List<Person> currentPersonsInModel = personRegister.getPersons();
		List<EObject> elemsToDelete = new ArrayList<>();
		correspondences.values().stream().filter(targetPerson -> !currentPersonsInModel.contains(targetPerson)).forEach(targetPerson -> {
			EObject sourceMember = getCorrespondingSourceElement(targetPerson);
			if (sourceMember != null) {
				elemsToDelete.add(sourceMember);
			}			
		});
				
//		FamilyRegister familyRegister = (FamilyRegister) familiesResource.getContents().get(0);
//		List<FamilyMember> fmToDelete = new ArrayList<>();
//		familiesResource.getContents().stream()
//			.flatMap(f -> getAllFamilyMembers(familyRegister).stream())
//			.filter(member -> !correspondences.containsKey(member))
//			.forEach(member -> elemsToDelete.add(member));		
//		
		EcoreUtil.deleteAll(elemsToDelete, true);
	}	
}

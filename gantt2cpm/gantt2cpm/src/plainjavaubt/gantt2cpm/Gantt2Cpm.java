package plainjavaubt.gantt2cpm;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import cpm.CpmFactory;
import cpm.CpmPackage;
import gantt.DependencyType;
import gantt.GanttPackage;
import plainjavaubt.util.test.BXToolTransformation;
import plainjavaubt.util.test.TestableTransformation;
import plainjavaubt.util.trafo.Transformation;

/**
 * Converts gantt into cpm xmi-files.
 */
public class Gantt2Cpm implements TestableTransformation, BXToolTransformation {
	/**
	 * Converts the given gantt xmi-file to the corresponding cpm xmi-file. The new cpm xmi-file is saved on the same
	 * location as the gantt xmi-file with the Postfix Transformed.
	 * 
	 * @param gantt xmi-file to transform.
	 * @return the location of the created cpm xmi-file.
	 * @throws IOException if saving fails.
	 */
	public File transform(File gantt) throws IOException {
		gantt.GanttDiagram source = null;
		source = Transformation.prepareTransformation(gantt, GanttPackage.eINSTANCE, CpmPackage.eINSTANCE, source);
		cpm.CPMNetwork target = performTransformation(source);
		return Transformation.finishTransformation(gantt, target);
	}
	
	/**
	 * Works like the one parameter version, but you can also specify the target file.
	 */
	public File transform(File gantt, File cpm) throws IOException {
		gantt.GanttDiagram source = null;
		source = Transformation.prepareTransformation(gantt, GanttPackage.eINSTANCE, CpmPackage.eINSTANCE, source);
		cpm.CPMNetwork target = performTransformation(source);
		plainjavaubt.util.io.Xmi.save(cpm, target);
		return cpm;
	}
	
	private cpm.CPMNetwork performTransformation(gantt.GanttDiagram source) {
		CpmFactory factory = CpmFactory.eINSTANCE;
		cpm.CPMNetwork target = factory.createCPMNetwork();
		
		target.setName(source.getName());
		HashMap<String, cpm.Activity> activities = new HashMap<>();
		int nextEventNumber = 1;
		for (gantt.Element activitySource : source.getElements()) {
			if (activitySource instanceof gantt.Activity) {
				cpm.Activity activityTarget = factory.createActivity();
				activityTarget.setName(((gantt.Activity) activitySource).getName());
				activityTarget.setDuration(((gantt.Activity) activitySource).getDuration());
				activityTarget.setNetwork(target);
				activities.put(((gantt.Activity) activitySource).getName(), activityTarget);
				cpm.Event startEvent = factory.createEvent();
				startEvent.setNumber(nextEventNumber++);
				startEvent.getOutgoingActivities().add(activityTarget);
				startEvent.setNetwork(target);
				cpm.Event endEvent = factory.createEvent();
				endEvent.setNumber(nextEventNumber++);
				endEvent.getIncomingActivities().add(activityTarget);
				endEvent.setNetwork(target);
			}
		}
		
		for (gantt.Element dependencySource : source.getElements()) {
			if (dependencySource instanceof gantt.Dependency) {
				String predecessorName = ((gantt.Dependency) dependencySource).getPredecessor().getName();
				String successorName = ((gantt.Dependency) dependencySource).getSuccessor().getName();
				
				cpm.Activity dependencyTarget = factory.createActivity();	
				dependencyTarget.setName(predecessorName + "->" + successorName);
				dependencyTarget.setDuration(((gantt.Dependency) dependencySource).getOffset());
				
				DependencyType type = ((gantt.Dependency) dependencySource).getDependencyType();
				if (type.equals(DependencyType.START_START) || type.equals(DependencyType.START_END)) {
					dependencyTarget.setSourceEvent(activities.get(predecessorName).getSourceEvent());
				} else {
					dependencyTarget.setSourceEvent(activities.get(predecessorName).getTargetEvent());
				}
				if (type.equals(DependencyType.START_START) || type.equals(DependencyType.END_START)) {
					dependencyTarget.setTargetEvent(activities.get(successorName).getSourceEvent());
				} else {
					dependencyTarget.setTargetEvent(activities.get(successorName).getTargetEvent());
				}
				
				dependencyTarget.setNetwork(target);
			}
		}
		
		return target;
	}
}

package plainjavaubt.cpm2gantt;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import cpm.CpmPackage;
import gantt.DependencyType;
import gantt.GanttFactory;
import gantt.GanttPackage;
import plainjavaubt.util.test.BXToolTransformation;
import plainjavaubt.util.test.TestableTransformation;
import plainjavaubt.util.trafo.Transformation;

/**
 * Converts cpm into gantt xmi-files.
 */
public class Cpm2Gantt implements TestableTransformation, BXToolTransformation {
	/**
	 * Converts the given cpm xmi-file to the corresponding gantt xmi-file. The new gantt xmi-file is saved on the same
	 * location as the cpm xmi-file with the Postfix Transformed.
	 * 
	 * @param cpm xmi-file to transform.
	 * @return the location of the created gantt xmi-file.
	 * @throws IOException if saving fails.
	 */
	public File transform(File cpm) throws IOException {
		cpm.CPMNetwork source = null;
		source = Transformation.prepareTransformation(cpm, CpmPackage.eINSTANCE, GanttPackage.eINSTANCE, source);
		gantt.GanttDiagram target = performTransformation(source);
		return Transformation.finishTransformation(cpm, target);
	}
	
	/**
	 * Works like the one parameter version, but you can also specify the target file.
	 */
	public File transform(File cpm, File gantt) throws IOException {
		cpm.CPMNetwork source = null;
		source = Transformation.prepareTransformation(cpm, CpmPackage.eINSTANCE, GanttPackage.eINSTANCE, source);
		gantt.GanttDiagram target = performTransformation(source);
		plainjavaubt.util.io.Xmi.save(gantt, target);
		return gantt;
	}
	
	private gantt.GanttDiagram performTransformation(cpm.CPMNetwork source) {
		GanttFactory factory = GanttFactory.eINSTANCE;
		gantt.GanttDiagram target = factory.createGanttDiagram();
		
		target.setName(source.getName());
		HashMap<Integer, gantt.Activity> activities = new HashMap<>();
		HashSet<Integer> startEvents = new HashSet<>();
		for (cpm.Element activitySource : source.getElements()) {
			if (activitySource instanceof cpm.Activity && !((cpm.Activity) activitySource).getName().contains("->")) {
				gantt.Activity activityTarget = factory.createActivity();
				activityTarget.setName(((cpm.Activity) activitySource).getName());
				activityTarget.setDuration(((cpm.Activity) activitySource).getDuration());
				activityTarget.setDiagram(target);
				activities.put(((cpm.Activity) activitySource).getSourceEvent().getNumber(), activityTarget);
				activities.put(((cpm.Activity) activitySource).getTargetEvent().getNumber(), activityTarget);
				startEvents.add(((cpm.Activity) activitySource).getSourceEvent().getNumber());
			}
		}
		
		for (cpm.Element dependencySource : source.getElements()) {
			if (dependencySource instanceof cpm.Activity
					&& ((cpm.Activity) dependencySource).getName().contains("->")) {
				int sourceEvent = ((cpm.Activity) dependencySource).getSourceEvent().getNumber();
				int targetEvent = ((cpm.Activity) dependencySource).getTargetEvent().getNumber();
				
				gantt.Dependency dependencyTarget = factory.createDependency();
				
				if (startEvents.contains(sourceEvent) && startEvents.contains(targetEvent)) {
					dependencyTarget.setDependencyType(DependencyType.START_START);
				} else if (startEvents.contains(sourceEvent)) {
					dependencyTarget.setDependencyType(DependencyType.START_END);
				} else if (startEvents.contains(targetEvent)) {
					dependencyTarget.setDependencyType(DependencyType.END_START);
				} else {
					dependencyTarget.setDependencyType(DependencyType.END_END);
				}
				
				dependencyTarget.setOffset(((cpm.Activity) dependencySource).getDuration());
				dependencyTarget.setPredecessor(activities.get(sourceEvent));
				dependencyTarget.setSuccessor(activities.get(targetEvent));
				dependencyTarget.setDiagram(target);
			}
		}
		
		return target;
	}
}

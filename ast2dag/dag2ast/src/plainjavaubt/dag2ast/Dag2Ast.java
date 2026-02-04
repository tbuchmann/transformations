package plainjavaubt.dag2ast;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;

import ast.AstFactory;
import ast.AstPackage;
import dag.DagPackage;
import plainjavaubt.util.test.BXToolTransformation;
import plainjavaubt.util.test.TestableTransformation;
import plainjavaubt.util.trafo.Transformation;

/**
 * Converts dag into ast xmi-files.
 */
public class Dag2Ast implements TestableTransformation, BXToolTransformation {
	private static class ExpressionSwitch extends dag.util.DagSwitch<ast.Expression> {
		@Override
		public ast.Expression caseOperator(dag.Operator object) {
			ast.Operator result = AstFactory.eINSTANCE.createOperator();
			result.setOp(object.getOp().equals(dag.ArithmeticOperator.ADD) ? ast.ArithmeticOperator.ADD
					: object.getOp().equals(dag.ArithmeticOperator.SUBTRACT) ? ast.ArithmeticOperator.SUBTRACT
					: object.getOp().equals(dag.ArithmeticOperator.MULTIPLY) ? ast.ArithmeticOperator.MULTIPLY
					: ast.ArithmeticOperator.DIVIDE);
			result.setLeft(doSwitch(object.getLeft()));
			result.setRight(doSwitch(object.getRight()));
			return result;
		}
		
		@Override
		public ast.Expression caseVariable(dag.Variable object) {
			ast.Variable result = AstFactory.eINSTANCE.createVariable();
			result.setName(object.getName());
			return result;
		}
		
		@Override
		public ast.Expression caseNumber(dag.Number object) {
			ast.Number result = AstFactory.eINSTANCE.createNumber();
			result.setValue(object.getValue());
			return result;
		}
	}
	
	/**
	 * Converts the given dag xmi-file to the corresponding ast xmi-file. The new ast xmi-file is saved on the same
	 * location as the dag xmi-file with the Postfix Transformed.
	 * 
	 * @param dag xmi-file to transform.
	 * @return the location of the created ast xmi-file.
	 * @throws IOException if saving fails.
	 */
	public File transform(File dag) throws IOException {
		dag.Model source = null;
		source = Transformation.prepareTransformation(dag, DagPackage.eINSTANCE, AstPackage.eINSTANCE, source);
		ast.Model target = performTransformation(source);
		return Transformation.finishTransformation(dag, target);
	}
	
	/**
	 * Works like the one parameter version, but you can also specify the target file.
	 */
	public File transform(File dag, File ast) throws IOException {
		dag.Model source = null;
		source = Transformation.prepareTransformation(dag, DagPackage.eINSTANCE, AstPackage.eINSTANCE, source);
		ast.Model target = performTransformation(source);
		plainjavaubt.util.io.Xmi.save(ast, target);
		return ast;
	}
	
	private ast.Model performTransformation(dag.Model source) {
		ast.Model target = AstFactory.eINSTANCE.createModel();
		if (!source.getExprs().isEmpty()) {
			LinkedHashSet<dag.Expression> rootCandidates = new LinkedHashSet<>(source.getExprs());
			for (dag.Expression expression : source.getExprs()) {
				if (expression instanceof dag.Operator) {
					rootCandidates.remove(((dag.Operator) expression).getLeft());
					rootCandidates.remove(((dag.Operator) expression).getRight());
				}
			}
			target.setExpr((new ExpressionSwitch()).doSwitch(rootCandidates.iterator().next()));
		}
		return target;
	}
}

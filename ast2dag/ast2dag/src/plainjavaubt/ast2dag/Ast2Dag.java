package plainjavaubt.ast2dag;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

import ast.AstPackage;
import dag.DagFactory;
import dag.DagPackage;
import plainjavaubt.util.test.BXToolTransformation;
import plainjavaubt.util.test.TestableTransformation;
import plainjavaubt.util.trafo.Transformation;

/**
 * Converts ast into dag xmi-files.
 */
public class Ast2Dag implements TestableTransformation, BXToolTransformation {
	private static class ExpressionSwitch extends ast.util.AstSwitch<dag.Expression> {
		private static class OperatorKey {			
			public OperatorKey(dag.ArithmeticOperator op, dag.Expression left, dag.Expression right) {
				this.op = op;
				this.left = left;
				this.right = right;
			}

			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + ((left == null) ? 0 : left.hashCode());
				result = prime * result + ((op == null) ? 0 : op.hashCode());
				result = prime * result + ((right == null) ? 0 : right.hashCode());
				return result;
			}
			
			@Override
			public boolean equals(Object obj) {
				if (this == obj) {
					return true;
				}
				if (obj == null) {
					return false;
				}
				if (getClass() != obj.getClass()) {
					return false;
				}
				OperatorKey other = (OperatorKey) obj;
				if (left == null) {
					if (other.left != null) {
						return false;
					}
				} else if (!left.equals(other.left)) {
					return false;
				}
				if (op != other.op) {
					return false;
				}
				if (right == null) {
					if (other.right != null) {
						return false;
					}
				} else if (!right.equals(other.right)) {
					return false;
				}
				return true;
			}

			private final dag.ArithmeticOperator op;
			private final dag.Expression left;
			private final dag.Expression right;
		}
		
		@Override
		public dag.Expression caseOperator(ast.Operator object) {
			dag.Operator result;
			dag.ArithmeticOperator resultOperator =
					object.getOp().equals(ast.ArithmeticOperator.ADD) ? dag.ArithmeticOperator.ADD
					: object.getOp().equals(ast.ArithmeticOperator.SUBTRACT) ? dag.ArithmeticOperator.SUBTRACT
					: object.getOp().equals(ast.ArithmeticOperator.MULTIPLY) ? dag.ArithmeticOperator.MULTIPLY
					: dag.ArithmeticOperator.DIVIDE;
			dag.Expression left = doSwitch(object.getLeft());
			dag.Expression right = doSwitch(object.getRight());
			
			OperatorKey operatorKey = new OperatorKey(resultOperator, left, right);
			if (operators.containsKey(operatorKey)) {
				result = operators.get(operatorKey);
			} else {
				result = DagFactory.eINSTANCE.createOperator();
				result.setOp(resultOperator);
				result.setLeft(left);
				result.setRight(right);
				operators.put(operatorKey, result);
			}
			
			expressions.put(object, result);
			return result;
		}
		
		@Override
		public dag.Expression caseVariable(ast.Variable object) {
			dag.Variable result;
			if (variables.containsKey(object.getName())) {
				result = variables.get(object.getName());
			} else {
				result = DagFactory.eINSTANCE.createVariable();
				result.setName(object.getName());
				variables.put(object.getName(), result);
			}
			
			expressions.put(object, result);
			return result;
		}
		
		@Override
		public dag.Expression caseNumber(ast.Number object) {
			dag.Number result;
			if (numbers.containsKey(object.getValue())) {
				result = numbers.get(object.getValue());
			} else {
				result = DagFactory.eINSTANCE.createNumber();
				result.setValue(object.getValue());
				numbers.put(object.getValue(), result);
			}
			
			expressions.put(object, result);
			return result;
		}
		
		private HashMap<ast.Expression, dag.Expression> expressions = new HashMap<>();
		private HashMap<OperatorKey, dag.Operator> operators = new HashMap<>();
		private HashMap<String, dag.Variable> variables = new HashMap<>();
		private HashMap<Integer, dag.Number> numbers = new HashMap<>();
	}
	
	/**
	 * Converts the given ast xmi-file to the corresponding dag xmi-file. The new dag xmi-file is saved on the same
	 * location as the ast xmi-file with the Postfix Transformed.
	 * 
	 * @param ast xmi-file to transform.
	 * @return the location of the created dag xmi-file.
	 * @throws IOException if saving fails.
	 */
	public File transform(File ast) throws IOException {
		ast.Model source = null;
		source = Transformation.prepareTransformation(ast, AstPackage.eINSTANCE, DagPackage.eINSTANCE, source);
		dag.Model target = performTransformation(source);
		return Transformation.finishTransformation(ast, target);
	}
	
	/**
	 * Works like the one parameter version, but you can also specify the target file.
	 */
	public File transform(File ast, File dag) throws IOException {
		ast.Model source = null;
		source = Transformation.prepareTransformation(ast, AstPackage.eINSTANCE, DagPackage.eINSTANCE, source);
		dag.Model target = performTransformation(source);
		plainjavaubt.util.io.Xmi.save(dag, target);
		return dag;
	}
	
	private dag.Model performTransformation(ast.Model source) {
		dag.Model target = DagFactory.eINSTANCE.createModel();
		LinkedList<dag.Expression> unvisited = new LinkedList<dag.Expression>();
		if (source.getExpr() != null) {
			unvisited.add((new ExpressionSwitch()).doSwitch(source.getExpr()));
		}
		while (!unvisited.isEmpty()) {
			dag.Expression current = unvisited.pollFirst();
			if (current instanceof dag.Operator) {
				unvisited.addFirst(((dag.Operator) current).getRight());
				unvisited.addFirst(((dag.Operator) current).getLeft());
			}
			target.getExprs().add(current);
		}
		return target;
	}
}
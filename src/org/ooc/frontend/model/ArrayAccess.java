package org.ooc.frontend.model;

import java.io.IOException;

import org.ooc.frontend.Visitor;
import org.ooc.frontend.model.AddressOf;
import org.ooc.frontend.model.OpDecl.OpType;
import org.ooc.frontend.model.interfaces.MustBeResolved;
import org.ooc.frontend.model.tokens.Token;
import org.ooc.middle.OocCompilationError;
import org.ooc.middle.hobgoblins.Resolver;

public class ArrayAccess extends Access implements MustBeResolved {

	Type type;
	protected Expression variable;
	protected NodeList<Expression> indices;

	public ArrayAccess(Expression variable, Token startToken) {
		super(startToken);
		this.variable = variable;
		this.indices =  new NodeList<Expression>(startToken);
	}
	
	public Expression getVariable() {
		return variable;
	}
	
	public void setVariable(Expression variable) {
		this.variable = variable;
	}
	
	public NodeList<Expression> getIndices() {
		return indices;
	}

	public Type getType() {
		if(type == null) {
			Type exprType = variable.getType();
			if(exprType != null) {
				Declaration ref = exprType.getRef();
				if(ref instanceof CoverDecl) {
					Type fromType = ((CoverDecl) ref).getFromType();
					if(fromType != null && fromType.getRef() instanceof CoverDecl) {
						Type clone = fromType.clone();
						clone.setPointerLevel(exprType.getPointerLevel() + fromType.getPointerLevel());
						exprType = clone;
					}
				}
				type = new Type(exprType.getName(), exprType.getPointerLevel() - 1, exprType.startToken);
				type.setRef(exprType.getRef());
			}
		}
		return type;
	}
	
	public void accept(Visitor visitor) throws IOException {
		visitor.visit(this);
	}
	
	public boolean hasChildren() {
		return true;
	}
	
	public void acceptChildren(Visitor visitor) throws IOException {
		variable.accept(visitor);
		indices.accept(visitor);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean replace(Node oldie, Node kiddo) {
		
		if(oldie == variable) {
			variable = (Expression) kiddo;
			return true;
		}
		
		if(oldie == indices) {
			indices = (NodeList<Expression>) kiddo;
			return true;
		}
		
		return false;
		
	}

	public boolean isResolved() {
		return false;
	}

	public Response resolve(NodeList<Node> stack, Resolver res, boolean fatal) {
		
		int assignIndex = -1;
		
		if(stack.peek() instanceof Assignment) {
			Assignment ass = (Assignment) stack.peek();
			if(ass.getLeft() == this) {
				assignIndex = stack.size() - 1;
			} else {
				NodeList<Node> copy = new NodeList<Node>();
				copy.addAll(stack);
				copy.pop();
				Response response = ass.resolve(copy, res, fatal);
				if(response != Response.OK) {
					return response;
				}
			}
		}
		
		OpDecl bestOp = null;
		int bestScore = 0;
		for(OpDecl op: res.module.getOps()) {
			int score = getOpScore(stack, res, assignIndex, op);
			if(bestScore < score) {
				bestOp = op;
				bestScore = score;
			}
		}
		for(Import imp: res.module.getImports()) {
			for(OpDecl op: imp.getModule().getOps()) {
				int score = getOpScore(stack, res, assignIndex, op);
				if(bestScore < score) {
					bestOp = op;
					bestScore = score;
				}
			}
		}
		
		if(bestOp != null) {
			NodeList<Argument> args = bestOp.getFunc().getArguments();
			FunctionCall call = new FunctionCall(bestOp.getFunc(), startToken);
			
			Argument arg = args.getFirst();
			if(arg.getType().getReferenceLevel() == variable.getType().getReferenceLevel() + 1) {
				variable = new AddressOf(variable, startToken);
			}
			
			call.getArguments().add(variable);
			
			for(int argIdx = 0; argIdx < indices.size(); ++argIdx) {
				arg = args.get(argIdx+1);
				Expression exp = indices.get(argIdx);
				
				if(arg.getType().getReferenceLevel() == exp.getType().getReferenceLevel() + 1) {
					exp = new AddressOf(exp, exp.startToken);
				}
				
				call.getArguments().add(exp);
			}
			
			if(assignIndex != -1) {
				Assignment ass = (Assignment)stack.get(assignIndex);
				call.getArguments().add(ass.getRight());
				
				if(!stack.get(assignIndex - 1).replace(ass, call)) {
					System.out.println("stack = "+stack.toString(true));
					Thread.dumpStack();
					throw new OocCompilationError(this, stack, "Couldn't replace array-access-assign with a function call");
				}
			} else {
				stack.peek().replace(this, call);
			}
		}
		
		return Response.OK;
		
	}
	
	private int getOpScore(NodeList<Node> stack, Resolver res, int assignIndex, OpDecl op) throws OocCompilationError {
		int score = 0;
		
		OpType opType = assignIndex == -1 ? OpType.IDX : OpType.IDX_ASS;
		int numArgs = 1 + indices.size() + (opType == OpType.IDX ? 0 : 1);
		
		NodeList<Argument> args = op.getFunc().getArguments();
		if(op.getOpType() != opType || args.size() != numArgs) {
			return 0;
		}
		
		Argument first = args.getFirst();
		if(first.getType().equals(variable.getType())) {
			if(opType == OpType.IDX && args.size() < 2) {
				throw new OocCompilationError(op, stack,
						"To overload the indexing operator, you need at least two arguments, not "
						+op.getFunc().getArgsRepr());
			} else if(opType == OpType.IDX_ASS && args.size() < 3) {
				throw new OocCompilationError(op, stack,
						"To overload the indexed assign operator, you need exactly three arguments, not "
						+op.getFunc().getArgsRepr());
			}
			
			score += 10;
			
			for(int idx = 0; idx < indices.size(); ++idx) {
				Expression exp = indices.get(idx);
				Argument arg = args.get(idx+1);
				
				if(exp instanceof MustBeResolved && ((MustBeResolved)exp).resolve(stack, res, true) != Response.OK) {
					// FIXME I'm not entirely sure how to handle this, so I'm doing this
					// in the least-friendly way possible: death by exception
					throw new OocCompilationError(exp, stack,
							"Unable to determine the type of the expression being used as an index");
				}
				
				if(exp.getType().softEquals(arg.getType(), res)) {
					score += 10;
					if(exp.getType().equals(arg.getType())) {
						score += 10;
					}
				}
			}
			
			if(assignIndex != -1) {
				Argument last = args.getLast();
				Assignment ass = (Assignment)stack.get(assignIndex);
				if(ass.getRight().getType().softEquals(last.getType(), res)) {
					score += 10;
					if(ass.getRight().getType().equals(last.getType())) {
						score += 20;
					}
				}
			}
		}
		
		return score;
	}
	
	@Override
	public String toString() {
		return variable.toString() + indices;
	}
	
}

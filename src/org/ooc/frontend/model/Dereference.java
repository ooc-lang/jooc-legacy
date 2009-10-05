package org.ooc.frontend.model;

import java.io.IOException;

import org.ooc.frontend.Visitor;
import org.ooc.frontend.model.tokens.Token;

public class Dereference extends Access {

	Type type;
	Expression expression;
	
	public Dereference(Expression expression, Token startToken) {
		super(startToken);
		setExpression(expression);
	}

	@Override
	public boolean replace(Node oldie, Node kiddo) {
		if(expression == oldie) {
			expression = (Expression) kiddo;
			return true;
		}
		
		return false;
	}

	public Type getType() {
		if(type == null) {
			Type exprType = expression.getType();
			if(exprType != null) {
				type = exprType.clone();
				type.setPointerLevel(type.getPointerLevel() - 1);
				//type = new Type(exprType.getName(), exprType.getPointerLevel() - 1, exprType.startToken);
				//type.setRef(exprType.getRef());
				if(type.getPointerLevel() < 0) {
					if(type.getRef() instanceof CoverDecl) {
						CoverDecl cover = (CoverDecl) type.getRef();
						System.out.println("Type of deref = "+type+", pointer level = "+type.getPointerLevel());
						System.out.println("cover from type = "+cover.getFromType()+", ref = "+cover.getFromType().getRef());
						if(cover.getFromType() != null && !cover.getFromType().isFlat()) {
							type = cover.getFromType().clone();
							type.setPointerLevel(type.getPointerLevel() - 1);
							System.out.println("Now type = "+type);
						}
					}
				}
			}
		}
		return type;
	}
	
	public Expression getExpression() {
		return expression;
	}
	
	public void setExpression(Expression expression) {
		this.expression = expression;
	}

	public void accept(Visitor visitor) throws IOException {
		visitor.visit(this);
	}

	public void acceptChildren(Visitor visitor) throws IOException {
		expression.accept(visitor);
	}

	public boolean hasChildren() {
		return true;
	}

}

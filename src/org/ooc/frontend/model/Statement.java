package org.ooc.frontend.model;

import org.ooc.frontend.model.tokens.Token;

public abstract class Statement extends Node {

	public Statement(Token startToken) {
		super(startToken);
	}
	
	public void addToModule(Module module) {
		module.getBody().add(this);
	}
	
}

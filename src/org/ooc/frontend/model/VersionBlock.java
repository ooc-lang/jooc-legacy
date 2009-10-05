package org.ooc.frontend.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.ooc.frontend.Visitor;
import org.ooc.frontend.model.VersionNodes.VersionAnd;
import org.ooc.frontend.model.VersionNodes.VersionName;
import org.ooc.frontend.model.VersionNodes.VersionNegation;
import org.ooc.frontend.model.VersionNodes.VersionNode;
import org.ooc.frontend.model.VersionNodes.VersionNodeVisitor;
import org.ooc.frontend.model.VersionNodes.VersionOr;
import org.ooc.frontend.model.VersionNodes.VersionParen;
import org.ooc.frontend.model.interfaces.MustBeResolved;
import org.ooc.frontend.model.tokens.Token;
import org.ooc.middle.OocCompilationError;
import org.ooc.middle.hobgoblins.Resolver;
import org.ubi.CompilationFailedError;

public class VersionBlock extends Block implements MustBeResolved {

	protected static Map<String, String> map = new HashMap<String, String>();

	static {
		
		// Java's excuse for a Map literal
		map.put("windows", 		"__WIN32"); // FIXME: does that imply that we're not 64 ?
		map.put("linux", 		"__linux__");
		map.put("unix", 		"__unix__");
		map.put("beos", 		"__BEOS__");
		map.put("haiku", 		"__HAIKU__");
		map.put("apple", 		"__APPLE__");
		map.put("gnuc", 		"__GNUC__");
		map.put("i386", 		"__i386__");
		map.put("x86", 			"__X86__");
		map.put("x86_64", 		"__X86_64_");
		map.put("64", 			"__X86_64_");
		
	}
	
	private VersionNode version;

	/**
	 * Default constructor
	 * @param location
	 * @param id The id of this version. One of "linux", "windows", etc.
	 */
	public VersionBlock(VersionNode version, Token startToken) {
		super(startToken);
		this.version = version;
	}
	
	public VersionNode getVersion() {
		return version;
	}

	public boolean isResolved() {
		return false;
	}

	public Response resolve(final NodeList<Node> stack, Resolver res, boolean fatal) {
		
		try {
			version.accept(new VersionNodeVisitor() {
				
				public void visit(VersionOr versionOr) throws IOException {
					versionOr.acceptChildren(this);
				}
				
				public void visit(VersionAnd versionAnd) throws IOException {
					versionAnd.acceptChildren(this);
				}
				
				public void visit(VersionNegation versionNegation) throws IOException {
					versionNegation.acceptChildren(this);				
				}
				
				public void visit(VersionName versionName) {
					if(versionName.solved) return;
					String match = map.get(versionName.name.toLowerCase());
					if(match != null) {
						versionName.name = match;
						versionName.solved = true;
					} else {
						System.out.println(new OocCompilationError(VersionBlock.this, stack,
								"Unknown version id: '" + versionName.name
								+ "', compiling anyway (who knows?)").toString());
					}
				}

				public void visit(VersionParen versionParen) throws IOException {
					versionParen.acceptChildren(this);
				}
			});
		} catch (IOException e) {
			throw new CompilationFailedError(e);
		}
		
		return Response.OK;
		
	}
	
	@Override
	public void accept(Visitor visitor) throws IOException {
		visitor.visit(this);
	}

}

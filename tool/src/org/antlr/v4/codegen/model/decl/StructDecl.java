/*
 * Copyright (c) 2012-2017 The ANTLR Project. All rights reserved.
 * Use of this file is governed by the BSD 3-clause license that
 * can be found in the LICENSE.txt file in the project root.
 */

package org.antlr.v4.codegen.model.decl;

import org.antlr.v4.codegen.OutputModelFactory;
import org.antlr.v4.codegen.model.*;
import org.antlr.v4.runtime.misc.OrderedHashSet;
import org.antlr.v4.tool.Attribute;
import org.antlr.v4.tool.Rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** This object models the structure holding all of the parameters,
 *  return values, local variables, and labels associated with a rule.
 */
public class StructDecl extends Decl {
	public String derivedFromName; // rule name or label name
	public boolean provideCopyFrom; // can be used to check if there are named Alts
	@ModelElement public OrderedHashSet<Decl> attrs = new OrderedHashSet<Decl>();
	@ModelElement public OrderedHashSet<Decl> getters = new OrderedHashSet<Decl>();
	@ModelElement public Collection<AttributeDecl> ctorAttrs;
	@ModelElement public List<? super DispatchMethod> dispatchMethods;
	@ModelElement public List<OutputModelObject> interfaces;
	@ModelElement public List<OutputModelObject> extensionMembers;
	// Used to generate method signatures in Go target interfaces
	@ModelElement public OrderedHashSet<Decl> signatures = new OrderedHashSet<Decl>();

	// Track these separately; Go target needs to generate getters/setters
	// Do not make them templates; we only need the Decl object not the ST
	// built from it. Avoids adding args to StructDecl template
	public OrderedHashSet<Decl> tokenDecls = new OrderedHashSet<Decl>();
	public OrderedHashSet<Decl> tokenTypeDecls = new OrderedHashSet<Decl>();
	public OrderedHashSet<Decl> tokenListDecls = new OrderedHashSet<Decl>();
	public OrderedHashSet<Decl> ruleContextDecls = new OrderedHashSet<Decl>();
	public OrderedHashSet<Decl> ruleContextListDecls = new OrderedHashSet<Decl>();
	public OrderedHashSet<Decl> attributeDecls = new OrderedHashSet<Decl>();
	// Required to be able to differently initialize attributes that come from constructor
	// required for Rust target
	public OrderedHashSet<AttributeDecl> notCtorAttrs = new OrderedHashSet<AttributeDecl>();

	public StructDecl(OutputModelFactory factory, Rule r) {
		this(factory, r, null);
	}

	protected StructDecl(OutputModelFactory factory, Rule r, String name) {
		super(factory, name == null ? factory.getGenerator().getTarget().getRuleFunctionContextStructName(r) : name);
		addDispatchMethods(r);
		derivedFromName = r.name;
		provideCopyFrom = r.hasAltSpecificContexts();
	}

	public void addDispatchMethods(Rule r) {
		dispatchMethods = new ArrayList<DispatchMethod>();
		if ( !r.hasAltSpecificContexts() ) {
			// no enter/exit for this ruleContext if rule has labels
			if ( factory.getGrammar().tool.gen_listener ) {
				dispatchMethods.add(new ListenerDispatchMethod(factory, true));
				dispatchMethods.add(new ListenerDispatchMethod(factory, false));
			}
			if ( factory.getGrammar().tool.gen_visitor ) {
				dispatchMethods.add(new VisitorDispatchMethod(factory));
			}
		}
	}

	public void addDecl(Decl d) {
		d.ctx = this;

		if ( d instanceof ContextGetterDecl ) {
			getters.add(d);
			signatures.add(((ContextGetterDecl) d).getSignatureDecl());
		} else {
			attrs.add(d);
		}
		// add to specific "lists"
		if ( d instanceof TokenTypeDecl ) {
			tokenTypeDecls.add(d);
		}
		else if ( d instanceof TokenListDecl ) {
			tokenListDecls.add(d);
		}
		else if ( d instanceof TokenDecl ) {
			tokenDecls.add(d);
		}
		else if ( d instanceof RuleContextListDecl ) {
			ruleContextListDecls.add(d);
		}
		else if ( d instanceof RuleContextDecl ) {
			ruleContextDecls.add(d);
		}
		else if ( d instanceof AttributeDecl ) {
			attributeDecls.add(d);
			AttributeDecl attr = (AttributeDecl) d;
			if (!attr.initFromConstructor) {
				notCtorAttrs.add(attr);
			}
		}
	}

	public void addDecl(Attribute a) {
		addDecl(new AttributeDecl(factory, a));
	}

	public void addDecls(Collection<Attribute> attrList) {
		for (Attribute a : attrList) addDecl(a);
	}

	public void implementInterface(OutputModelObject value) {
		if (interfaces == null) {
			interfaces = new ArrayList<OutputModelObject>();
		}

		interfaces.add(value);
	}

	public void addExtensionMember(OutputModelObject member) {
		if (extensionMembers == null) {
			extensionMembers = new ArrayList<OutputModelObject>();
		}

		extensionMembers.add(member);
	}

	public boolean isEmpty() { return attrs.isEmpty(); }
}

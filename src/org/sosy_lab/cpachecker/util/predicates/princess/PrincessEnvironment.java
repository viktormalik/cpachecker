/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.predicates.princess;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Appenders;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.io.PathCounterTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.counterexample.Model.TermType;

import scala.collection.JavaConversions;
import scala.collection.mutable.ArrayBuffer;
import ap.SimpleAPI;
import ap.basetypes.IdealInt;
import ap.parser.IAtom;
import ap.parser.IBoolLit;
import ap.parser.IConstant;
import ap.parser.IExpression;
import ap.parser.IFormula;
import ap.parser.IFormulaITE;
import ap.parser.IFunApp;
import ap.parser.IFunction;
import ap.parser.IIntLit;
import ap.parser.ITerm;
import ap.parser.ITermITE;

/** This is a Wrapper around Princess.
 * This Wrapper allows to set a logfile for all Smt-Queries (default "princess.###.smt2").
 * // TODO logfile is only available as tmpfile in /tmp, perhaps it is not closed?
 * It also manages the "shared variables": each variable is declared for all stacks.
 */
class PrincessEnvironment {

  /** cache for variables, because they do not implement equals() and hashCode(),
   * so we need to have the same objects. */
  private final Map<String, IFormula> boolVariablesCache = new HashMap<>();
  private final Map<String, ITerm> intVariablesCache = new HashMap<>();
  private final Map<String, IFunction> functionsCache = new HashMap<>();
  private final Map<IFunction, TermType> functionsReturnTypes = new HashMap<>();

  private final @Nullable PathCounterTemplate basicLogfile;

  /** the wrapped api is the first created api.
   * It will never be used outside of this class and never be closed.
   * If a variable is declared, it is declared in the first api, then copied into all registered apis.
   * Each api has its own stack for formulas. */
  private final SimpleAPI api;
  private final List<SymbolTrackingPrincessStack> registeredStacks = new ArrayList<>();

  /** The Constructor creates the wrapped Element, sets some options
   * and initializes the logger. */
  public PrincessEnvironment(Configuration config, final LogManager pLogger,
      final PathCounterTemplate pBasicLogfile) {
    basicLogfile = pBasicLogfile;
    api = getNewApi(false); // this api is only used local in this environment, no need for interpolation
  }


  /** This method returns a new stack, that is registered in this environment.
   * All variables are shared in all registered stacks. */
  PrincessStack getNewStack(boolean useForInterpolation) {
    SimpleAPI newApi = getNewApi(useForInterpolation);
    SymbolTrackingPrincessStack stack = new SymbolTrackingPrincessStack(this, newApi);

    // add all symbols, that are available until now
    for (IFormula s : boolVariablesCache.values()) {
      stack.addSymbol(s);
    }
    for (ITerm s : intVariablesCache.values()) {
      stack.addSymbol(s);
    }
    for (IFunction s : functionsCache.values()) {
      stack.addSymbol(s);
    }

    registeredStacks.add(stack);
    return stack;
  }

  private SimpleAPI getNewApi(boolean useForInterpolation) {
    final SimpleAPI newApi;
    if (basicLogfile != null) {
      newApi = SimpleAPI.spawnWithLogNoSanitise(basicLogfile.getFreshPath().getAbsolutePath());
    } else {
      newApi = SimpleAPI.spawnNoSanitise();
    }
    // we do not use 'sanitise', because variable-names contain special chars like "@" and ":"

    if (useForInterpolation) {
      newApi.setConstructProofs(true); // needed for interpolation
    }
    return newApi;
  }

  void unregisterStack(SymbolTrackingPrincessStack stack) {
    assert registeredStacks.contains(stack) : "cannot remove stack, it is not registered";
    registeredStacks.remove(stack);
  }

  public List<IExpression> parseStringToTerms(String s) {
    throw new UnsupportedOperationException(); // todo: implement this
  }

  public Appender dumpFormula(final IExpression formula) {
    return new Appenders.AbstractAppender() {

      @Override
      public void appendTo(Appendable out) throws IOException {
        Set<IExpression> declaredFunctions = PrincessUtil.getVarsAndUIFs(Collections.singleton(formula));

        for (IExpression var : declaredFunctions) {
          out.append("(declare-fun ");
          out.append(getName(var));

          // function parameters
          out.append(" (");
          if (var instanceof IFunApp) {
            IFunApp function = (IFunApp) var;
            Iterator<ITerm> args = JavaConversions.asJavaIterable(function.args()).iterator();
            while (args.hasNext()) {
              args.next();
              // Princess does only support IntegerFormulas in UIFs we don't need
              // to check the type here separately
              if (args.hasNext()) {
                out.append("Int ");
              } else {
                out.append("Int");
              }
            }
          }

          out.append(") ");
          out.append(getType(var));
          out.append(")\n");
        }

        out.append("(assert ");
        out.append(formula.toString());
        out.append(")");
      }
    };
  }

  private String getName(IExpression var) {
    if (var instanceof IAtom) {
      return ((IAtom) var).pred().name();
    } else if (var instanceof IConstant) {
      return ((IConstant)var).toString();
    } else if (var instanceof IFunApp) {
      String fullStr = ((IFunApp)var).fun().toString();
      return fullStr.substring(0, fullStr.indexOf("/"));
    }

    throw new IllegalArgumentException("The given parameter is no variable or function");
  }

  private String getType(IExpression var) {
    if (var instanceof IFormula) {
      return "Bool";

      // functions are included here, they cannot be handled separate for princess
    } else if (var instanceof ITerm) {
      return "Int";
    }

    throw new IllegalArgumentException("The given parameter is no variable or function");
  }

  public IExpression makeVariable(TermType type, String varname) {
    switch (type) {

      case Boolean: {
        if (boolVariablesCache.containsKey(varname)) {
          return boolVariablesCache.get(varname);
        } else {
          IFormula var = api.createBooleanVariable(varname);
          for (SymbolTrackingPrincessStack stack : registeredStacks) {
            stack.addSymbol(var);
          }
          boolVariablesCache.put(varname, var);
          return var;
        }
      }

      case Integer: {
        if (intVariablesCache.containsKey(varname)) {
          return intVariablesCache.get(varname);
        } else {
          ITerm var = api.createConstant(varname);
          for (SymbolTrackingPrincessStack stack : registeredStacks) {
            stack.addSymbol(var);
          }
          intVariablesCache.put(varname, var);
          return var;
        }
      }

      default:
        throw new AssertionError("unsupported type: " + type);
    }
  }

  /** This function declares a new functionSymbol, that has a given number of params.
   * Princess has no support for typed params, only their number is important. */
  public IFunction declareFun(String name, int nofArgs, TermType returnType) {
    if (functionsCache.containsKey(name)) {
      assert returnType == functionsReturnTypes.get(functionsCache.get(name));
      return functionsCache.get(name);

    } else {
      IFunction funcDecl = api.createFunction(name, nofArgs);
      for (SymbolTrackingPrincessStack stack : registeredStacks) {
         stack.addSymbol(funcDecl);
      }
      functionsCache.put(name, funcDecl);
      functionsReturnTypes.put(funcDecl, returnType);
      return funcDecl;
    }
  }

  TermType getReturnTypeForFunction(IFunction fun) {
    return functionsReturnTypes.get(fun);
  }
  public IExpression makeFunction(IFunction funcDecl, List<IExpression> args) {
    checkArgument(args.size() == funcDecl.arity(),
        "functiontype has different number of args.");

    final ArrayBuffer<ITerm> argsBuf = new ArrayBuffer<>();
    for (IExpression arg : args) {
      ITerm termArg;
      if (arg instanceof IFormula) { // boolean term -> build ITE(t,0,1), TODO why not ITE(t,1,0) ??
        termArg = new ITermITE((IFormula)arg, new IIntLit(IdealInt.ZERO()), new IIntLit(IdealInt.ONE()));
      } else {
        termArg = (ITerm) arg;
      }
      argsBuf.$plus$eq(termArg);
    }

    IExpression returnFormula = new IFunApp(funcDecl, argsBuf.toSeq());
    TermType returnType = getReturnTypeForFunction(funcDecl);

    // boolean term -> build ITE(t > 0, true, false)
    if (returnType == TermType.Boolean) {
      IFormula condition = ((ITerm)returnFormula).$greater(new IIntLit(IdealInt.ZERO()));
      returnFormula = new IFormulaITE(condition, new IBoolLit(true), new IBoolLit(false));

    } else if (returnType != TermType.Integer) {
      throw new AssertionError("Not possible to have return types for functions other than bool or int.");
    }

    return returnFormula;
  }

  public String getVersion() {
    return "Princess (unknown version)";
  }
}

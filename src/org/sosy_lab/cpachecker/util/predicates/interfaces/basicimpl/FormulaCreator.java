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
package org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FloatingPointFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;


/**
 * This is a helper class with several methods that are commonly used
 * throughout the basicimpl package and may have solver-specific implementations.
 * Each solver package is expected to provide an instance of this class,
 * with the appropriate methods overwritten.
 * Depending on the solver, some or all non-final methods in this class
 * may need to be overwritten.
 * @param <TFormulaInfo> the solver specific type for formulas.
 * @param <TType> the solver specific type for formula types.
 * @param <TEnv> the solver specific type for the environment/context.
 */
public abstract class FormulaCreator<TFormulaInfo, TType, TEnv> {

  private final TType boolType;
  private final TType integerType;
  private final TType rationalType;
  private final TEnv environment;

  protected FormulaCreator(
      TEnv env,
      TType boolType,
      TType pIntegerType,
      TType pRationalType
      ) {
    this.environment = env;
    this.boolType = boolType;
    this.integerType = pIntegerType;
    this.rationalType = pRationalType;
  }

  public final TEnv getEnv() {
    return environment;
  }

  public TType getBoolType() {
    return boolType;
  }

  public TType getIntegerType() {
    return integerType;
  }

  public TType getRationalType() {
    return rationalType;
  }

  public abstract TType getBitvectorType(int bitwidth);

  public abstract TFormulaInfo makeVariable(TType type, String varName);

  public BooleanFormula encapsulateBoolean(TFormulaInfo pTerm) {
    return new BooleanFormulaImpl<>(pTerm);
  }

  protected BitvectorFormula encapsulateBitvector(TFormulaInfo pTerm) {
    return new BitvectorFormulaImpl<>(pTerm);
  }

  protected FloatingPointFormula encapsulateFloatingPoint(TFormulaInfo pTerm) {
    return new FloatingPointFormulaImpl<>(pTerm);
  }

  @SuppressWarnings("unchecked")
  protected <T extends Formula> T encapsulate(FormulaType<T> pType, TFormulaInfo pTerm) {
    if (pType.isBooleanType()) {
      return (T)new BooleanFormulaImpl<>(pTerm);
    } else if (pType.isIntegerType()) {
      return (T)new IntegerFormulaImpl<>(pTerm);
    } else if (pType.isRationalType()) {
      return (T)new RationalFormulaImpl<>(pTerm);
    } else if (pType.isBitvectorType()) {
      return (T)new BitvectorFormulaImpl<>(pTerm);
    } else if (pType.isFloatingPointType()) {
      return (T)new FloatingPointFormulaImpl<>(pTerm);
    }
    throw new IllegalArgumentException("Cannot create formulas of type " + pType + " in MathSAT");
  }

  @SuppressWarnings("unchecked")
  protected TFormulaInfo extractInfo(Formula pT) {
    return ((AbstractFormula<TFormulaInfo>)pT).getFormulaInfo();
  }


  /**
   * Returns the type of the given Formula.
   */
  @SuppressWarnings("unchecked")
  protected <T extends Formula> FormulaType<T> getFormulaType(T formula) {
    checkNotNull(formula);
    FormulaType<?> t;
    if (formula instanceof BooleanFormula) {
      t = FormulaType.BooleanType;
    } else if (formula instanceof IntegerFormula) {
      t = FormulaType.IntegerType;
    } else if (formula instanceof RationalFormula) {
      t = FormulaType.RationalType;
    } else if (formula instanceof BitvectorFormula) {
      throw new UnsupportedOperationException("SMT solvers with support for bitvectors needs to overwrite FormulaCreator.getFormulaType()");
    } else {
      throw new IllegalArgumentException("Formula with unexpected type " + formula.getClass());
    }
    return (FormulaType<T>) t;
  }

  protected abstract FormulaType<?> getFormulaType(TFormulaInfo formula);
}
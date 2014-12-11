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
package org.sosy_lab.cpachecker.util.precondition.segkro.rules.tests;

import static com.google.common.truth.Truth.assertThat;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.precondition.segkro.rules.EliminationRule;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory.Solvers;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType.NumeralType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.z3.matching.SmtAstMatcher;
import org.sosy_lab.cpachecker.util.predicates.z3.matching.Z3AstMatcher;
import org.sosy_lab.cpachecker.util.test.SolverBasedTest0;

import com.google.common.collect.Lists;


public class EliminationRuleTest0 extends SolverBasedTest0 {

  private SmtAstMatcher matcher;
  private Solver solver;
  private FormulaManagerView mgrv;

  private EliminationRule er;

  @Override
  protected Solvers solverToUse() {
    return Solvers.Z3;
  }

  @Before
  public void setUp() throws Exception {
    mgrv = new FormulaManagerView(mgr, config, logger);
    solver = new Solver(mgrv, factory);

    matcher = new Z3AstMatcher(logger, mgr, mgrv);
    er = new EliminationRule(mgr, mgrv, solver, matcher);
  }

  @Test
  public void testElim1() throws SolverException, InterruptedException {

    IntegerFormula _c1 = mgrv.makeVariable(NumeralType.IntegerType, "c1");
    IntegerFormula _c2 = mgrv.makeVariable(NumeralType.IntegerType, "c2");
    IntegerFormula _e1 = mgrv.makeVariable(NumeralType.IntegerType, "e1");
    IntegerFormula _e2 = mgrv.makeVariable(NumeralType.IntegerType, "e2");
    IntegerFormula _eX = mgrv.makeVariable(NumeralType.IntegerType, "eX");
    IntegerFormula _0 = imgr.makeNumber(0);

    // Formulas for the premise
    BooleanFormula _c1_times_ex_plus_e1_GEQ_0
      = imgr.greaterOrEquals(
          imgr.add(
              imgr.multiply(_c1, _eX),
              _e1),
          _0);
    BooleanFormula _minus_c2_times_ex_plus_e2_GEQ_0
      = imgr.greaterOrEquals(
          imgr.add(
              imgr.multiply(
                  imgr.subtract(_0, _c2),
                  _eX),
              _e2),
          _0);

    // The formula that is expected as conclusion
    BooleanFormula expectedConclusion
      = imgr.greaterOrEquals(
          imgr.subtract(
              imgr.multiply(_c2, _e1),
              imgr.multiply(_c1, _e2)),
          _0);

    // Check if the expected conclusion is implied by the conjunction of the premises
    Set<BooleanFormula> concluded = er.applyWithInputRelatingPremises(
        Lists.newArrayList(
            _c1_times_ex_plus_e1_GEQ_0,
            _minus_c2_times_ex_plus_e2_GEQ_0));

    assertThat(concluded).isNotEmpty();
    assertThat(concluded.iterator().next().toString()).isEqualTo(expectedConclusion.toString());
  }
}
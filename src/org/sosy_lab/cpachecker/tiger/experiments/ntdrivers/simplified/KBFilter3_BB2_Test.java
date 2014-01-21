/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.tiger.experiments.ntdrivers.simplified;

import java.util.LinkedList;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.cpachecker.tiger.core.CPAtigerResult;
import org.sosy_lab.cpachecker.tiger.experiments.ExperimentalSeries;
import org.sosy_lab.cpachecker.tiger.fql.PredefinedCoverageCriteria;

public class KBFilter3_BB2_Test extends ExperimentalSeries {

  @Test
  public void test003() throws Exception {
    /*String[] lArguments = Main.getParameters(Main.BASIC_BLOCK_2_COVERAGE,
                                        "test/programs/fql/ntdrivers-simplified/kbfiltr_simpl2.cil.c",
                                        "main",
                                        true);

    FShell3Result lResult = execute(lArguments);*/


    String lCFile = "kbfiltr_simpl2.cil.c";

    LinkedList<String> lArguments = new LinkedList<>();

    lArguments.add(PredefinedCoverageCriteria.BASIC_BLOCK_2_COVERAGE);
    lArguments.add("test/programs/fql/ntdrivers-simplified/" + lCFile);
    lArguments.add("main");
    lArguments.add("--withoutCilPreprocessing");
    lArguments.add("--nooutput");

    String[] lArgs = new String[lArguments.size()];
    lArguments.toArray(lArgs);

    CPAtigerResult lResult = execute(lArgs);


    Assert.assertEquals(690, lResult.getNumberOfTestGoals());
    Assert.assertEquals(-1, lResult.getNumberOfFeasibleTestGoals());
    Assert.assertEquals(-1, lResult.getNumberOfInfeasibleTestGoals());
    Assert.assertEquals(1, lResult.getNumberOfTestCases());
    Assert.assertEquals(0, lResult.getNumberOfImpreciseTestCases());

    /**
     * Discussion: get_exit_nondet() in its original implementation is faulty
     */
    Assert.assertTrue(false);
  }
}

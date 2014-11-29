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
package org.sosy_lab.cpachecker.util.predicates.z3.matching;

import java.util.Collection;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;



public class SmtAstPatternImpl implements SmtAstPattern {

  public static enum PatternLogic {
    ALL, ANY, NONE
  }

  public final Optional<Comparable<?>> function;
  public final Optional<String> bindMatchTo;
  public final ImmutableList<SmtAstPattern> argumentMatchers;
  public final PatternLogic argumentToSatisfy;

  public SmtAstPatternImpl(
      Optional<Comparable<?>> pFunction,
      Optional<String> pBindMatchTo,
      Collection<SmtAstPattern> pArgumentMatchers,
      PatternLogic pArgumentToSatisfy) {

    this.function = pFunction;
    this.bindMatchTo = pBindMatchTo;
    this.argumentMatchers = ImmutableList.copyOf(pArgumentMatchers);
    this.argumentToSatisfy = pArgumentToSatisfy;
  }

}
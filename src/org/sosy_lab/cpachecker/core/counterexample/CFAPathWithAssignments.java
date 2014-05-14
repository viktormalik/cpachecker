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
package org.sosy_lab.cpachecker.core.counterexample;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.Model.AssignableTerm;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;


/**
 * This class represents a path of cfaEdges, that contain the additional Information
 * at which edge which assignableTerm was created when this path was checked by
 * the class {@link PathChecker}.
 *
 */
public class CFAPathWithAssignments implements Iterable<CFAEdgeWithAssignments> {

  private final List<CFAEdgeWithAssignments> pathWithAssignments;
  private final Multimap<CFAEdge, AssignableTerm> assignableTerms;

  public CFAPathWithAssignments(
      List<CFAEdgeWithAssignments> pPathWithAssignments,
      Multimap<CFAEdge, AssignableTerm> pAssignableTerms) {
    pathWithAssignments = ImmutableList.copyOf(pPathWithAssignments);
    assignableTerms = ImmutableMultimap.copyOf(pAssignableTerms);
  }

  @Nullable
  public CFAPathWithAssignments getExactVariableValues(List<CFAEdge> pPath) {

    if (fitsPath(pPath)) {
      return this;
    }

    int index = pathWithAssignments.size() - pPath.size();

    if (index < 0) {
      return null;
    }

    List<CFAEdgeWithAssignments> result;

    result = new ArrayList<>(pPath.size());

    for (CFAEdge edge : pPath) {

      if (index > pathWithAssignments.size()) {
        return null;
      }

      CFAEdgeWithAssignments cfaWithAssignment = pathWithAssignments.get(index);

      if (!edge.equals(cfaWithAssignment.getCFAEdge())) {
        return null;
      }

      result.add(cfaWithAssignment);
      index++;
    }

    return new CFAPathWithAssignments(result, assignableTerms);
  }

  private boolean fitsPath(List<CFAEdge> pPath) {

    int index = 0;

    for (CFAEdge edge : pPath) {

      if (index > pathWithAssignments.size()) {
        return false;
      }

      CFAEdgeWithAssignments cfaWithAssignment = pathWithAssignments.get(index);

      if (!edge.equals(cfaWithAssignment.getCFAEdge())) {
        return false;
      }

      index++;

      return true;
    }

    return false;
  }

  @Nullable
  public Map<ARGState, CFAEdgeWithAssignments> getExactVariableValues(ARGPath pPath) {

    if (pPath.isEmpty() || pPath.size() != pathWithAssignments.size()) {
      return null;
    }

    Map<ARGState, CFAEdgeWithAssignments> result = new HashMap<>();

    int index = 0;

    for (Pair<ARGState, CFAEdge> argPair : pPath) {

      CFAEdgeWithAssignments edgeWithAssignment = pathWithAssignments.get(index);

      if (!edgeWithAssignment.getCFAEdge().equals(argPair.getSecond())) {
        // path is not equivalent
        return null;
      }

      result.put(argPair.getFirst(), edgeWithAssignment);
      index++;
    }

    return result;
  }

  public Collection<AssignableTerm> getAllAssignedTerms() {
    return assignableTerms.values();
  }

  public Collection<AssignableTerm> getAllAssignedTerms(CFAEdge edge) {
    return assignableTerms.get(edge);
  }

  public boolean isEmpty() {
    return pathWithAssignments.isEmpty();
  }

  public CFAPathWithAssignments() {
    pathWithAssignments = ImmutableList.of();
    assignableTerms = ImmutableMultimap.of();
  }

  @Override
  public String toString() {
    return pathWithAssignments.toString();
  }

  public CFAEdge getCFAEdgeAtPosition(int index) {
    return pathWithAssignments.get(index).getCFAEdge();
  }

  public int size() {
    return pathWithAssignments.size();
  }

  @Override
  public Iterator<CFAEdgeWithAssignments> iterator() {
    return pathWithAssignments.iterator();
  }
}
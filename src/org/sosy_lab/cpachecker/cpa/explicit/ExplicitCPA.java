/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.explicit;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.StopNeverOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithABM;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitPrecision.CegarPrecision;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;

@Options(prefix="cpa.explicit")
public class ExplicitCPA implements ConfigurableProgramAnalysisWithABM, StatisticsProvider {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ExplicitCPA.class);
  }

  @Option(name="merge", toUppercase=true, values={"SEP", "JOIN"},
      description="which merge operator to use for ExplicitCPA")
  private String mergeType = "SEP";

  @Option(name="stop", toUppercase=true, values={"SEP", "JOIN", "NEVER"},
      description="which stop operator to use for ExplicitCPA")
  private String stopType = "SEP";

  @Option(name="variableBlacklist",
      description="blacklist regex for variables that won't be tracked by ExplicitCPA")
  private String variableBlacklist = "";

  @Option(description="get an initial precison from file")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private File initialPrecisionFile = null;

  private ExplicitPrecision precision;

  private AbstractDomain abstractDomain;
  private MergeOperator mergeOperator;
  private StopOperator stopOperator;
  private TransferRelation transferRelation;
  private PrecisionAdjustment precisionAdjustment;
  private final ExplicitReducer reducer;
  private final ExplicitCPAStatistics statistics;

  private final Configuration config;
  private final LogManager logger;
  private final MachineModel machineModel;

  private ExplicitCPA(Configuration config, LogManager logger, CFA cfa) throws InvalidConfigurationException {
    this.config = config;
    this.logger = logger;
    this.machineModel = cfa.getMachineModel();

    config.inject(this);

    abstractDomain      = new ExplicitDomain();
    transferRelation    = new ExplicitTransferRelation(config);
    precision           = initializePrecision(config, cfa);
    mergeOperator       = initializeMergeOperator();
    stopOperator        = initializeStopOperator();
    precisionAdjustment = StaticPrecisionAdjustment.getInstance();
    reducer             = new ExplicitReducer();
    statistics          = new ExplicitCPAStatistics(this);
  }

  private MergeOperator initializeMergeOperator() {
    if (mergeType.equals("SEP")) {
      return MergeSepOperator.getInstance();
    }

    else if (mergeType.equals("JOIN")) {
      return new MergeJoinOperator(abstractDomain);
    }

    return null;
  }

  private StopOperator initializeStopOperator() {
    if (stopType.equals("SEP")) {
      return new StopSepOperator(abstractDomain);
    }

    else if (stopType.equals("JOIN")) {
      return new StopJoinOperator(abstractDomain);
    }

    else if (stopType.equals("NEVER")) {
      return new StopNeverOperator();
    }

    return null;
  }

  private ExplicitPrecision initializePrecision(Configuration config, CFA cfa) throws InvalidConfigurationException {
    return new ExplicitPrecision(variableBlacklist, config, cfa.getVarClassification(), restoreMappingFromFile(cfa));
  }

  private Multimap<CFANode, String> restoreMappingFromFile(CFA cfa) throws InvalidConfigurationException {
    Multimap<CFANode, String> mapping = HashMultimap.create();
    if (initialPrecisionFile == null) {
      return mapping;
    }

    List<String> variablesAtLocations = null;
    try {
      variablesAtLocations = Files.readLines(initialPrecisionFile, Charset.defaultCharset());
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not read predicate map from file named " + initialPrecisionFile);
      return mapping;
    }

    Map<Integer, CFANode> idToCfaNode = createMappingForCFANodes(cfa);
    for (String variablesAtLocation : variablesAtLocations) {
      String[] splits = variablesAtLocation.split("=");

      CFANode location = idToCfaNode.get(Integer.parseInt(splits[0].substring(1)));

      if (location != null) {
        String variables = splits[1].substring(1, splits[1].length() - 1);
        mapping.putAll(location, Arrays.asList(variables.split(CegarPrecision.DELIMITER)));
      }
    }

    return mapping;
  }

  private Map<Integer, CFANode> createMappingForCFANodes(CFA cfa) {
    Map<Integer, CFANode> idToNodeMap = Maps.newHashMap();
    for (CFANode n : cfa.getAllNodes()) {
      idToNodeMap.put(n.getNodeNumber(), n);
    }
    return idToNodeMap;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public AbstractState getInitialState(CFANode node) {
    return new ExplicitState();
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return precision;
  }

  ExplicitPrecision getPrecision() {
    return precision;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  public Configuration getConfiguration() {
    return config;
  }

  public LogManager getLogger() {
    return logger;
  }

  public MachineModel getMachineModel() {
    return machineModel;
  }

  @Override
  public Reducer getReducer() {
    return reducer;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(statistics);
  }

  public ExplicitCPAStatistics getStats() {
    return statistics;
  }
}

package org.sosy_lab.cpachecker.cpa.policyiteration;

import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.loopstack.LoopstackCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;

import com.google.common.collect.Iterables;

/**
 * Start with unguided precision: increase precision at each refiner call.
 */
@Options(prefix="cpa.stator.policy")
public class PolicyUnguidedRefiner implements Refiner {

  @Option(secure=true,
      description="Number of refinements after which the unrolling depth is increased")
  private int unrollingRefinementThreshold = 2;

  private final PolicyCPA policyCPA;
  private final LoopstackCPA loopstackCPA;
  private final LogManager logger;

  private int refinementsPerformed = 0;

  public PolicyUnguidedRefiner(PolicyCPA pPolicyCPA, LoopstackCPA pLoopstackCPA,
      LogManager pLogger, Configuration pConfig) throws InvalidConfigurationException{
    pConfig.inject(this);

    policyCPA = pPolicyCPA;
    loopstackCPA = pLoopstackCPA;
    logger = pLogger;
  }


  public static PolicyUnguidedRefiner create(
      final ConfigurableProgramAnalysis pCpa
  ) throws InvalidConfigurationException {

    PolicyCPA policyCPA = CPAs.retrieveCPA(pCpa, PolicyCPA.class);
    LoopstackCPA loopstackCPA = CPAs.retrieveCPA(pCpa, LoopstackCPA.class);
    if (policyCPA == null) {
      throw new InvalidConfigurationException(
          PolicyUnguidedRefiner.class.getSimpleName() + " needs a ValueAnalysisCPA"
      );
    }

    return new PolicyUnguidedRefiner(policyCPA, loopstackCPA, policyCPA.getLogger(),
        policyCPA.getConfig());
  }

  @Override
  public boolean performRefinement(ReachedSet pReached)
      throws CPAException, InterruptedException {
    boolean out = policyCPA.adjustPrecision();
    if (out) {

      if (refinementsPerformed == unrollingRefinementThreshold
            && loopstackCPA != null) {
        loopstackCPA.incLoopIterationsBeforeAbstraction();
      }

      // Keep performing refinement.
      forceRestart(pReached);
      logger.log(Level.INFO, "LPI: Additional refinement requested.");
      refinementsPerformed++;
      return true;
    } else {
      // No more tricks up our sleeves, do not clear the reached set.
      logger.log(Level.INFO, "LPI: No more refinement possible.");
      return false;
    }
  }

  public void forceRestart(ReachedSet reached) {
    ARGState firstChild = Iterables
        .getOnlyElement(((ARGState)reached.getFirstState()).getChildren());

    new ARGReachedSet(reached).removeSubtree(firstChild);
  }
}

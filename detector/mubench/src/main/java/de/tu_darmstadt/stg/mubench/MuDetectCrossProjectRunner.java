package de.tu_darmstadt.stg.mubench;

import de.tu_darmstadt.stg.mubench.cli.MuBenchRunner;


public class MuDetectCrossProjectRunner {
    public static void main(String[] args) throws Exception {
        //TODO (myCode)
        args = MuDetectRunner.supplyArgs(args);
        new MuBenchRunner()
                .withDetectOnlyStrategy(new CrossProjectStrategyForWholeProject())
                .withMineAndDetectStrategy(new CrossProjectStrategy())
                .run(args);
    }
}

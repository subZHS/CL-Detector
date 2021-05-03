package de.tu_darmstadt.stg.mubench;

import de.tu_darmstadt.stg.mubench.cli.DetectorArgs;
import de.tu_darmstadt.stg.mubench.cli.DetectorOutput;
import de.tu_darmstadt.stg.mudetect.aug.model.APIUsageExample;
import edu.iastate.cs.egroum.aug.AUGBuilder;
import edu.iastate.cs.egroum.aug.TypeUsageExamplePredicate;

import java.util.*;

public class CrossProjectStrategyForWholeProject extends CrossProjectStrategy {

    @Override
    public Set<String> involveAPIs(List<TargetProject> targetProjectList) throws Exception {
        Set<String> minedForAPIs = super.involveAPIs(targetProjectList);
//        Set<String> minedForAPIs = super.involveAPIs(TargetProject.find(CrossProjectStrategy.getIndexFilePath(), null));
        return minedForAPIs;
    }

    @Override
    public Collection<APIUsageExample> loadDetectionTargets(DetectorArgs args, List<TargetProject> targetProjectList) throws Exception {
        Collection<APIUsageExample> targetExamples = new ArrayList<>();
        for(TargetProject targetProject: targetProjectList){
            AUGBuilder builder = new AUGBuilder(new DefaultAUGConfiguration() {{
                String[] apis = involveAPIs(Arrays.asList(targetProject)).toArray(new String[0]);
                usageExamplePredicate = TypeUsageExamplePredicate.usageExamplesOf(apis);
            }});
            targetExamples.addAll(builder.build(args.getTargetSrcPaths(), args.getDependencyClassPath()));
        }
        return targetExamples;
    }
}

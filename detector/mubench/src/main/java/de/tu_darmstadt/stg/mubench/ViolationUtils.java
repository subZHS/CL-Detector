package de.tu_darmstadt.stg.mubench;

import de.tu_darmstadt.stg.mubench.cli.DetectorFinding;
import de.tu_darmstadt.stg.mudetect.aug.model.APIUsageExample;
import de.tu_darmstadt.stg.mudetect.aug.model.Location;
import de.tu_darmstadt.stg.mudetect.aug.model.patterns.APIUsagePattern;
import de.tu_darmstadt.stg.mudetect.dot.ViolationDotExporter;
import de.tu_darmstadt.stg.mudetect.model.Violation;

import java.util.Set;
import java.util.stream.Collectors;

public class ViolationUtils {
    private static final ViolationDotExporter violationDotExporter = new ViolationDotExporter();
    public static final String CHECKOUTS_PATH_SUFFIX = "checkouts/";
    public static final int UNKNOWN_LINE = -1;

    public static DetectorFinding toFinding(Violation violation) {
        Location location = violation.getLocation();
        DetectorFinding finding = new DetectorFinding(location.getFilePath(), location.getMethodSignature());
        finding.put("pattern_violation", violationDotExporter.toDotGraph(violation));
        finding.put("target_environment_mapping", violationDotExporter.toTargetEnvironmentDotGraph(violation));
        finding.put("confidence", violation.getConfidence());
        finding.put("pattern_support", violation.getOverlap().getPattern().getSupport());
        finding.put("confidence_string", violation.getConfidenceString());
        finding.put("pattern_examples", getPatternInstanceLocations(violation.getOverlap().getPattern()));
        finding.put("startline", getStartLine(violation));
        String ruleApi = violation.getOverlap().getPattern().getRuleApi();
        if(ruleApi!=null){
            finding.put("ruleAPI", ruleApi);
        }
        return finding;
    }

    //TODO (myCode) 将violation参数变成pattern
    public static Set<String> getPatternInstanceLocations(APIUsagePattern pattern) {
        return pattern.getExampleLocations().stream()
                .map(ViolationUtils::getLocationString).distinct().limit(5)
                .collect(Collectors.toSet());
    }

    public static Location parseLocation(String str){
        String[] splits = str.split("#");
        if (splits.length!=3){
            throw new RuntimeException("cannot parse Location from str: "+ str);
        }
        return new Location(splits[0], splits[1], splits[2]);
    }

    private static String getLocationString(Location loc) {
        String filePath = loc.getFilePath();
        int startOfCheckoutsSubPath = filePath.indexOf(CHECKOUTS_PATH_SUFFIX);
        if (startOfCheckoutsSubPath > -1) {
            startOfCheckoutsSubPath += CHECKOUTS_PATH_SUFFIX.length() - 1;
        }
        return loc.getProjectName() + "#" + filePath.substring(startOfCheckoutsSubPath + 1) + "#" + loc.getMethodSignature();
    }

    private static int getStartLine(Violation violation) {
        APIUsageExample target = violation.getOverlap().getTarget();
        int startLine = violation.getOverlap().getMappedTargetNodes().stream()
                .mapToInt(node -> target.getSourceLineNumber(node).orElse(Integer.MAX_VALUE))
                .min().orElse(UNKNOWN_LINE);
        return (startLine == Integer.MAX_VALUE) ? UNKNOWN_LINE : startLine;
    }

}

package sealab.burt.qualitychecker;

import sealab.burt.nlparser.euler.actions.nl.NLAction;
import sealab.burt.qualitychecker.graph.AppGraphInfo;

import java.util.List;

import static sealab.burt.qualitychecker.QualityResult.Result.MULTIPLE_MATCH;
import static sealab.burt.qualitychecker.QualityResult.Result.NO_MATCH;

public class S2RChecker {

    private final String appName;
    private final String appVersion;

    public S2RChecker(String appName, String appVersion) {
        this.appName = appName;
        this.appVersion = appVersion;
    }

    public QualityResult checkS2R(String S2RDescription) throws Exception {
        List<NLAction> nlActions = NLParser.parseText(appName, S2RDescription);
        if (nlActions.isEmpty()) return new QualityResult(NO_MATCH);
        return matchActions(nlActions);
    }

    private QualityResult matchActions(List<NLAction> nlActions) throws Exception {
        AppGraphInfo graph = GraphReader.getGraph(appName, appVersion);
        return new QualityResult(MULTIPLE_MATCH);
    }
}

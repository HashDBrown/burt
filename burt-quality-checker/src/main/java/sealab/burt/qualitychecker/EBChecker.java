package sealab.burt.qualitychecker;

import sealab.burt.nlparser.euler.actions.nl.NLAction;
import sealab.burt.qualitychecker.graph.AppGraphInfo;

import java.util.List;

import static sealab.burt.qualitychecker.QualityResult.Result.NO_MATCH;

public class EBChecker {

    private final String app;
    private final String appVersion;

    public EBChecker(String app, String appVersion) {
        this.app = app;
        this.appVersion = appVersion;
    }

    public QualityResult checkEb(String ebDescription) throws Exception {
        List<NLAction> nlActions = NLParser.parseText(app, ebDescription);
        if (nlActions.isEmpty()) return new QualityResult(NO_MATCH);
        return matchActions(nlActions);
    }

    private QualityResult matchActions(List<NLAction> nlActions) throws Exception {
        AppGraphInfo graph = GraphReader.getGraph(app, appVersion);
        //TODO: continue here
        return new QualityResult(QualityResult.Result.MATCH);
    }
}

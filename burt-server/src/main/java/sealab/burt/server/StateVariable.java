package sealab.burt.server;

public enum StateVariable {
    //general ones
    NEXT_INTENT, CURRENT_MESSAGE,

    //app variables
    APP, APP_VERSION, APP_ASKED,

    //ob variables
    COLLECTING_OB, OB_SCREEN_SELECTED,

    //eb variables
    COLLECTING_EB,

    //s2r variables
    COLLECTING_S2R, CONFIRM_LAST_STEP, DISAMBIGUATE_S2R,

    //quality checkers/results
    EB_CHECKER, S2R_CHECKER, OB_CHECKER, EB_QUALITY_RESULT, OB_QUALITY_RESULT,
}

package sealab.burt.nlparser.euler.actions.utils;

import org.apache.commons.lang3.StringUtils;
import sealab.burt.nlparser.euler.actions.DeviceActions;
import sealab.burt.nlparser.euler.actions.nl.NLAction;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;


public class GeneralUtils {

    private static final DecimalFormat DEC_FORMATTER = new DecimalFormat("#,###,###,##0.00");

    public static boolean isAppSubject(NLAction nlAction, String appName) {
        String subject = nlAction.getSubject();
        return isAppWord(appName, subject, null);
    }

    public static boolean isAppWord(String term, String appName, String packageName) {
        return "app".equalsIgnoreCase(term) || "application".equalsIgnoreCase(term) || "build".equalsIgnoreCase(term)
                || isAppName(term, appName, packageName);
    }


    public static boolean isAppName(String text, String appName, String packageName) {

        if (appName == null || text == null) {
            return false;
        }

        String toCompare = appName;
        if (StringUtils.isEmpty(toCompare)) {
            if (packageName != null) {
                if (packageName.contains("."))
                    toCompare = packageName.substring(packageName.lastIndexOf(".") + 1, packageName.length());
                else
                    toCompare = packageName;
            }
        }

        if (StringUtils.isEmpty(toCompare))
            return false;

        List<String> subTokens = Arrays.asList(StringUtils.splitByCharacterTypeCamelCase(text));
        List<String> toCompareTokens = Arrays.asList(StringUtils.splitByCharacterTypeCamelCase(toCompare));

        long numMatches = toCompareTokens.stream().filter(toCompareTok -> subTokens.stream()
                .anyMatch(subjTok -> toCompareTok.toLowerCase().contains(subjTok.toLowerCase()))).count();

        // return subTokens.stream()
        // .anyMatch(subjTok -> appNameTokens.stream()
        // .anyMatch(appNameTok ->
        // subjTok.toLowerCase().contains(appNameTok.toLowerCase())
        // || appNameTok.toLowerCase().contains(subjTok.toLowerCase())));
        return ((double) numMatches) / toCompareTokens.size() >= 0.5;
    }

    public static String format(double value, DecimalFormat formatter) {
        return formatter.format(value);
    }

    public static String format(double value) {
        return format(value, DEC_FORMATTER);
    }

    public static String replaceHTML(String htmlTemplate, List<String> parameters) {
        String bugHtml = htmlTemplate;
        for (int i = 0; i < parameters.size(); i++) {
            CharSequence parameter = parameters.get(i);
            bugHtml = bugHtml.replace("[ " + (i + 1) + " ]", parameter == null ? "" : parameter);
        }
        return bugHtml;
    }

    public static List<File> filterScenariosBySystems(Collection<File> scenarioFiles, Set<String> allowedSystems) {
        return scenarioFiles.stream().filter(f -> {

            if (allowedSystems == null || allowedSystems.isEmpty()) {
                return true;
            }

            String fileName = f.getName().toLowerCase();
            Optional<String> findFirst = allowedSystems.stream()
                    .filter(s -> fileName.startsWith(s.toLowerCase()))
                    .findFirst();
            return findFirst.isPresent();

        }).collect(Collectors.toList());
    }


    public static List<String> getAllLines(Path path, String fileName) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(path)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }


    public static List<String> getAllLines(ClassLoader classLoader, String fileName) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(classLoader.getResourceAsStream
                (fileName)))) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    //---------------------------------



    //--------------------------------


    public static String getEventName(int event) {
        String eventName = "unknown";
        switch (event) {
            case DeviceActions.CLICK:
                eventName = "click";
                break;
            case DeviceActions.TYPE:
                eventName = "type";
                break;
            case DeviceActions.CLICK_TYPE:
                eventName = "click + type";
                break;
            case DeviceActions.OPEN_APP:
                eventName = "open app";
                break;
            case DeviceActions.CLOSE_APP:
                eventName = "close app";
                break;
            case DeviceActions.LONG_CLICK:
                eventName = "long click";
                break;
            case DeviceActions.BACK:
                eventName = "back";
                break;
            case DeviceActions.ROTATION:
                eventName = "rotate";
                break;
            case DeviceActions.KEYEVENT:
                eventName = "key event";
                break;
            case DeviceActions.MENU_BTN:
                eventName = "click menu";
                break;
            case DeviceActions.DELETE_TEXT:
                eventName = "delete text";
                break;
            case DeviceActions.SWIPE:
                eventName = "swipe";
                break;
            case DeviceActions.SWIPE_UP:
                eventName = "swipe up";
                break;
            case DeviceActions.SWIPE_RIGHT:
                eventName = "swipe right";
                break;
            case DeviceActions.SWIPE_DOWN:
                eventName = "swipe down";
                break;
            case DeviceActions.SWIPE_LEFT:
                eventName = "swipe left";
                break;
        }
        return eventName;
    }

}

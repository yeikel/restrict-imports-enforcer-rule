package de.skuzzle.enforcer.restrictimports.formatting;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import de.skuzzle.enforcer.restrictimports.analyze.AnalyzeResult;
import de.skuzzle.enforcer.restrictimports.analyze.BannedImportGroup;
import de.skuzzle.enforcer.restrictimports.analyze.MatchedFile;
import de.skuzzle.enforcer.restrictimports.analyze.MatchedImport;
import de.skuzzle.enforcer.restrictimports.util.Preconditions;

class MatchFormatterImpl implements MatchFormatter {

    static final MatchFormatter INSTANCE = new MatchFormatterImpl();

    @Override
    public String formatMatches(Collection<Path> roots, AnalyzeResult analyzeResult) {
        final StringBuilder b = new StringBuilder();

        if (analyzeResult.bannedImportsInCompileCode()) {
            b.append("\nBanned imports detected:\n\n");

            final Map<BannedImportGroup, List<MatchedFile>> srcMatchesByGroup = analyzeResult.srcMatchesByGroup();
            formatGroupedMatches(roots, b, srcMatchesByGroup);
        }

        if (analyzeResult.bannedImportsInTestCode()) {
            b.append("\nBanned imports detected in TEST code:\n\n");
            final Map<BannedImportGroup, List<MatchedFile>> testMatchesByGroup = analyzeResult.testMatchesByGroup();
            formatGroupedMatches(roots, b, testMatchesByGroup);
        }

        final Duration duration = analyzeResult.duration();
        b.append("\nAnalysis of ")
                .append(pluralize(analyzeResult.analysedFiles(), " file"))
                .append(" took ")
                .append(DurationFormat.formatDuration(duration))
                .append("\n");

        return b.toString();
    }

    private static String pluralize(long value, String singular) {
        return value == 1
                ? value + singular
                : value + singular + "s";
    }

    private void formatGroupedMatches(Collection<Path> roots, StringBuilder b,
            Map<BannedImportGroup, List<MatchedFile>> matchesByGroup) {

        final int longestMatchedString = longestMatch(matchesByGroup.values());

        matchesByGroup.forEach((group, matches) -> {
            group.getReason().ifPresent(reason -> b.append("Reason: ").append(reason).append("\n"));
            matches.forEach(fileMatch -> {
                b.append("\tin file").append(": ")
                        .append(relativize(roots, fileMatch.getSourceFile()))
                        .append("\n");
                fileMatch.getMatchedImports().forEach(match -> appendMatch(match, longestMatchedString, b));
            });
        });
    }

    private int longestMatch(Collection<? extends Collection<MatchedFile>> files) {
        return files.stream()
                .flatMap(Collection::stream)
                .map(MatchedFile::getMatchedImports)
                .flatMap(Collection::stream)
                .map(MatchedImport::getMatchedString)
                .mapToInt(String::length)
                .max()
                .orElse(1);
    }

    private static Path relativize(Collection<Path> roots, Path path) {
        return roots.stream()
                .filter(path::startsWith)
                .map(root -> root.relativize(path))
                .findFirst()
                .orElse(path);
    }

    private void appendMatch(MatchedImport match, int longestMatchedString, StringBuilder b) {
        b.append("\t\t")
                .append(padRight(match.getMatchedString(), longestMatchedString))
                .append(" \t(Line: ")
                .append(match.getImportLine())
                .append(", Matched by: ")
                .append(match.getMatchedBy())
                .append(")\n");
    }

    private String padRight(String original, int intendedLength) {
        Preconditions.checkArgument(original.length() <= intendedLength);
        final int diff = intendedLength - original.length();
        final StringBuilder builder = new StringBuilder(intendedLength)
                .append(original);

        for (int i = 0; i < diff; ++i) {
            builder.append(" ");
        }
        return builder.toString();
    }

}

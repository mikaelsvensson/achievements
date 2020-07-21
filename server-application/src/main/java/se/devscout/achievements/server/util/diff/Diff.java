package se.devscout.achievements.server.util.diff;

import se.devscout.achievements.server.data.model.SegmentType;
import se.devscout.achievements.server.data.model.StringSegment;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Wrapper class for DiffMatchPatch
 */
public class Diff {

    private static final DiffMatchPatch differ = new DiffMatchPatch();

    public static List<StringSegment> diff(String text1, String text2) {
        final var diffs = differ.diff_main(text1, text2, true);
        differ.diff_cleanupSemantic(diffs);
        return diffs.stream()
                .map(diff -> new StringSegment(
                        diff.text,
                        switch (diff.operation) {
                            case DELETE -> SegmentType.REMOVED;
                            case INSERT -> SegmentType.ADDED;
                            default -> null;
                        }))
                .collect(Collectors.toList());
    }
}

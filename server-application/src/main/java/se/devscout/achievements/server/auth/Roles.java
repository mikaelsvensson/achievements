package se.devscout.achievements.server.auth;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import java.util.Set;

public interface Roles {
    String EDITOR = "editor";
    String READER = "reader";
    //TODO: When more than two roles exist: Implement authorization feature ensuring that users cannot elevate their own, or someone else's, privileges when they edit or create people in the system.

    ImmutableMap<String, Set<String>> IMPLICIT_ROLES = ImmutableMap.of(
            EDITOR, Sets.newHashSet(READER)
    );
}

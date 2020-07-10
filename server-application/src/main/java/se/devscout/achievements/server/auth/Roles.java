package se.devscout.achievements.server.auth;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import java.util.Set;

public interface Roles {
    String EDITOR = "editor";
    String READER = "reader";
    String ADMIN = "admin";

    ImmutableMap<String, Set<String>> IMPLICIT_ROLES = ImmutableMap.of(
            EDITOR, Sets.newHashSet(READER),
            ADMIN, Sets.newHashSet(EDITOR, READER)
    );
}

package se.devscout.achievements.server.api;

import java.net.URI;
import java.util.List;

public class AchievementBaseDTO {
    public String id;
    public String name;
    public String slug;
    public List<String> tags;
    public URI image;
}

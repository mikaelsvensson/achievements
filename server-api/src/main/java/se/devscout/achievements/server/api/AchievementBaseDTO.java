package se.devscout.achievements.server.api;

import java.net.URI;
import java.util.List;

public class AchievementBaseDTO {
    public String id;
    public String name;
    public List<String> tags;
    public URI image;
}

package se.devscout.achievements.server.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class AchievementBaseDTO {
    public String id;
    public String name;
    public String slug;
    public List<String> tags = new ArrayList<>();
    public URI image;
}

package se.devscout.achievements.server.api;

public class GroupBaseDTO {
    public Integer id;
    public String name;

    public GroupBaseDTO() {
    }

    public GroupBaseDTO(Integer id, String name) {
        this.id = id;
        this.name = name;
    }
}

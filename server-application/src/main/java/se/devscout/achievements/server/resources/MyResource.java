package se.devscout.achievements.server.resources;

import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import se.devscout.achievements.server.api.OrganizationAchievementSummaryDTO;
import se.devscout.achievements.server.api.OrganizationDTO;
import se.devscout.achievements.server.api.PersonDTO;
import se.devscout.achievements.server.api.PersonProfileDTO;
import se.devscout.achievements.server.auth.User;
import se.devscout.achievements.server.data.dao.AchievementsDao;
import se.devscout.achievements.server.data.dao.OrganizationsDao;
import se.devscout.achievements.server.data.dao.PeopleDao;
import se.devscout.achievements.server.data.model.Achievement;
import se.devscout.achievements.server.data.model.Person;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

@Path("my")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MyResource extends AbstractResource {
    private PeopleDao peopleDao;
    private OrganizationsDao organizationsDao;
    private AchievementsDao achievementsDao;

    public MyResource(PeopleDao peopleDao, OrganizationsDao organizationsDao, AchievementsDao achievementsDao) {
        this.peopleDao = peopleDao;
        this.organizationsDao = organizationsDao;
        this.achievementsDao = achievementsDao;
    }

    @GET
    @Path("profile")
    @UnitOfWork
    public PersonProfileDTO getMyProfile(@Auth User user) {
        return new PersonProfileDTO(
                map(user.getCredentials().getPerson().getOrganization(), OrganizationDTO.class),
                map(user.getCredentials().getPerson(), PersonDTO.class));
    }

    @GET
    @Path("people")
    @UnitOfWork
    public List<PersonDTO> getMyPeople(@Auth User user) {
        return peopleDao.getByParent(user.getCredentials().getPerson().getOrganization()).stream()
                .map(p -> map(p, PersonDTO.class))
                .collect(Collectors.toList());
    }

    @GET
    @Path("achievement-summary")
    @UnitOfWork
    public OrganizationAchievementSummaryDTO getMyAchievementsSummary(@Auth User user) {
        final Person person = user.getCredentials().getPerson();

        final List<Achievement> achievements = achievementsDao.findWithProgressForPerson(person);

        final OrganizationAchievementSummaryDTO summary = createAchievementSummaryDTO(achievements, person.getId());

        return summary;
    }

}

package se.devscout.achievements.server.resources;

import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import se.devscout.achievements.server.api.OrganizationAchievementSummaryDTO;
import se.devscout.achievements.server.api.OrganizationDTO;
import se.devscout.achievements.server.api.PersonDTO;
import se.devscout.achievements.server.api.PersonProfileDTO;
import se.devscout.achievements.server.data.dao.AchievementsDao;
import se.devscout.achievements.server.data.dao.ObjectNotFoundException;
import se.devscout.achievements.server.data.dao.PeopleDao;
import se.devscout.achievements.server.data.model.Achievement;
import se.devscout.achievements.server.data.model.Person;
import se.devscout.achievements.server.resources.auth.User;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

@Path("my")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MyResource extends AbstractResource {
    private PeopleDao peopleDao;
    private AchievementsDao achievementsDao;

    public MyResource(PeopleDao peopleDao, AchievementsDao achievementsDao) {
        this.peopleDao = peopleDao;
        this.achievementsDao = achievementsDao;
    }

    @GET
    @Path("profile")
    @UnitOfWork
    public PersonProfileDTO getMyProfile(@Auth User user) {
        final Person person = getPerson(user);
        return new PersonProfileDTO(
                map(person.getOrganization(), OrganizationDTO.class),
                map(person, PersonDTO.class));
    }

    @GET
    @Path("people")
    @UnitOfWork
    public List<PersonDTO> getMyPeople(@Auth User user) {
        final Person person = getPerson(user);
        return peopleDao.getByParent(person.getOrganization()).stream()
                .map(p -> map(p, PersonDTO.class))
                .collect(Collectors.toList());
    }

    private Person getPerson(@Auth User user) {
        try {
            return peopleDao.read(user.getPersonId());
        } catch (ObjectNotFoundException e) {
            // If this happens it basically means that the user was deleted between when the user was authenticated and now.
            throw new WebApplicationException("Could not find user mentioned in User object.");
        }
    }

    @GET
    @Path("achievement-summary")
    @UnitOfWork
    public OrganizationAchievementSummaryDTO getMyAchievementsSummary(@Auth User user) {
        final Person person = getPerson(user);

        final List<Achievement> achievements = achievementsDao.findWithProgressForPerson(person);

        final OrganizationAchievementSummaryDTO summary = createAchievementSummaryDTO(achievements, person.getId());

        return summary;
    }

}

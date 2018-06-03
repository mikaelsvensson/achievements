package se.devscout.achievements.server.resources;

import com.google.common.base.Strings;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import se.devscout.achievements.server.api.*;
import se.devscout.achievements.server.auth.ValidationResult;
import se.devscout.achievements.server.auth.password.PasswordValidator;
import se.devscout.achievements.server.auth.password.SecretGenerator;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.Achievement;
import se.devscout.achievements.server.data.model.Credentials;
import se.devscout.achievements.server.data.model.CredentialsType;
import se.devscout.achievements.server.data.model.Person;
import se.devscout.achievements.server.resources.auth.User;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Path("my")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MyResource extends AbstractResource {
    private PeopleDao peopleDao;
    private GroupsDao groupsDao;
    private AchievementsDao achievementsDao;
    private CredentialsDao credentialsDao;

    public MyResource(PeopleDao peopleDao, GroupsDao groupsDao, AchievementsDao achievementsDao, CredentialsDao credentialsDao) {
        this.peopleDao = peopleDao;
        this.groupsDao = groupsDao;
        this.achievementsDao = achievementsDao;
        this.credentialsDao = credentialsDao;
    }

    @GET
    @Path("profile")
    @UnitOfWork
    public PersonProfileDTO getMyProfile(@Auth User user) {
        final Person person = getPerson(user);
        final PersonProfileDTO dto = new PersonProfileDTO(
                map(person.getOrganization(), OrganizationDTO.class),
                map(person, PersonDTO.class));
        final Optional<Credentials> passwordCredential = person.getCredentials().stream().filter(c -> c.getType() == CredentialsType.PASSWORD).findFirst();
        dto.person.is_password_credential_created = passwordCredential.isPresent();
        if (dto.person.is_password_credential_created) {
            dto.person.is_password_set = passwordCredential.get().getData() != null && passwordCredential.get().getData().length > 0;
        }
        return dto;
    }

    @POST
    @Path("password")
    @UnitOfWork
    public void setPassword(@Auth User user, SetPasswordDTO payload) {
        final Person person = getPerson(user);
        final Optional<Credentials> passwordOpt = person.getCredentials().stream()
                .filter(c -> c.getType() == CredentialsType.PASSWORD)
                .findFirst();
        if (passwordOpt.isPresent()) {
            final Credentials credentials = passwordOpt.get();
            final byte[] currentPwData = credentials.getData();
            if (currentPwData != null && currentPwData.length > 0) {
                if (!Strings.isNullOrEmpty(payload.current_password)) {
                    final PasswordValidator currentPwValidator = new PasswordValidator(currentPwData);
                    final ValidationResult currentPwValidationResult = currentPwValidator.validate(payload.current_password.toCharArray());
                    if (!currentPwValidationResult.isValid()) {
                        throw new BadRequestException();
                    }
                } else {
                    throw new BadRequestException();
                }
            }
            if (!Strings.isNullOrEmpty(payload.new_password) && !Strings.isNullOrEmpty(payload.new_password_confirm)) {
                if (payload.new_password.equals(payload.new_password_confirm)) {
                    try {
                        final PasswordValidator passwordValidator = new PasswordValidator(
                                SecretGenerator.PDKDF2,
                                payload.new_password.toCharArray());
                        credentials.setData(passwordValidator.getCredentialsData());
                        credentials.setType(passwordValidator.getCredentialsType());
                        credentialsDao.update(credentials.getId(), credentials);
                    } catch (ObjectNotFoundException e) {
                        throw new NotFoundException();
                    } catch (DaoException e) {
                        throw new InternalServerErrorException(e);
                    }
                } else {
                    throw new BadRequestException();
                }
            } else {
                throw new BadRequestException();
            }
        } else {
            throw new NotFoundException();
        }
    }

    @GET
    @Path("people")
    @UnitOfWork
    public List<PersonBaseDTO> getMyPeople(@Auth User user) {
        final Person person = getPerson(user);
        return peopleDao.getByParent(person.getOrganization()).stream()
                .map(p -> map(p, PersonBaseDTO.class))
                .collect(Collectors.toList());
    }

    @GET
    @Path("groups")
    @UnitOfWork
    public List<GroupBaseDTO> getMyGroups(@Auth User user) {
        final Person person = getPerson(user);
        return groupsDao.getByParent(person.getOrganization()).stream()
                .map(p -> map(p, GroupBaseDTO.class))
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

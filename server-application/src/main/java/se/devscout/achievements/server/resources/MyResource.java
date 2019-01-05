package se.devscout.achievements.server.resources;

import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import org.apache.commons.lang3.StringUtils;
import se.devscout.achievements.server.I18n;
import se.devscout.achievements.server.RateLimited;
import se.devscout.achievements.server.api.*;
import se.devscout.achievements.server.auth.ValidationResult;
import se.devscout.achievements.server.auth.jwt.JwtSignInTokenService;
import se.devscout.achievements.server.auth.password.PasswordValidator;
import se.devscout.achievements.server.auth.password.SecretGenerator;
import se.devscout.achievements.server.data.dao.*;
import se.devscout.achievements.server.data.model.*;
import se.devscout.achievements.server.filter.audit.Audited;
import se.devscout.achievements.server.mail.EmailSender;
import se.devscout.achievements.server.mail.EmailSenderException;
import se.devscout.achievements.server.mail.template.SetPasswordTemplate;
import se.devscout.achievements.server.resources.auth.AbstractAuthResource;
import se.devscout.achievements.server.resources.auth.ExternalIdpCallbackException;
import se.devscout.achievements.server.resources.auth.User;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Path("my")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MyResource extends AbstractAuthResource {
    private final I18n i18n;
    private final PeopleDao peopleDao;
    private final GroupsDao groupsDao;
    private final AchievementsDao achievementsDao;
    private final CredentialsDao credentialsDao;
    private final EmailSender emailSender;
    private final URI guiApplicationHost;
    private final SetPasswordTemplate template = new SetPasswordTemplate();

    public MyResource(PeopleDao peopleDao, GroupsDao groupsDao, AchievementsDao achievementsDao, CredentialsDao credentialsDao, EmailSender emailSender, URI guiApplicationHost, JwtSignInTokenService signInTokenService, I18n i18n) {
        super(signInTokenService, credentialsDao);
        this.peopleDao = peopleDao;
        this.groupsDao = groupsDao;
        this.achievementsDao = achievementsDao;
        this.credentialsDao = credentialsDao;
        this.emailSender = emailSender;
        this.guiApplicationHost = guiApplicationHost;

        this.i18n = i18n;
    }

    @GET
    @Path("profile")
    @UnitOfWork
    @Audited
    public PersonProfileDTO getMyProfile(@Auth User user) {
        final Person person = getPerson(user);

        final Optional<Credentials> passwordCredential = person
                .getCredentials()
                .stream()
                .filter(c -> c.getType() == CredentialsType.PASSWORD)
                .findFirst();

        final boolean isPasswordCredentialCreated = passwordCredential.isPresent();
        final boolean isPasswordSet = isPasswordCredentialCreated &&
                passwordCredential.get().getData() != null &&
                passwordCredential.get().getData().length > 0;

        final Organization organization = person.getOrganization();

        // TODO: Implement COUNT(*) query instead of counting the number of result from SELECT
        final int peopleCount = peopleDao.getByParent(organization).size();
        final boolean isOnlyPersonInOrganization = 1 == peopleCount;

        // TODO: Implement COUNT(*) query instead of counting the number of result from SELECT
        final int groupsCount = groupsDao.getByParent(organization).size();
        final boolean isGroupCreated = groupsCount > 0;

        return new PersonProfileDTO(
                map(organization, OrganizationDTO.class),
                map(person, PersonDTO.class),
                new GettingStartedDTO(
                        isOnlyPersonInOrganization,
                        // TODO: Implement support for sending, and counting, welcome letters
                        true,
                        // TODO: Implement support for counting "number of progress records"
                        true,
                        isPasswordCredentialCreated,
                        isPasswordSet,
                        isGroupCreated
                ));
    }

    @POST
    @Path("password")
    @UnitOfWork
    @Audited
    // TODO: Split into smaller methods
    public Response setPassword(@Auth User user, SetPasswordDTO payload) {
        final Person person = getPerson(user);
        final Optional<Credentials> passwordOpt = person.getCredentials().stream()
                .filter(c -> c.getType() == CredentialsType.PASSWORD)
                .findFirst();
        if (passwordOpt.isPresent()) {
            final Credentials credentials = passwordOpt.get();
            final byte[] currentPwData = credentials.getData();
            final boolean validationOfCurrentPasswordRequired = currentPwData != null
                    && currentPwData.length > 0
                    && user.getCredentialsTypeUsed() != CredentialsType.ONETIME_PASSWORD;
            if (validationOfCurrentPasswordRequired) {
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

                        if (user.getCredentialsTypeUsed() == CredentialsType.ONETIME_PASSWORD) {
                            try {
                                return Response
                                        .ok()
                                        .entity(createTokenDTO(CredentialsType.PASSWORD, credentials.getUserId()))
                                        .build();
                            } catch (ExternalIdpCallbackException e) {
                                throw new InternalServerErrorException(e);
                            }
                        } else {
                            return Response.noContent().build();
                        }
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

    @POST
    @Path("send-set-password-link")
    @Audited
    @UnitOfWork
    @RateLimited(requestsPerMinute = 10, burstLimit = 0)
    public void sendResetPasswordLink(@Auth Optional<User> user,
                                      ForgotPasswordDTO payload,
                                      @Context HttpServletRequest req) {

        Person person = null;

        if (user.isPresent()) {
            person = getPerson(user.get());
        } else {
            final List<Person> people = peopleDao.getByEmail(payload.email);
            if (people != null && people.size() == 1) {
                person = people.get(0);
            }
        }

        if (person != null) {
            try {
                final byte[] bytes = new byte[20];
                new SecureRandom().nextBytes(bytes);
                final String onetimePassword = BaseEncoding.base32().encode(bytes).toLowerCase();
                credentialsDao.create(person, new CredentialsProperties(onetimePassword, CredentialsType.ONETIME_PASSWORD, null));

                final URI link = URI.create(StringUtils.appendIfMissing(guiApplicationHost.toString(), "/") + "#set-password/" + onetimePassword);

                emailSender.send(
                        req != null ? req.getRemoteAddr() : "ANONYMOUS",
                        person.getEmail(),
                        i18n.get("sendResetPasswordLink.subject"),
                        template.render(link));
            } catch (DaoException e) {
                // TODO: Fix default catch clause
                e.printStackTrace();
            } catch (EmailSenderException e) {
                // TODO: Fix default catch clause
                e.printStackTrace();
            }
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

        final OrganizationAchievementSummaryDTO summary = createAchievementSummaryDTO(achievements, person.getId(), person.getOrganization().getId());

        return summary;
    }

}

package se.devscout.achievements.server.data.dao;

import com.google.common.base.Strings;
import org.hibernate.SessionFactory;
import se.devscout.achievements.server.data.model.Organization;
import se.devscout.achievements.server.data.model.OrganizationProperties;

import javax.persistence.EntityExistsException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import java.util.List;
import java.util.UUID;

public class OrganizationsDaoImpl extends DaoImpl<Organization, UUID> implements OrganizationsDao {

    private Long maxOrganizationCount;

    public OrganizationsDaoImpl(SessionFactory sessionFactory, Long maxOrganizationCount) {
        super(sessionFactory);
        this.maxOrganizationCount = maxOrganizationCount;
    }

    @Override
    public Organization read(UUID id) throws ObjectNotFoundException {
        return getEntity(id);
    }

    @Override
    public Organization create(OrganizationProperties properties) throws TooManyOrganizationsException {
        if (maxOrganizationCount != null) {
            final Long count = getEntityCount();
            if (count >= maxOrganizationCount) {
                throw new TooManyOrganizationsException();
            }
        }
        final Organization existingOrg = super.currentSession()
                .byNaturalId(Organization.class)
                .using("name", properties.getName())
                .load();
        if (existingOrg == null) {
            final Organization entity = new Organization(properties.getName());
            return super.persist(entity);
        } else {
            throw new EntityExistsException("Organization " + properties.getName() + " already exists.");
        }
    }

    @Override
    public Organization update(UUID id, OrganizationProperties properties) throws ObjectNotFoundException {
        final Organization organization = read(id);
        organization.apply(properties);
        return super.persist(organization);
    }

    @Override
    public void delete(UUID id) throws ObjectNotFoundException {
        final Organization organization = read(id);
        super.currentSession().delete(organization);
    }

    private Long getEntityCount() {
        final CriteriaBuilder cb = super.currentSession().getCriteriaBuilder();
        final CriteriaQuery<Long> query = cb.createQuery(Long.class);
        query.select(cb.count(query.from(Organization.class)));
        return super.currentSession().createQuery(query).getSingleResult();
    }

    @Override
    public List<Organization> find(String name) {
        name = Strings.nullToEmpty(name).trim();
        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("Search condition was not specified.");
        }
        return namedQuery("Organization.find")
                .setParameter("name", "%" + name + "%")
                .getResultList();
    }
}

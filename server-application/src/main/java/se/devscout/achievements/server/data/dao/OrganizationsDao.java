package se.devscout.achievements.server.data.dao;

import se.devscout.achievements.server.data.model.Organization;
import se.devscout.achievements.server.data.model.OrganizationProperties;

import java.util.List;
import java.util.UUID;

public interface OrganizationsDao extends CrudRootDao<Organization, OrganizationProperties, UUID> {
    List<Organization> find(String name);
    List<Organization> all();
}

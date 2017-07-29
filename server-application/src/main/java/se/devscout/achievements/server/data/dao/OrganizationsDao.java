package se.devscout.achievements.server.data.dao;

import se.devscout.achievements.server.data.model.Organization;
import se.devscout.achievements.server.data.model.OrganizationProperties;

import java.util.List;

public interface OrganizationsDao extends CrudRootDao<Organization, OrganizationProperties> {
    List<Organization> find(String name);
}
